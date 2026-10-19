package com.emmanuela.launcher.platform

import com.emmanuela.launcher.MainActivity
import com.emmanuela.launcher.R
import com.emmanuela.launcher.data.ConfigurationRepository
import com.emmanuela.launcher.data.NotificationMode


import android.Manifest
import android.app.Notification
import android.app.AppOpsManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

private val Context.digestStore by preferencesDataStore("notification_digest")
data class DigestCount(
    val packageName:String,val count:Int,val firstAt:Long,val lastAt:Long,val announced:Int=0,
    val reasons:Map<String,Int> = emptyMap(),val retainedAt:Long=firstAt
)
/** Stores counts and classification reasons only: no notification title, body, or URL. */
class DigestRepository(context:Context) {
    private val contextForActions=context.applicationContext
    private val store=contextForActions.digestStore
    private val key=stringPreferencesKey("counts")
    private val seenKey=stringPreferencesKey("seen")
    private val pendingKey=stringPreferencesKey("pending")
    private fun decode(raw:String?):List<DigestCount> {
        val rows=JSONArray(raw?:"[]")
        return List(rows.length()){i->rows.getJSONObject(i).let{o->
            val reasons=o.optJSONObject("reasons")
            DigestCount(o.getString("package"),o.getInt("count"),o.getLong("first"),o.getLong("last"),o.optInt("announced"),
                reasons?.keys()?.asSequence()?.associateWith{reasons.getInt(it)}?:emptyMap(),o.optLong("retained",o.getLong("first")))
        }}
    }
    private fun encode(rows:List<DigestCount>)=JSONArray().apply{rows.forEach{row->
        put(JSONObject().put("package",row.packageName).put("count",row.count).put("first",row.firstAt)
            .put("last",row.lastAt).put("announced",row.announced).put("reasons",JSONObject(row.reasons)).put("retained",row.retainedAt))
    }}.toString()
    private fun hash(identity:String)=MessageDigest.getInstance("SHA-256").digest(identity.toByteArray()).joinToString(""){"%02x".format(it)}
    private fun strings(raw:String?):List<String> = JSONArray(raw?:"[]").let{a->List(a.length()){a.getString(it)}}
    val counts=store.data.map{decode(it[key]).filter{row->System.currentTimeMillis()-row.retainedAt<TimeUnit.DAYS.toMillis(7)}}
        .catch{error->
            if(error is CancellationException)throw error
            NotificationDiagnostics.update(NotificationDiagnostics.status.value.connected,"Saved notification counts are unreadable. Clear counts to recover.")
            emit(emptyList())
        }.flowOn(Dispatchers.IO)
    /** Durable capture precedes dismissal. Only Android's listener-cancel callback commits a count. */
    suspend fun stage(pkg:String,identity:String,reason:String,capturedAt:Long=System.currentTimeMillis()):Boolean {
        var captured=false
        store.edit{prefs->
            val id=hash(identity)
            if(id !in strings(prefs[seenKey])){
                val pending=JSONArray(prefs[pendingKey]?:"[]")
                if((0 until pending.length()).none{pending.getJSONObject(it).getString("id")==id}){
                    pending.put(JSONObject().put("id",id).put("package",pkg).put("reason",reason).put("at",capturedAt))
                    prefs[pendingKey]=JSONArray((0 until pending.length()).map{pending.getJSONObject(it)}.takeLast(1000)).toString()
                }
                captured=true
            }
        }
        return captured
    }
    suspend fun removed(identity:String,confirmed:Boolean) {
        store.edit{prefs->
            val id=hash(identity)
            val pending=JSONArray(prefs[pendingKey]?:"[]")
            val records=List(pending.length()){pending.getJSONObject(it)}
            val record=records.firstOrNull{it.getString("id")==id}?:return@edit
            if(confirmed){
                val now=record.getLong("at")
                val rows=decode(prefs[key]).filter{System.currentTimeMillis()-it.retainedAt<TimeUnit.DAYS.toMillis(7)}.toMutableList()
                val pkg=record.getString("package");val reason=record.getString("reason")
                val index=rows.indexOfFirst{it.packageName==pkg}
                if(index<0)rows+=DigestCount(pkg,1,now,now,reasons=mapOf(reason to 1))
                else {val row=rows[index];rows[index]=row.copy(count=(row.count+1).coerceAtMost(99999),lastAt=now,
                    firstAt=if(row.count==row.announced)now else row.firstAt,
                    reasons=row.reasons+(reason to ((row.reasons[reason]?:0)+1)))}
                prefs[key]=encode(rows.takeLast(500))
                prefs[seenKey]=JSONArray((strings(prefs[seenKey])+id).takeLast(1000)).toString()
            }
            prefs[pendingKey]=JSONArray(records.filter{it.getString("id")!=id}).toString()
        }
    }
    suspend fun acknowledge(snapshot:List<DigestCount>){store.edit{prefs->
        prefs[key]=encode(decode(prefs[key]).map{row->
            val captured=snapshot.find{it.packageName==row.packageName}
            // A cleared/recreated package group must not inherit an older publication.
            if(captured==null||captured.retainedAt!=row.retainedAt)row
            else row.copy(announced=maxOf(row.announced,minOf(captured.count,row.count)))
        })
    }}
    suspend fun prune(){store.edit{prefs->
        prefs[key]=encode(decode(prefs[key]).filter{System.currentTimeMillis()-it.retainedAt<TimeUnit.DAYS.toMillis(7)})
        val pending=JSONArray(prefs[pendingKey]?:"[]")
        prefs[pendingKey]=JSONArray(List(pending.length()){pending.getJSONObject(it)}.filter{System.currentTimeMillis()-it.getLong("at")<TimeUnit.DAYS.toMillis(1)}).toString()
    }}
    suspend fun clear(){store.edit{it.remove(key);it.remove(seenKey);it.remove(pendingKey)};
        NotificationManagerCompat.from(contextForActions).cancel(200)
        NotificationDiagnostics.update(NotificationDiagnostics.status.value.connected)}
    suspend fun clearPackage(pkg:String){store.edit{prefs->
        prefs[key]=encode(decode(prefs[key]).filter{it.packageName!=pkg})
        val pending=JSONArray(prefs[pendingKey]?:"[]")
        prefs[pendingKey]=JSONArray(List(pending.length()){pending.getJSONObject(it)}.filter{it.getString("package")!=pkg}).toString()
    };NotificationManagerCompat.from(contextForActions).cancel(200)}
}

data class NotificationListenerStatus(val connected:Boolean=false,val issue:String?=null,val ruleRevision:Long=0)
object NotificationDiagnostics {
    private val mutable=kotlinx.coroutines.flow.MutableStateFlow(NotificationListenerStatus())
    val status:kotlinx.coroutines.flow.StateFlow<NotificationListenerStatus> = mutable
    internal fun update(connected:Boolean,issue:String?=null){mutable.update{it.copy(connected=connected,issue=issue)}}
    internal fun rulesChanged(){mutable.update{it.copy(ruleRevision=it.ruleRevision+1)}}
}

internal const val DIGEST_BATCH_TOKEN="emmanuela.digest.batch"
/** The listener callback is a publication signal; the worker still verifies the active record. */
internal object DigestPublication {
    private val observed=MutableStateFlow<String?>(null)
    fun posted(notification:Notification){notification.extras?.getString(DIGEST_BATCH_TOKEN)?.let{observed.value=it}}
    suspend fun await(token:String){observed.first{it==token}}
}

internal data class DigestDeliveryPermission(val notificationsEnabled:Boolean,val runtimeGranted:Boolean,val appOpMode:Int){
    val allowed:Boolean get()=notificationsEnabled&&runtimeGranted&&appOpMode in setOf(AppOpsManager.MODE_ALLOWED,AppOpsManager.MODE_DEFAULT)
}
@Suppress("DEPRECATION")
internal fun Context.digestDeliveryPermission():DigestDeliveryPermission {
    val appOps=getSystemService(AppOpsManager::class.java)
    // Android exposes this operation by its stable string, without a public OPSTR constant.
    val operation="android:post_notification"
    val mode=try{
        if(Build.VERSION.SDK_INT>=29)appOps.unsafeCheckOpRawNoThrow(operation,applicationInfo.uid,packageName)
        else appOps.checkOpNoThrow(operation,applicationInfo.uid,packageName)
    }catch(_:SecurityException){AppOpsManager.MODE_ERRORED}
    return DigestDeliveryPermission(NotificationManagerCompat.from(this).areNotificationsEnabled(),
        Build.VERSION.SDK_INT<33||ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED,mode)
}

open class EmmanuelaNotificationListener : NotificationListenerService() {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private val repository by lazy{ConfigurationRepository(applicationContext)}
    private val digest by lazy{DigestRepository(applicationContext)}
    private var configuration:com.emmanuela.launcher.data.LauncherData?=null
    private var connected=false
    private var boundary:Job?=null
    private val inFlight=mutableSetOf<String>()
    private val spam=com.emmanuela.launcher.data.NotificationSpamClassifier()
    private var receiverRegistered=false
    private val clockReceiver=object:android.content.BroadcastReceiver(){
        override fun onReceive(context:Context?,intent:Intent?){if(connected){refresh();scheduleBoundary()}}
    }
    override fun onListenerConnected() {
        scope.coroutineContext.cancelChildren()
        connected=true
        NotificationDiagnostics.update(true)
        NotificationBadges.clear()
        if(!receiverRegistered){
            ContextCompat.registerReceiver(this,clockReceiver,android.content.IntentFilter(Intent.ACTION_TIME_CHANGED).apply{addAction(Intent.ACTION_TIMEZONE_CHANGED)},ContextCompat.RECEIVER_NOT_EXPORTED)
            receiverRegistered=true
        }
        scope.launch {
            try { repository.data.collect { data ->
                configuration=data
                NotificationDiagnostics.rulesChanged()
                applicationContext.updateDigestWork(data.settings.ui.v2.notificationFilter && data.settings.ui.experience.digestEnabled)
                refresh()
                scheduleBoundary()
            }}catch(e:CancellationException){throw e}
            catch(_:java.io.IOException){NotificationDiagnostics.update(connected,"Cannot read notification rules. Check storage and reopen EmmanueLA.")}
        }
    }
    override fun onListenerDisconnected(){
        connected=false;configuration=null
        scope.coroutineContext.cancelChildren()
        NotificationBadges.clear();NotificationDiagnostics.update(false)
        try{requestRebind(android.content.ComponentName(this,com.emmanuela.launcher.EmmanuelaNotificationListener::class.java))}
        catch(_:SecurityException){NotificationDiagnostics.update(false,"Notification Access was revoked. Enable it in Android settings.")}
    }
    private fun refresh(){
        if(!connected)return
        try{activeNotifications?.let{rows->NotificationBadges.clear();rows.forEach(::onNotificationPosted)}}
        catch(_:SecurityException){NotificationDiagnostics.update(connected,"Notification Access is unavailable. Recheck Android settings.")}
    }
    private fun scheduleBoundary(){
        boundary?.cancel()
        val data=configuration?:return
        if(!connected||!data.settings.ui.v2.notificationFilter)return
        val now=java.time.ZonedDateTime.now()
        val deadlines=mutableListOf<Long>()
        data.policies.values.forEach{policy->
            val rule=policy.notificationRule
            val filtering=policy.notifications!=NotificationMode.NORMAL||
                !rule.spamAllowed&&(rule.spamMode?:data.settings.ui.experience.spamMode)!=com.emmanuela.launcher.data.SpamMode.OFF
            (rule.suppressUntil-now.toInstant().toEpochMilli()).takeIf{it>0}?.let(deadlines::add)
            if(filtering&&rule.scope==com.emmanuela.launcher.data.NotificationScope.SCHEDULE){
                com.emmanuela.launcher.data.FocusWindows.nextBoundary(com.emmanuela.launcher.data.FocusGroup(schedule=true,days=rule.days,startMinute=rule.startMinute,endMinute=rule.endMinute),now)?.let(deadlines::add)
            }
        }
        if(data.policies.values.any{(it.notifications!=NotificationMode.NORMAL||!it.notificationRule.spamAllowed&&(it.notificationRule.spamMode?:data.settings.ui.experience.spamMode)!=com.emmanuela.launcher.data.SpamMode.OFF)&&it.notificationRule.scope==com.emmanuela.launcher.data.NotificationScope.FOCUS})
            data.focusGroups.forEach{com.emmanuela.launcher.data.FocusWindows.nextBoundary(it,now)?.let(deadlines::add)}
        val next=deadlines.filter{it>0}.minOrNull()?:return
        boundary=scope.launch{delay(next.coerceAtLeast(100));refresh();scheduleBoundary()}
    }
    override fun onNotificationRemoved(sbn:StatusBarNotification?,rankingMap:RankingMap?,reason:Int){
        if(sbn==null)return
        NotificationBadges.removed(sbn.key)
        scope.launch(Dispatchers.IO){
            try{digest.removed(sbn.key+":"+sbn.postTime,reason==REASON_LISTENER_CANCEL)}
            catch(e:CancellationException){throw e}
            catch(_:Exception){NotificationDiagnostics.update(connected,"A dismissed notification is pending storage recovery. Reopen EmmanueLA.")}
        }
    }
    override fun onNotificationPosted(sbn:StatusBarNotification?){
        if(!connected||sbn==null)return
        if(sbn.packageName==packageName){if(sbn.id==200)DigestPublication.posted(sbn.notification);return}
        NotificationBadges.posted(sbn.key,sbn.packageName)
        if(!sbn.isClearable||sbn.isOngoing)return
        val n=sbn.notification
        if(n.flags and (Notification.FLAG_GROUP_SUMMARY or Notification.FLAG_FOREGROUND_SERVICE)!=0||n.category in setOf(Notification.CATEGORY_CALL,Notification.CATEGORY_ALARM,Notification.CATEGORY_TRANSPORT,Notification.CATEGORY_SERVICE,Notification.CATEGORY_NAVIGATION,Notification.CATEGORY_SYSTEM))return
        val data=configuration?:return
        if(!data.settings.ui.v2.notificationFilter)return
        val policy=data.policies[sbn.packageName]?:com.emmanuela.launcher.data.AppPolicy()
        val now=java.time.ZonedDateTime.now()
        val text by lazy {
            // Read only opted-in matching fields, locally. Never log or persist their content.
            runCatching{listOf(Notification.EXTRA_TITLE,Notification.EXTRA_TEXT,Notification.EXTRA_BIG_TEXT).joinToString("\n"){n.extras?.getCharSequence(it)?.toString().orEmpty().take(8_192)}.take(16_384)}.getOrDefault("")
        }
        val delivery=com.emmanuela.launcher.data.NotificationRules.action(policy,sbn.packageName,data.focusGroups,now,data.settings.ui.experience.digestEnabled){text}
        val rule=policy.notificationRule.copy(spamMode=policy.notificationRule.spamMode?:data.settings.ui.experience.spamMode)
        val spamActive=!rule.spamAllowed&&rule.spamMode!=com.emmanuela.launcher.data.SpamMode.OFF&&
            com.emmanuela.launcher.data.NotificationRules.applies(rule,sbn.packageName,data.focusGroups,now)
        if(delivery==NotificationMode.NORMAL&&!spamActive)return
        if(com.emmanuela.launcher.data.NotificationSpamClassifier.essentialText(text))return
        val spamReason=if(delivery==NotificationMode.NORMAL&&spamActive)
            spam.classify(sbn.packageName,sbn.key+":"+sbn.postTime,text,now.toInstant().toEpochMilli(),rule,sbn.postTime) else null
        val action=if(spamReason!=null)NotificationMode.DIGEST else delivery
        if(action==NotificationMode.NORMAL)return
        val identity=sbn.key+":"+sbn.postTime
        if(!inFlight.add(identity))return
        scope.launch {
            var reevaluate=false
            try {
                if(action==NotificationMode.DIGEST){
                    if(!withContext(Dispatchers.IO){digest.stage(sbn.packageName,identity,spamReason?:"Notification digest rule")})return@launch
                }
                val latest=configuration
                val currentTime=java.time.ZonedDateTime.now()
                val currentDelivery=latest?.let{com.emmanuela.launcher.data.NotificationRules.action(
                    it.policies[sbn.packageName]?:com.emmanuela.launcher.data.AppPolicy(),sbn.packageName,it.focusGroups,currentTime,it.settings.ui.experience.digestEnabled){text}}
                val stillApplies=if(spamReason==null)currentDelivery==action else currentDelivery==NotificationMode.NORMAL&&
                    com.emmanuela.launcher.data.NotificationRules.applies(rule,sbn.packageName,latest?.focusGroups.orEmpty(),currentTime)
                if(!connected||latest==null||!latest.settings.ui.v2.notificationFilter||
                    (latest.policies[sbn.packageName]?:com.emmanuela.launcher.data.AppPolicy())!=policy||latest.focusGroups!=data.focusGroups||
                    latest.settings.ui.experience.spamMode!=data.settings.ui.experience.spamMode||
                    latest.settings.ui.experience.digestEnabled!=data.settings.ui.experience.digestEnabled||!stillApplies){
                    if(action==NotificationMode.DIGEST)withContext(Dispatchers.IO){digest.removed(identity,false)}
                    reevaluate=connected&&latest?.settings?.ui?.v2?.notificationFilter==true
                    return@launch
                }
                cancelNotification(sbn.key)
            }catch(e:CancellationException){throw e}
            catch(_:SecurityException){
                if(action==NotificationMode.DIGEST)withContext(Dispatchers.IO){digest.removed(identity,false)}
                NotificationDiagnostics.update(connected,"Cannot dismiss notifications. Recheck Notification Access.")
            }catch(_:Exception){NotificationDiagnostics.update(connected,"Cannot save digest. Notification was left unchanged; check available storage.")}
            finally{inFlight.remove(identity);if(reevaluate)onNotificationPosted(sbn)}
        }
    }

    override fun onDestroy(){
        connected=false;configuration=null;NotificationDiagnostics.update(false);NotificationBadges.clear()
        if(receiverRegistered)unregisterReceiver(clockReceiver)
        scope.cancel();super.onDestroy()
    }
}
open class DigestWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result { return try {
        val context = applicationContext
        val data=ConfigurationRepository(context).data.first()
        if(!data.settings.ui.v2.notificationFilter||!data.settings.ui.experience.digestEnabled)return Result.success()
        val policies = data.policies
        val repository = DigestRepository(context)
        repository.prune()
        val now = System.currentTimeMillis()
        val due = repository.counts.first().filter { row ->
            val policy = policies[row.packageName]
            row.count > row.announced && now - row.firstAt >= com.emmanuela.launcher.data.NotificationRules.digestMinutes(policy,data.settings.ui.experience.digestInterval) * 60_000L
        }
        if (due.isNotEmpty() && context.digestDeliveryPermission().allowed) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel("ema_digest", "Batched notification summary", NotificationManager.IMPORTANCE_LOW))
            val intent = PendingIntent.getActivity(context, 20, Intent(context, MainActivity::class.java).putExtra("open_digest", true), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val count = due.sumOf { it.count - it.announced }
            if (manager.getNotificationChannel("ema_digest")?.importance == NotificationManager.IMPORTANCE_NONE) return Result.success()
            // Stable for this exact snapshot, so a confirmed prior retry can be acknowledged too.
            val identity=due.sortedBy{it.packageName}.joinToString("\n"){"${it.packageName}:${it.count}:${it.announced}:${it.firstAt}:${it.retainedAt}"}
            val token=MessageDigest.getInstance("SHA-256").digest(identity.toByteArray()).joinToString(""){"%02x".format(it)}
            fun published()=manager.activeNotifications.any{it.id==200&&it.notification.extras?.getString(DIGEST_BATCH_TOKEN)==token}
            if(published()){repository.acknowledge(due);return Result.success()}
            manager.notify(200, NotificationCompat.Builder(context, "ema_digest").setSmallIcon(com.emmanuela.launcher.R.drawable.ic_notification)
                .setContentTitle("Your notification summary").setContentText("$count notifications filtered. Tap to review counts.")
                .addExtras(android.os.Bundle().apply{putString(DIGEST_BATCH_TOKEN,token)})
                .setContentIntent(intent).setAutoCancel(true).setOnlyAlertOnce(true).build())
            if(!published()){
                if(NotificationDiagnostics.status.value.connected)withTimeoutOrNull(5_000){DigestPublication.await(token)}
                if(!context.digestDeliveryPermission().allowed||!published())return Result.retry()
            }
            repository.acknowledge(due)
        }
        Result.success()
    } catch (e: CancellationException) { throw e }
      catch (_: Exception) { Result.retry() }
    }
}
fun Context.updateDigestWork(enabled: Boolean) {
    val work = WorkManager.getInstance(this)
    if (enabled) work.enqueueUniquePeriodicWork("ema-digest", ExistingPeriodicWorkPolicy.UPDATE,
        PeriodicWorkRequestBuilder<DigestWorker>(15, TimeUnit.MINUTES).build())
    else work.cancelUniqueWork("ema-digest")
}
fun Context.notificationAccess() = packageName in NotificationManagerCompat.getEnabledListenerPackages(this)
