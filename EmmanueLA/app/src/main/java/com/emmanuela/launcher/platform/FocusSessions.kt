package com.emmanuela.launcher.platform

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.emmanuela.launcher.MainActivity
import com.emmanuela.launcher.R
import com.emmanuela.launcher.data.ConfigurationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/** A session spans foreground group use separated by less than a minute of rest. */
data class GroupSession(val milliseconds:Long,val lastActiveAt:Long,val active:Boolean)
object SessionMath {
    fun recent(events:List<AppUsageEvent>,packages:Set<String>,end:Long):GroupSession {
        val active=mutableSetOf<Pair<String,String>>()
        var previous=events.firstOrNull()?.at?:end
        var duration=0L
        var lastActive=0L
        for(event in events){
            if(event.at>end)break
            if(active.isNotEmpty()) {duration+=(event.at-previous).coerceAtLeast(0);lastActive=event.at}
            if(event.clearAll)active.clear()
            else if(event.packageName in packages){
                val key=event.packageName to event.activity
                if(event.resumed){
                    if(active.isEmpty()&&event.at-lastActive>=60_000L)duration=0L
                    active+=key
                }else active-=key
            }
            previous=event.at
        }
        if(active.isNotEmpty()){duration+=(end-previous).coerceAtLeast(0);lastActive=end}
        return GroupSession(duration,lastActive,active.isNotEmpty())
    }
}
@Suppress("DEPRECATION")
suspend fun Context.recentGroupSession(packages:Set<String>):GroupSession?=withContext(Dispatchers.IO){
    if(!hasUsageAccess())return@withContext null
    val end=System.currentTimeMillis()
    val cursor=getSystemService(UsageStatsManager::class.java).queryEvents(end-86_400_000L,end)?:return@withContext null
    val event=UsageEvents.Event();val rows=mutableListOf<AppUsageEvent>()
    while(cursor.hasNextEvent()){
        cursor.getNextEvent(event)
        when(event.eventType){
            UsageEvents.Event.MOVE_TO_FOREGROUND,UsageEvents.Event.MOVE_TO_BACKGROUND ->
                if(event.packageName in packages)rows+=AppUsageEvent(event.timeStamp,event.packageName.orEmpty(),event.className.orEmpty(),event.eventType==UsageEvents.Event.MOVE_TO_FOREGROUND)
            UsageEvents.Event.SCREEN_NON_INTERACTIVE -> rows+=AppUsageEvent(event.timeStamp,"","",false,true)
        }
    }
    SessionMath.recent(rows,packages,end)
}
/** One deferred check, not a continuous monitor. Android may defer this reminder in Doze. */
fun Context.scheduleSessionReminder(groupId:String,remainingMs:Long){
    WorkManager.getInstance(this).enqueueUniqueWork("session:$groupId",ExistingWorkPolicy.REPLACE,
        OneTimeWorkRequestBuilder<SessionReminderWorker>().setInputData(workDataOf("group" to groupId))
            .addTag("focus-session").setInitialDelay(remainingMs,TimeUnit.MILLISECONDS).build())
}
class SessionReminderWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params){
    override suspend fun doWork():Result {
        val context=applicationContext
        val group=ConfigurationRepository(context).data.first().focusGroups.find{it.id==inputData.getString("group")}?:return Result.success()
        if(group.sessionMinutes==0)return Result.success()
        val session=context.recentGroupSession(group.packages)?:return Result.success()
        if(!session.active||session.milliseconds<group.sessionMinutes*60_000L)return Result.success()
        if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return Result.success()
        val manager=context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("focus_sessions","Session reminders",NotificationManager.IMPORTANCE_DEFAULT))
        val pending=PendingIntent.getActivity(context,group.id.hashCode(),Intent(context,MainActivity::class.java).addCategory(Intent.CATEGORY_HOME),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify(group.id.hashCode(),NotificationCompat.Builder(context,"focus_sessions").setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(group.name).setContentText("Session limit reached. Take a moment; tap to return Home.").setContentIntent(pending).setAutoCancel(true).build())
        return Result.success()
    }
}
