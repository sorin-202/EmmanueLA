package com.emmanuela.launcher

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.emmanuela.launcher.data.*
import com.emmanuela.launcher.platform.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationListenerTest {
    @Test fun realListenerFiltersKeywordsPreservesImportantNotificationsAndReconnects()=runBlocking {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val target=instrumentation.targetContext
        val publisher=instrumentation.context
        val repository=ConfigurationRepository(target)
        val original=repository.data.first()
        val wasGranted=target.notificationAccess()
        val component="${target.packageName}/${target.packageName}.EmmanuelaNotificationListener"
        fun shell(command:String):String=instrumentation.uiAutomation.executeShellCommand(command).use{android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes().toString(Charsets.UTF_8)}
        val receiver="${publisher.packageName}/com.emmanuela.launcher.NotificationPublisher"
        fun publishCommand(extras:String):String {
            val result=shell("am broadcast -f 32 -n $receiver $extras")
            check(result.contains("data=\"|")){"Publisher did not return notification IDs: $result"}
            return result
        }
        fun present(id:Int)=publishCommand("--es operation list").contains("|$id|")
        suspend fun waitFor(stage:String,predicate:()->Boolean){ assertTrue("$stage; listener=${NotificationDiagnostics.status.value}",withTimeoutOrNull(15_000){while(!predicate())delay(100);true}==true) }
        fun post(id:Int,text:String,category:String?=null){publishCommand("--es operation post --ei id $id --es text $text"+(category?.let{" --es category $it"}?:""))}
        try{
            if(Build.VERSION.SDK_INT>=33)shell("pm grant ${publisher.packageName} android.permission.POST_NOTIFICATIONS")
            repository.restore(LauncherData(settings=Preferences(ui=UiPreferences(v2=V2Preferences(notificationFilter=true))),policies=mapOf(publisher.packageName to AppPolicy(notifications=NotificationMode.DISMISS,notificationRule=NotificationRule(keywords=setOf("sale"))))))
            shell("cmd notification allow_listener $component")
            waitFor("listener connected"){NotificationDiagnostics.status.value.connected}
            post(6101,"sale_today")
            post(6102,"personal_message")
            post(6103,"sale_alarm",Notification.CATEGORY_ALARM)
            waitFor("keyword filtering and alarm preservation"){present(6102)&&present(6103)&&!present(6101)}
            // A nonmatching notification remains; now a rule change must reevaluate it.
            repository.update{it.copy(policies=mapOf(publisher.packageName to AppPolicy(notifications=NotificationMode.DISMISS)))}
            waitFor("rule change removes existing notification"){!present(6102)}
            assertTrue(present(6103))
            shell("cmd notification disallow_listener $component")
            waitFor("listener disconnected"){!NotificationDiagnostics.status.value.connected}
            post(6104,"after_access_revoked")
            waitFor("revoked listener leaves notifications"){present(6104)}
            shell("cmd notification allow_listener $component")
            waitFor("listener connected"){NotificationDiagnostics.status.value.connected}
            waitFor("reconnected listener filters existing notification"){!present(6104)}
            assertFalse(ConfigurationCodec.encode(repository.data.first()).contains("personal_message"))
        }finally{
            publishCommand("--es operation clear");repository.restore(original)
            if(!wasGranted)shell("cmd notification disallow_listener $component")
        }
    }
}
