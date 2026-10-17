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
    fun browser(pkg:String,result:BrowserAdapters.Result){
        val issue=when(result.unavailable){
            BrowserAdapters.Unavailable.UNSUPPORTED_BROWSER->"Unsupported browser: website rules have no address adapter."
            BrowserAdapters.Unavailable.FOREIGN_WINDOW->"Browser window unavailable. Website rules cannot be verified."
            BrowserAdapters.Unavailable.MISSING_ADDRESS_FIELD->"Address control not exposed on this browser screen. Website rules cannot be verified."
            BrowserAdapters.Unavailable.EDITING_ADDRESS->"Address is being edited. Waiting for navigation before checking website rules."
            BrowserAdapters.Unavailable.TREE_ACCESS_FAILED->"Android accessibility tree query failed. Website rule enforcement cannot be verified."
            BrowserAdapters.Unavailable.UNREADABLE_ADDRESS->"Address control found, but no web address is readable. Website rules cannot be verified."
            null->null
        }
        mutableStatus.value=mutableStatus.value.copy(browserPackage=pkg,addressReadable=result.address!=null,issue=issue)
    }
    fun action(pkg:String?,outcome:EnforcementOutcome,reason:String?=null){
        mutableStatus.value=mutableStatus.value.copy(foregroundPackage=pkg,outcome=outcome,reason=reason)
    }
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
    private var foregroundRead:Job?=null
    private var foregroundReadPending=false
    private var permissionListener:android.app.AppOpsManager.OnOpChangedListener?=null
    private var receiverRegistered=false
    private val screenReceiver=object:BroadcastReceiver(){
        override fun onReceive(context:Context?,intent:Intent?){
            when(intent?.action){
                Intent.ACTION_SCREEN_OFF->leaveForeground()
                Intent.ACTION_SCREEN_ON,Intent.ACTION_USER_PRESENT->refreshForeground()
                Intent.ACTION_TIME_CHANGED,Intent.ACTION_TIMEZONE_CHANGED->{timer?.cancel();evaluate()}
            }
        }
    }
    private val sessionBase=mutableMapOf<String,Long>()
    private val sessionAnchors=mutableMapOf<String,Long>()
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
        super.onServiceConnected()
        EnforcementBridge.connected=true
        if(!receiverRegistered){
            ContextCompat.registerReceiver(this,screenReceiver,IntentFilter(Intent.ACTION_SCREEN_OFF).apply {
                addAction(Intent.ACTION_SCREEN_ON);addAction(Intent.ACTION_USER_PRESENT)
                addAction(Intent.ACTION_TIME_CHANGED);addAction(Intent.ACTION_TIMEZONE_CHANGED)
            },ContextCompat.RECEIVER_NOT_EXPORTED)
            receiverRegistered=true
        }
        // Usage access can be revoked while a limited app remains open.
        if(permissionListener==null){
            permissionListener=android.app.AppOpsManager.OnOpChangedListener { operation,pkg ->
                if(operation==android.app.AppOpsManager.OPSTR_GET_USAGE_STATS && pkg==packageName)
                    scope.launch { sessionBase.clear();sessionAnchors.clear();evaluate() }
            }
            getSystemService(android.app.AppOpsManager::class.java).startWatchingMode(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,packageName,permissionListener!!)
        }
        configurationJob?.cancel()
        configurationJob=scope.launch {
            ConfigurationRepository(applicationContext).data.collect { next ->
                val changed=next.focusGroups!=state.focusGroups||next.policies!=state.policies||next.folders!=state.folders
                val enabling=!state.settings.ui.v2.backgroundRules&&next.settings.ui.v2.backgroundRules
                if(changed){
                    generation++;check?.cancel();admission?.cancel();timer?.cancel();removeOverlay()
                    admitted=false;handoffPending=false;sessionBase.clear();sessionAnchors.clear();warnedRules.clear()
                }
                state=next
                if(!next.settings.ui.v2.backgroundRules)leaveForeground()
                else if(enabling||foreground.isEmpty())refreshForeground()
                else {evaluate();if(hasWebsiteRules(foreground))readBrowserAddress(foreground)}
            }
        }
    }

    private fun hasWebsiteRules(pkg:String)=state.focusGroups.any {
        pkg in it.browsers&&(it.websites.isNotEmpty()||it.keywords.isNotEmpty())
    }

    override fun onAccessibilityEvent(event:AccessibilityEvent?){
        if(event==null||!state.settings.ui.v2.backgroundRules)return
        when(event.eventType){
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,AccessibilityEvent.TYPE_WINDOWS_CHANGED -> refreshForeground()
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED,AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> if(event.packageName?.toString()==foreground&&hasWebsiteRules(foreground))readBrowserAddress(foreground)
        }
    }

    /** Reconcile with actual application windows, including already-open apps on connect/unlock. */
    private fun refreshForeground(){
        foregroundReadPending=true
        if(foregroundRead?.isActive==true)return
        foregroundRead=scope.launch {
            do {
                foregroundReadPending=false
                val expected=generation
                val pkg=try {
                    withContext(Dispatchers.IO){
                        if(!getSystemService(android.os.PowerManager::class.java).isInteractive||
                            getSystemService(android.app.KeyguardManager::class.java).isKeyguardLocked)null
                        else activeApplicationRoot()?.let { root ->
                            try { root.packageName?.toString() } finally { @Suppress("DEPRECATION") root.recycle() }
                        }
                    }
                }catch(cancelled:CancellationException){throw cancelled}catch(_:Exception){
                    EnforcementBridge.action(foreground,EnforcementOutcome.FAILED,"Android application window query failed. Background rules cannot be verified.")
                    return@launch
                }
                if(expected!=generation||!state.settings.ui.v2.backgroundRules)continue
                if(pkg==null){leaveForeground();continue}
                // Permission, keyguard and emergency system controls must remain reachable.
                if(pkg in systemPackages){leaveForeground();continue}
                if(pkg!=foreground){
                    leaveForeground()
                    foreground=pkg;handoffPending=EnforcementBridge.consume(pkg)
                    EnforcementBridge.action(pkg,EnforcementOutcome.IDLE)
                    evaluate()
                }
                if(hasWebsiteRules(pkg))readBrowserAddress(pkg)
            }while(foregroundReadPending)
        }
    }

    private fun activeApplicationRoot():android.view.accessibility.AccessibilityNodeInfo? {
        // Our accessibility overlay can own rootInActiveWindow. Query application windows
        // beneath it so URL changes and rule updates never mistake it for the launcher.
        val snapshot=windows
        try {
            val application=snapshot.filter { it.type==android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION }
            val selected=application.firstOrNull { it.isFocused }?:application.firstOrNull { it.isActive }
                ?:if(overlay!=null)application.maxByOrNull { it.layer }else null
            selected?.root?.let { return it }
            val root=rootInActiveWindow
            if(overlay!=null&&root?.packageName?.toString()==packageName){
                @Suppress("DEPRECATION") root.recycle()
                return null
            }
            return root
        }finally {
            snapshot.forEach { @Suppress("DEPRECATION") it.recycle() }
        }
    }

    private fun leaveForeground(){
        generation++;check?.cancel();admission?.cancel();timer?.cancel();browserRead?.cancel();browserRead=null
        pauseJob?.cancel();removeOverlay();foreground="";currentUrl="";admitted=false;handoffPending=false
        sessionBase.clear();sessionAnchors.clear();countedRules.clear();warnedRules.clear();activeRules=emptySet();warnedAddress=false
        EnforcementBridge.action(null,EnforcementOutcome.IDLE)
    }

    private fun readBrowserAddress(pkg:String){
        browserReadPending=true
        if(browserRead?.isActive==true)return
        browserRead=scope.launch {
            do {
                browserReadPending=false
                val expected=generation
                val (owner,result)=withContext(Dispatchers.IO){
                    try {
                        val root=activeApplicationRoot()
                        try { root?.packageName?.toString() to BrowserAdapters.read(root,pkg) } finally { @Suppress("DEPRECATION") root?.recycle() }
                    }catch(_:Exception){null to BrowserAdapters.Result(unavailable=BrowserAdapters.Unavailable.TREE_ACCESS_FAILED)}
                }
                if(expected!=generation||foreground!=pkg||!state.settings.ui.v2.backgroundRules)return@launch
                // Window callbacks and address reads reconcile independently. A new
                // application's root is not a failed read of the previous browser.
                if(owner!=null&&owner!=pkg){refreshForeground();return@launch}
                // Our verified blocking overlay can obscure the toolbar, including
                // during a foreground transition. Preserve its last browser result
                // until a real browser read or foreground reconciliation completes.
                if(result.unavailable!=null&&result.unavailable!=BrowserAdapters.Unavailable.EDITING_ADDRESS&&
                    overlay!=null&&currentUrl.isNotEmpty())continue
                EnforcementBridge.browser(pkg,result)
                if(result.unavailable!=null&&result.unavailable!=BrowserAdapters.Unavailable.EDITING_ADDRESS&&!warnedAddress){
                    warnedAddress=true
                    android.widget.Toast.makeText(this@RuleEnforcementService,EnforcementBridge.status.value.issue,android.widget.Toast.LENGTH_LONG).show()
                }
                // Editing is not a new navigation. Keep an already-enforced page covered.
                if(result.unavailable==BrowserAdapters.Unavailable.EDITING_ADDRESS)continue
                if(result.address.orEmpty()!=currentUrl){currentUrl=result.address.orEmpty();evaluate()}
            }while(browserReadPending)
        }
    }

    private fun groups(pkg:String)=FocusTargets.groups(state.focusGroups,pkg,currentUrl)
    private fun evaluate(){
        val pkg=foreground
        if(pkg.isEmpty()||pkg==packageName){removeOverlay();return}
        check?.cancel()
        check=scope.launch{
            try{
                val policy=state.policies[pkg]?:AppPolicy()
                val groups=groups(pkg)
                val protectedFolder=state.folders.any{it.isProtected&&it.apps.any{id->id.substringBefore('/')==pkg}}
                if(groups.isEmpty()&&policy==AppPolicy()&&!protectedFolder){removeOverlay();timer?.cancel();admitted=false;sessionBase.clear();sessionAnchors.clear();activeRules=emptySet();EnforcementBridge.action(pkg,EnforcementOutcome.ALLOWED);return@launch}
                val ids=groups.map{it.id}.toSet()
                if(ids!=activeRules){
                    sessionBase.keys.toList().forEach { sessionBase[it]=sessionElapsed(it) }
                    admitted=false;activeRules=ids;sessionBase.keys.retainAll(ids);sessionAnchors.keys.retainAll(ids)
                }
                val handoff=handoffPending
                if(!handoff&&!admitted&&(policy.privateApp||protectedFolder)){
                    showBlock("Confirm your identity in EmmanueLA",0,onOpen={removeOverlay();startActivity(Intent(this@RuleEnforcementService,com.emmanuela.launcher.MainActivity::class.java).putExtra("authenticate_package",pkg).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))});return@launch
                }
                val now=ZonedDateTime.now()
                val usage=withContext(Dispatchers.IO){applicationContext.appUsageToday()}
                val counts=withContext(Dispatchers.IO){getSharedPreferences("focus_opens",MODE_PRIVATE).all}
                var reason=PolicyRules.reason(policy,now,usage,pkg)
                groups.forEach{g->
                    val used=FocusTargets.used(g,pkg,usage)
                    val opens=(counts[FocusWindows.countKey(g,pkg,now.toLocalDate().toString())]as?Int)?:0
                    val effective=if(admitted)g.copy(maxOpens=0)else g
                    val admissionOpens=(opens-if(handoff)1 else 0).coerceAtLeast(0)
                    reason=reason?:FocusWindows.reason(effective,now,used,admissionOpens)
                    if(reason!=null)return@forEach
                    if(g.sessionMinutes>0&&FocusWindows.limited(g,now)&&g.id !in sessionBase){
                        val prior=applicationContext.recentGroupSession(FocusTargets.usagePackages(g,pkg),g.cooldownSeconds)
                        if(prior==null){
                            if(g.limitAction=="Block")reason="Enable Usage Access for session limits"
                            else warnOnce(g.id,"Enable Usage Access for session warnings",now)
                            return@forEach
                        }
                        sessionBase[g.id]=if(!prior.active&&System.currentTimeMillis()-prior.lastActiveAt>=g.cooldownSeconds*1000L)0L else prior.milliseconds
                        sessionAnchors[g.id]=SystemClock.elapsedRealtime()
                    }
                    val elapsed=sessionElapsed(g.id)
                    reason=FocusWindows.reason(effective,now,used,admissionOpens,elapsed)
                    if(reason==null)FocusWindows.warning(g,now,used,opens,elapsed)?.let{warnOnce(g.id,it,now)}
                }
                if(reason!=null){showBlock(reason!!,0,null);scheduleDeadline(pkg,groups,usage,blocked=true);return@launch}
                val pause=if(!admitted&&!handoff)groups.filter{FocusWindows.limited(it,now)}.maxOfOrNull{FocusCodec.pauseDelay(it,(counts[FocusWindows.countKey(it,pkg,now.toLocalDate().toString())]as?Int)?:0)}?:0 else 0
                val intention=!admitted&&!handoff&&groups.any{it.pause&&it.requireIntention&&FocusWindows.limited(it,now)}
                if(pause>0||intention){showBlock("Look around",pause,onOpen={admit(pkg,groups,false)},requireIntention=intention);return@launch}
                if(!admitted)admit(pkg,groups,handoff)else {removeOverlay();EnforcementBridge.action(pkg,EnforcementOutcome.ALLOWED);scheduleDeadline(pkg,groups,usage)}
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
            handoffPending=false;admitted=true;sessionAnchors.keys.toList().forEach { sessionAnchors[it]=SystemClock.elapsedRealtime() };removeOverlay()
            EnforcementBridge.action(pkg,EnforcementOutcome.ALLOWED)
            evaluate()
        }
    }
    private fun sessionElapsed(id:String):Long = (sessionBase[id]?:0L)+
        if(admitted)sessionAnchors[id]?.let { (SystemClock.elapsedRealtime()-it).coerceAtLeast(0) }?:0L else 0L
    private fun scheduleDeadline(pkg:String,groups:List<FocusGroup>,usage:Map<String,Long>?,blocked:Boolean=false){
        timer?.cancel();val now=ZonedDateTime.now();val times=mutableListOf<Long>()
        groups.forEach{g->
            FocusWindows.nextBoundary(g,now)?.let(times::add)
            if(!blocked&&FocusWindows.limited(g,now)){
                if(g.sessionMinutes>0){
                    times+=FocusWindows.sessionBudget(g)-sessionElapsed(g.id)
                    if(g.warningMinutes>0)times+=(g.sessionMinutes-g.warningMinutes).coerceAtLeast(0)*60_000L-sessionElapsed(g.id)
                }
                if(g.dailyMinutes>0&&usage!=null){val used=FocusTargets.used(g,pkg,usage)?:0L;times+=FocusWindows.dailyBudget(g)-used;if(g.warningMinutes>0)times+=(g.dailyMinutes-g.warningMinutes).coerceAtLeast(0)*60_000L-used}
            }
        }
        val p=state.policies[pkg]
        p?.dailyLimitMinutes?.let{limit->if(!blocked&&usage!=null)times+=limit*60_000L-(usage[pkg]?:0)}
        if(p?.scheduleEnabled==true){val w=FocusGroup(schedule=true,days=p.days,startMinute=p.startMinute,endMinute=p.endMinute);FocusWindows.nextBoundary(w,now)?.let(times::add)}
        times+=java.time.Duration.between(now,now.toLocalDate().plusDays(1).atStartOfDay(now.zone)).toMillis()
        val next=RuleDeadline.next(times)?:return
        timer=scope.launch{delay(next.coerceAtLeast(100));if(foreground==pkg)evaluate()}
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
        box.addView(Button(this).apply{text="Home";setOnClickListener{
            val sent=performGlobalAction(GLOBAL_ACTION_HOME)
            EnforcementBridge.action(this@RuleEnforcementService.foreground,if(sent)EnforcementOutcome.HOME_REQUESTED else EnforcementOutcome.FAILED,
                if(sent)"Return Home requested"else "Android rejected the Home action")
            if(sent)removeOverlay()
        }})
        val params=WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,android.graphics.PixelFormat.TRANSLUCENT)
        try{
            wm.addView(box,params);overlay=box
            EnforcementBridge.action(foreground,EnforcementOutcome.OVERLAY_SHOWN,reason)
        }catch(_:Exception){
            val sent=performGlobalAction(GLOBAL_ACTION_HOME)
            EnforcementBridge.action(this@RuleEnforcementService.foreground,if(sent)EnforcementOutcome.HOME_REQUESTED else EnforcementOutcome.FAILED,
                if(sent)"Overlay unavailable; return Home requested"else "Android rejected the blocking overlay and Home action")
            return
        }
        if(seconds>0)pauseJob=scope.launch{var remaining=seconds;while(remaining>0){delay(1000);remaining--;button.text=remaining.toString()};button.text="Open app";ready=true;button.isEnabled=!requireIntention||!intention?.text.isNullOrBlank()}
    }
    private fun removeOverlay(){pauseJob?.cancel();overlay?.let{runCatching{getSystemService(WindowManager::class.java).removeView(it)}};overlay=null;shownReason=""}
    override fun onInterrupt(){
        leaveForeground()
        // Android calls onServiceConnected again after a new binding, not after every interrupt.
        if(state.settings.ui.v2.backgroundRules)refreshForeground()
    }
    override fun onUnbind(intent:Intent?):Boolean {
        EnforcementBridge.connected=false
        leaveForeground()
        return super.onUnbind(intent)
    }
    override fun onDestroy(){
        EnforcementBridge.connected=false
        if(receiverRegistered){unregisterReceiver(screenReceiver);receiverRegistered=false}
        permissionListener?.let { getSystemService(android.app.AppOpsManager::class.java).stopWatchingMode(it) }
        permissionListener=null;leaveForeground();scope.cancel();super.onDestroy()
    }
    private companion object {
        val systemPackages=setOf("com.android.systemui","com.android.settings","com.google.android.permissioncontroller","com.android.permissioncontroller","com.android.phone","com.android.server.telecom","com.android.emergency")
    }
}
