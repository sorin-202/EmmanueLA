package com.emmanuela.launcher.platform

import com.emmanuela.launcher.MainActivity
import com.emmanuela.launcher.R
import com.emmanuela.launcher.data.ConfigurationRepository
import com.emmanuela.launcher.data.NotificationMode


import android.Manifest
import android.app.Notification
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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

private val Context.digestStore by preferencesDataStore("notification_digest")
data class DigestCount(val packageName: String, val count: Int, val firstAt: Long, val lastAt: Long, val announced: Int = 0)
class DigestRepository(context: Context) {
    private val store = context.applicationContext.digestStore
    private val key = stringPreferencesKey("counts")
    private val seenKey = stringPreferencesKey("seen")
    private fun decode(raw: String?): List<DigestCount> {
        val rows = JSONArray(raw ?: "[]")
        return List(rows.length()) { i -> rows.getJSONObject(i).let {
            DigestCount(it.getString("package"), it.getInt("count"), it.getLong("first"), it.getLong("last"), it.optInt("announced"))
        } }
    }
    private fun encode(rows: List<DigestCount>) = JSONArray().apply { rows.forEach {
        put(JSONObject().put("package", it.packageName).put("count", it.count).put("first", it.firstAt).put("last", it.lastAt).put("announced", it.announced))
    } }.toString()
    val counts = store.data.map { decode(it[key]) }.flowOn(Dispatchers.IO)
    suspend fun record(pkg: String, identity: String) {
        val hash = MessageDigest.getInstance("SHA-256").digest(identity.toByteArray()).joinToString("") { "%02x".format(it) }
        store.edit { prefs ->
            val seen = JSONArray(prefs[seenKey] ?: "[]")
            val identities = List(seen.length()) { seen.getString(it) }
            if (hash !in identities) {
                val now = System.currentTimeMillis()
                val rows = decode(prefs[key]).filter { now - it.lastAt < TimeUnit.DAYS.toMillis(7) }.toMutableList()
                val index = rows.indexOfFirst { it.packageName == pkg }
                if (index < 0) rows += DigestCount(pkg, 1, now, now)
                else rows[index] = rows[index].copy(count = (rows[index].count + 1).coerceAtMost(99999), lastAt = now,
                    firstAt = if (rows[index].count == rows[index].announced) now else rows[index].firstAt)
                prefs[key] = encode(rows.takeLast(500))
                prefs[seenKey] = JSONArray((identities + hash).takeLast(1000)).toString()
            }
        }
    }
    suspend fun acknowledge(snapshot: List<DigestCount>) { store.edit { prefs ->
        prefs[key] = encode(decode(prefs[key]).map { row ->
            val count = snapshot.find { it.packageName == row.packageName }?.count
            if (count == null) row else row.copy(announced = maxOf(row.announced, minOf(count, row.count)))
        })
    } }
    suspend fun clear() { store.edit { it.remove(key); it.remove(seenKey) } }
}

data class NotificationListenerStatus(val connected:Boolean=false,val issue:String?=null)
object NotificationDiagnostics {
    private val mutable=kotlinx.coroutines.flow.MutableStateFlow(NotificationListenerStatus())
    val status:kotlinx.coroutines.flow.StateFlow<NotificationListenerStatus> = mutable
    internal fun update(connected:Boolean,issue:String?=null){mutable.value=NotificationListenerStatus(connected,issue)}
}

open class EmmanuelaNotificationListener : NotificationListenerService() {
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.Main.immediate)
    private val repository by lazy{ConfigurationRepository(applicationContext)}
    private val digest by lazy{DigestRepository(applicationContext)}
    private var configuration:com.emmanuela.launcher.data.LauncherData?=null
    private var connected=false
    private var boundary:Job?=null
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
            if(policy.notifications!=NotificationMode.NORMAL&&rule.scope==com.emmanuela.launcher.data.NotificationScope.SCHEDULE){
                com.emmanuela.launcher.data.FocusWindows.nextBoundary(com.emmanuela.launcher.data.FocusGroup(schedule=true,days=rule.days,startMinute=rule.startMinute,endMinute=rule.endMinute),now)?.let(deadlines::add)
            }
        }
        if(data.policies.values.any{it.notifications!=NotificationMode.NORMAL&&it.notificationRule.scope==com.emmanuela.launcher.data.NotificationScope.FOCUS})
            data.focusGroups.forEach{com.emmanuela.launcher.data.FocusWindows.nextBoundary(it,now)?.let(deadlines::add)}
        val next=deadlines.filter{it>0}.minOrNull()?:return
        boundary=scope.launch{delay(next.coerceAtLeast(100));refresh();scheduleBoundary()}
    }
    override fun onNotificationRemoved(sbn:StatusBarNotification?){sbn?.let{NotificationBadges.removed(it.key)}}
    override fun onNotificationPosted(sbn:StatusBarNotification?){
        if(!connected||sbn==null||sbn.packageName==packageName)return
        NotificationBadges.posted(sbn.key,sbn.packageName)
        if(!sbn.isClearable||sbn.isOngoing)return
        val n=sbn.notification
        if(n.flags and Notification.FLAG_GROUP_SUMMARY!=0||n.category in setOf(Notification.CATEGORY_CALL,Notification.CATEGORY_ALARM,Notification.CATEGORY_TRANSPORT,Notification.CATEGORY_SERVICE))return
        val data=configuration?:return
        if(!data.settings.ui.v2.notificationFilter)return
        val policy=data.policies[sbn.packageName]?:return
        val action=com.emmanuela.launcher.data.NotificationRules.action(policy,sbn.packageName,data.focusGroups,java.time.ZonedDateTime.now(),data.settings.ui.experience.digestEnabled){
            // Read only opted-in matching fields, locally. Never log or persist their content.
            listOf(Notification.EXTRA_TITLE,Notification.EXTRA_TEXT,Notification.EXTRA_BIG_TEXT).joinToString("\n"){n.extras.getCharSequence(it)?.toString().orEmpty()}
        }
        if(action==NotificationMode.NORMAL)return
        try{cancelNotification(sbn.key)}
        catch(_:SecurityException){NotificationDiagnostics.update(connected,"Cannot dismiss notifications. Recheck Notification Access.");return}
        if(action==NotificationMode.DIGEST)scope.launch {
            try{withContext(Dispatchers.IO){digest.record(sbn.packageName,sbn.key+":"+sbn.postTime)}}
            catch(e:CancellationException){throw e}
            catch(_:java.io.IOException){NotificationDiagnostics.update(connected,"A filtered count could not be saved. Check available storage.")}
            catch(_:org.json.JSONException){NotificationDiagnostics.update(connected,"Saved digest counts are unreadable. Clear the digest in EmmanueLA.")}
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
        val now = System.currentTimeMillis()
        val due = repository.counts.first().filter { row ->
            val policy = policies[row.packageName]
            policy?.notifications == NotificationMode.DIGEST && row.count > row.announced && now - row.firstAt >= data.settings.ui.experience.digestInterval * 60_000L
        }
        if (due.isNotEmpty() && NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel("ema_digest", "Batched notification summary", NotificationManager.IMPORTANCE_LOW))
            val intent = PendingIntent.getActivity(context, 20, Intent(context, MainActivity::class.java).putExtra("open_digest", true), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val count = due.sumOf { it.count - it.announced }
            if (manager.getNotificationChannel("ema_digest")?.importance == NotificationManager.IMPORTANCE_NONE) return Result.success()
            manager.notify(200, NotificationCompat.Builder(context, "ema_digest").setSmallIcon(com.emmanuela.launcher.R.drawable.ic_notification)
                .setContentTitle("Your notification summary").setContentText("$count notifications filtered. Tap to review counts.")
                .setContentIntent(intent).setAutoCancel(true).setOnlyAlertOnce(true).build())
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
