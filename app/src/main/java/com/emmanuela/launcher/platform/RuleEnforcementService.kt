package com.emmanuela.launcher.platform

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import android.graphics.Color
import android.os.SystemClock
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.emmanuela.launcher.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.time.ZonedDateTime

/** Short, package-bound handoff prevents counting launcher admissions twice. */
object EnforcementBridge {
    private val mutableStatus=kotlinx.coroutines.flow.MutableStateFlow(EnforcementStatus())
    val status:kotlinx.coroutines.flow.StateFlow<EnforcementStatus> = mutableStatus
    var connected:Boolean
        get()=mutableStatus.value.connected
        set(value){mutableStatus.value=mutableStatus.value.copy(connected=value)}
    fun browser(pkg:String,readable:Boolean,issue:String?){mutableStatus.value=EnforcementStatus(connected,pkg,readable,issue)}
    private val grants=mutableMapOf<String,Long>()
    @Synchronized fun grant(pkg:String){grants[pkg]=SystemClock.elapsedRealtime()+5000}
    @Synchronized fun consume(pkg:String):Boolean{val at=grants.remove(pkg)?:return false;return SystemClock.elapsedRealtime()<at}
}
class RuleEnforcementService:AccessibilityService(){
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private var state=LauncherData()
    private var foreground=""
    private var handoffPending=false
    private var admitted=false
    private var generation=0L
    private var admission:Job?=null
    private var configurationJob:Job?=null
    private var receiverRegistered=false
    private val screenReceiver=object:BroadcastReceiver(){override fun onReceive(context:Context?,intent:Intent?){if(intent?.action==Intent.ACTION_SCREEN_OFF){generation++;check?.cancel();admission?.cancel();timer?.cancel();foreground="";currentUrl="";browserRead?.cancel();browserRead=null;admitted=false;removeOverlay()}else if(intent?.action in setOf(Intent.ACTION_TIME_CHANGED,Intent.ACTION_TIMEZONE_CHANGED)){timer?.cancel();evaluate(false)}}}
    private var sessionStarted=0L
    private val sessionBase=mutableMapOf<String,Long>()
    private var check:Job?=null
    private var timer:Job?=null
    private var pauseJob:Job?=null
    private var overlay:LinearLayout?=null
    private var shownReason=""
    private var currentUrl=""
    private var browserRead:Job?=null
    private var browserReadPending=false
    private var warnedAddress=false
    private var activeRules=emptySet<String>()
    private val countedRules=mutableSetOf<String>()
    private val warnedRules=mutableSetOf<String>()
    private var warningDay=""
    override fun onServiceConnected(){
        EnforcementBridge.connected=true
        if(!receiverRegistered){ContextCompat.registerReceiver(this,screenReceiver,IntentFilter(Intent.ACTION_SCREEN_OFF).apply{addAction(Intent.ACTION_TIME_CHANGED);addAction(Intent.ACTION_TIMEZONE_CHANGED)},ContextCompat.RECEIVER_NOT_EXPORTED);receiverRegistered=true}
        configurationJob?.cancel()
        configurationJob=scope.launch{ConfigurationRepository(applicationContext).data.collect{next->if(next.focusGroups!=state.focusGroups||next.policies!=state.policies){generation++;check?.cancel();admission?.cancel();timer?.cancel();removeOverlay();admitted=false;handoffPending=false;warnedRules.clear()};state=next;if(!state.settings.ui.v2.backgroundRules){generation++;check?.cancel();admission?.cancel();removeOverlay();timer?.cancel();admitted=false}else evaluate(false)}}
    }
    override fun onAccessibilityEvent(event:AccessibilityEvent?){
        if(event==null||!state.settings.ui.v2.backgroundRules)return
        val pkg=event.packageName?.toString()?:return
        if(event.eventType==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED){
            // System permission/keyguard windows must remain reachable.
            if(pkg==packageName&&overlay!=null)return
            if(pkg=="com.android.systemui"||pkg=="com.android.settings"||pkg=="com.google.android.permissioncontroller"||pkg=="com.android.permissioncontroller"){generation++;check?.cancel();admission?.cancel();timer?.cancel();pauseJob?.cancel();removeOverlay();foreground="";admitted=false;return}
            if(pkg!=foreground){generation++;browserRead?.cancel();browserRead=null;warnedAddress=false;EnforcementBridge.connected=true;admission?.cancel();check?.cancel();timer?.cancel();pauseJob?.cancel();removeOverlay();sessionBase.clear();countedRules.clear();warnedRules.clear();activeRules=emptySet();foreground=pkg;handoffPending=EnforcementBridge.consume(pkg);admitted=false;currentUrl="";sessionStarted=SystemClock.elapsedRealtime();evaluate(true)}
        }
        if(pkg==foreground&&state.focusGroups.any{pkg in it.browsers}&&(event.eventType==AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED||event.eventType==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)){
            readBrowserAddress(pkg)
        }
    }
    private fun readBrowserAddress(pkg:String){
        browserReadPending=true
        if(browserRead?.isActive==true)return
        browserRead=scope.launch{
            do {
                browserReadPending=false
                val expected=generation
                // Accessibility tree queries can block on the browser; keep them off the UI thread.
                val snapshot=withContext(Dispatchers.IO){
                    val root=rootInActiveWindow
                    val owner=root?.packageName?.toString()
                    val values=if(owner==pkg)BrowserAdapters.ids[pkg].orEmpty().flatMap{id->root?.findAccessibilityNodeInfosByViewId("$pkg:id/$id").orEmpty().mapNotNull{it.text?.toString()}}else emptyList()
                    owner to values
                }
                if(expected!=generation||foreground!=pkg||!state.settings.ui.v2.backgroundRules)return@launch
                // Our own overlay hides the browser tree; keep the decision that created it.
                if(snapshot.first==packageName&&overlay!=null)return@launch
                val address=BrowserAdapters.address(pkg,snapshot.first,snapshot.second)
                val issue=if(address!=null)null else if(pkg !in BrowserAdapters.ids)"This browser has no address adapter. Website rules are unavailable." else "Browser address unavailable. Website rules cannot be verified on this screen."
                EnforcementBridge.browser(pkg,address!=null,issue)
                if(issue!=null&&!warnedAddress){warnedAddress=true;android.widget.Toast.makeText(this@RuleEnforcementService,issue,android.widget.Toast.LENGTH_LONG).show()}
                if(address.orEmpty()!=currentUrl){currentUrl=address.orEmpty();evaluate(false)}
            }while(browserReadPending)
        }
    }
    private fun groups(pkg:String)=state.focusGroups.filter{pkg in it.packages||(pkg in it.browsers&&(it.websites.isNotEmpty()||it.keywords.isNotEmpty())&&FocusWindows.matches(it,currentUrl))}
    private fun evaluate(opening:Boolean){
        val pkg=foreground
        if(pkg.isEmpty()||pkg==packageName){removeOverlay();return}
        check?.cancel()
        check=scope.launch{
            try{
                val policy=state.policies[pkg]?:AppPolicy()
                val groups=groups(pkg)
                val protectedFolder=state.folders.any{it.isProtected&&it.apps.any{id->id.substringBefore('/')==pkg}}
                if(groups.isEmpty()&&policy==AppPolicy()&&!protectedFolder){removeOverlay();timer?.cancel();admitted=false;sessionBase.clear();activeRules=emptySet();return@launch}
                val ids=groups.map{it.id}.toSet()
                if(ids!=activeRules){admitted=false;activeRules=ids;sessionBase.keys.retainAll(ids);sessionStarted=SystemClock.elapsedRealtime()}
                val handoff=handoffPending
                if(!handoff&&!admitted&&(policy.privateApp||protectedFolder)){
                    showBlock("Confirm your identity in EmmanueLA",0,onOpen={removeOverlay();startActivity(Intent(this@RuleEnforcementService,com.emmanuela.launcher.MainActivity::class.java).putExtra("authenticate_package",pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))});return@launch
                }
                val now=ZonedDateTime.now()
                val usage=withContext(Dispatchers.IO){applicationContext.appUsageToday()}
                val counts=withContext(Dispatchers.IO){getSharedPreferences("focus_opens",MODE_PRIVATE).all}
                var reason=PolicyRules.reason(policy,now,usage,pkg)
                val session=(SystemClock.elapsedRealtime()-sessionStarted).coerceAtLeast(0)
                groups.forEach{g->
                    val used=usage?.let{if(g.perApp)it[pkg]?:0L else g.packages.sumOf{p->it[p]?:0L}+if(g.packages.isEmpty())it[pkg]?:0L else 0L}
                    val opens=(counts[FocusWindows.countKey(g,pkg,now.toLocalDate().toString())]as?Int)?:0
                    val effective=if(admitted)g.copy(maxOpens=0)else g
                    val admissionOpens=(opens-if(handoff)1 else 0).coerceAtLeast(0)
                    reason=reason?:FocusWindows.reason(effective,now,used,admissionOpens)
                    if(reason!=null)return@forEach
                    if(g.sessionMinutes>0&&FocusWindows.limited(g,now)&&g.id !in sessionBase){
                        val prior=applicationContext.recentGroupSession(if(g.perApp||g.packages.isEmpty())setOf(pkg)else g.packages,g.cooldownSeconds)
                        if(prior==null){
                            if(g.limitAction=="Block")reason="Enable Usage Access for session limits"
                            else warnOnce(g.id,"Enable Usage Access for session warnings",now)
                            return@forEach
                        }
                        sessionBase[g.id]=if(!prior.active&&System.currentTimeMillis()-prior.lastActiveAt>=g.cooldownSeconds*1000L)0L else prior.milliseconds
                    }
                    val elapsed=(sessionBase[g.id]?:0L)+if(admitted)session else 0
                    reason=FocusWindows.reason(effective,now,used,admissionOpens,elapsed)
                    if(reason==null)FocusWindows.warning(g,now,used,opens,elapsed)?.let{warnOnce(g.id,it,now)}
                }
                if(reason!=null){showBlock(reason!!,0,null);scheduleDeadline(pkg,groups,usage,session,blocked=true);return@launch}
                val pause=if(!admitted&&!handoff)groups.filter{FocusWindows.limited(it,now)}.maxOfOrNull{FocusCodec.pauseDelay(it,(counts[FocusWindows.countKey(it,pkg,now.toLocalDate().toString())]as?Int)?:0)}?:0 else 0
                val intention=!admitted&&!handoff&&groups.any{it.pause&&it.requireIntention&&FocusWindows.limited(it,now)}
                if(pause>0||intention){showBlock("Look around",pause,onOpen={admit(pkg,groups,false)},requireIntention=intention);return@launch}
                if(!admitted)admit(pkg,groups,handoff)else scheduleDeadline(pkg,groups,usage,session)
            }catch(cancelled:CancellationException){throw cancelled}catch(_:Exception){showBlock("Rules unavailable. Return Home and check Usage Access.",0,null)}
        }
    }
    private fun admit(pkg:String,groups:List<FocusGroup>,alreadyCounted:Boolean){
        if(admission?.isActive==true)return
        val expected=generation
        val countedSnapshot=countedRules.toSet()
        admission=scope.launch{
            if(foreground!=pkg||expected!=generation)return@launch
            if(!alreadyCounted)withContext(Dispatchers.IO){val prefs=getSharedPreferences("focus_opens",MODE_PRIVATE);val edit=prefs.edit();val day=ZonedDateTime.now().toLocalDate().toString();prefs.all.keys.filterNot{it.startsWith("$day:")}.forEach{edit.remove(it)};groups.filter{it.id !in countedSnapshot&&FocusWindows.limited(it,ZonedDateTime.now())}.forEach{g->val key=FocusWindows.countKey(g,pkg,day);edit.putInt(key,prefs.getInt(key,0)+1)};kotlin.check(edit.commit())}
            if(foreground!=pkg||expected!=generation||!state.settings.ui.v2.backgroundRules)return@launch
            countedRules+=groups.map{it.id}
            handoffPending=false;admitted=true;sessionStarted=SystemClock.elapsedRealtime();removeOverlay()
            evaluate(false)
        }
    }
    private fun scheduleDeadline(pkg:String,groups:List<FocusGroup>,usage:Map<String,Long>?,session:Long,blocked:Boolean=false){
        timer?.cancel();val now=ZonedDateTime.now();val times=mutableListOf<Long>()
        groups.forEach{g->
            FocusWindows.nextBoundary(g,now)?.let(times::add)
            if(!blocked&&FocusWindows.limited(g,now)){
                if(g.sessionMinutes>0){
                    times+=FocusWindows.sessionBudget(g)-session-(sessionBase[g.id]?:0L)
                    if(g.warningMinutes>0)times+=(g.sessionMinutes-g.warningMinutes).coerceAtLeast(0)*60_000L-session-(sessionBase[g.id]?:0L)
                }
                if(g.dailyMinutes>0&&usage!=null){val used=if(g.perApp)usage[pkg]?:0 else g.packages.sumOf{usage[it]?:0}+if(g.packages.isEmpty())usage[pkg]?:0 else 0;times+=FocusWindows.dailyBudget(g)-used;if(g.warningMinutes>0)times+=(g.dailyMinutes-g.warningMinutes).coerceAtLeast(0)*60_000L-used}
            }
        }
        val p=state.policies[pkg]
        p?.dailyLimitMinutes?.let{limit->if(!blocked&&usage!=null)times+=limit*60_000L-(usage[pkg]?:0)}
        if(p?.scheduleEnabled==true){val w=FocusGroup(schedule=true,days=p.days,startMinute=p.startMinute,endMinute=p.endMinute);FocusWindows.nextBoundary(w,now)?.let(times::add)}
        times+=java.time.Duration.between(now,now.toLocalDate().plusDays(1).atStartOfDay(now.zone)).toMillis()
        val next=RuleDeadline.next(times)?:return
        timer=scope.launch{delay(next.coerceAtLeast(100));if(foreground==pkg)evaluate(false)}
    }
    private fun warnOnce(id:String,message:String,now:ZonedDateTime){
        val day=now.toLocalDate().toString()
        if(day!=warningDay){warningDay=day;warnedRules.clear()}
        if(warnedRules.add("$id:$message"))android.widget.Toast.makeText(this,message,android.widget.Toast.LENGTH_LONG).show()
    }
    private fun showBlock(reason:String,seconds:Int,onOpen:(()->Unit)?,requireIntention:Boolean=false){
        val key="$reason:$requireIntention"
        if(overlay!=null&&shownReason==key)return
        removeOverlay();shownReason=key
        val wm=getSystemService(WindowManager::class.java)
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setPadding(48,48,48,48);setBackgroundColor(Color.BLACK)}
        box.addView(TextView(this).apply{text=reason;setTextColor(Color.WHITE);textSize=28f;gravity=Gravity.CENTER})
        var ready=seconds==0
        val intention=if(requireIntention)android.widget.EditText(this).apply{
            hint="Your intention (not saved)";setTextColor(Color.WHITE);setHintTextColor(Color.LTGRAY)
            filters=arrayOf(android.text.InputFilter.LengthFilter(200));maxLines=3;box.addView(this)
        }else null
        val button=Button(this).apply{text=if(seconds>0)"$seconds"else if(onOpen!=null)"Open app"else "Blocked";isEnabled=ready&&onOpen!=null&&!requireIntention;setOnClickListener{onOpen?.invoke()}}
        intention?.addTextChangedListener{button.isEnabled=ready&&onOpen!=null&&!it.isNullOrBlank()}
        if(onOpen!=null)box.addView(button)
        box.addView(Button(this).apply{text="Home";setOnClickListener{performGlobalAction(GLOBAL_ACTION_HOME);removeOverlay()}})
        val params=WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,android.graphics.PixelFormat.TRANSLUCENT)
        try{wm.addView(box,params);overlay=box}catch(_:Exception){performGlobalAction(GLOBAL_ACTION_HOME);return}
        if(seconds>0)pauseJob=scope.launch{var remaining=seconds;while(remaining>0){delay(1000);remaining--;button.text=remaining.toString()};button.text="Open app";ready=true;button.isEnabled=!requireIntention||!intention?.text.isNullOrBlank()}
    }
    private fun removeOverlay(){pauseJob?.cancel();overlay?.let{runCatching{getSystemService(WindowManager::class.java).removeView(it)}};overlay=null;shownReason=""}
    override fun onInterrupt(){timer?.cancel();removeOverlay()}
    override fun onDestroy(){EnforcementBridge.connected=false;if(receiverRegistered){unregisterReceiver(screenReceiver);receiverRegistered=false};removeOverlay();scope.cancel();super.onDestroy()}
}
