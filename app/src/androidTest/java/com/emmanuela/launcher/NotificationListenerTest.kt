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
            val digest=DigestRepository(target)
            digest.clearPackage(publisher.packageName)
            val revision=NotificationDiagnostics.status.value.ruleRevision
            repository.update{it.copy(settings=it.settings.copy(ui=it.settings.ui.copy(experience=it.settings.ui.experience.copy(digestEnabled=true))),
                policies=mapOf(publisher.packageName to AppPolicy(notifications=NotificationMode.DIGEST)))}
            waitFor("digest rule applied"){NotificationDiagnostics.status.value.ruleRevision>revision}
            post(6105,"digest_capture")
            waitFor("digest dismisses original"){!present(6105)}
            assertEquals(1,withTimeout(5_000){digest.counts.first{rows->rows.any{it.packageName==publisher.packageName}}}.first{it.packageName==publisher.packageName}.count)
            assertEquals(1,DigestRepository(target).counts.first().first{it.packageName==publisher.packageName}.reasons["Notification digest rule"])
            digest.clearPackage(publisher.packageName)
            assertTrue(digest.counts.first().none{it.packageName==publisher.packageName})
            val spamRevision=NotificationDiagnostics.status.value.ruleRevision
            repository.update{it.copy(settings=it.settings.copy(ui=it.settings.ui.copy(experience=it.settings.ui.experience.copy(digestEnabled=false,spamMode=SpamMode.STRONG))),
                policies=mapOf(publisher.packageName to AppPolicy()))}
            waitFor("spam default applied"){NotificationDiagnostics.status.value.ruleRevision>spamRevision}
            post(6110,"sale_discount_today")
            post(6111,"OTP_sale_discount")
            post(6112,"sale_discount_navigation",Notification.CATEGORY_NAVIGATION)
            waitFor("spam filtering preserves authentication and navigation"){!present(6110)&&present(6111)&&present(6112)}
            val spamRow=withTimeout(5_000){digest.counts.first{rows->rows.any{it.packageName==publisher.packageName}}}.first{it.packageName==publisher.packageName}
            assertEquals(1,spamRow.reasons["Multiple promotional patterns"])
            val allowRevision=NotificationDiagnostics.status.value.ruleRevision
            repository.update{it.copy(policies=mapOf(publisher.packageName to AppPolicy(notificationRule=NotificationRule(spamAllowed=true))))}
            waitFor("spam app allowlist applied"){NotificationDiagnostics.status.value.ruleRevision>allowRevision}
            post(6113,"sale_discount_allowlisted")
            waitFor("allowlisted app remains visible"){present(6113)}
            val offRevision=NotificationDiagnostics.status.value.ruleRevision
            repository.update{it.copy(policies=mapOf(publisher.packageName to AppPolicy(notificationRule=NotificationRule(spamMode=SpamMode.OFF))))}
            waitFor("spam app Off override applied"){NotificationDiagnostics.status.value.ruleRevision>offRevision}
            post(6114,"sale_discount_overridden")
            waitFor("per-app Off overrides stronger default"){present(6114)}
            repository.update{it.copy(policies=mapOf(publisher.packageName to AppPolicy(notifications=NotificationMode.DISMISS)))}
            shell("cmd notification disallow_listener $component")
            waitFor("listener disconnected"){!NotificationDiagnostics.status.value.connected}
            post(6104,"after_access_revoked")
            waitFor("revoked listener leaves notifications"){present(6104)}
            shell("cmd notification allow_listener $component")
            waitFor("listener connected"){NotificationDiagnostics.status.value.connected}
            waitFor("reconnected listener filters existing notification"){!present(6104)}
            assertFalse(ConfigurationCodec.encode(repository.data.first()).contains("personal_message"))
        }finally{
            DigestRepository(target).clearPackage(publisher.packageName)
            publishCommand("--es operation clear");repository.restore(original)
            if(!wasGranted)shell("cmd notification disallow_listener $component")
        }
    }
    @Test fun digestOnlyCountsConfirmedDismissalsAndSurvivesRepositoryRecreation()=runBlocking {
        val target=InstrumentationRegistry.getInstrumentation().targetContext
        val repository=DigestRepository(target)
        val pkg="test.digest.repository"
        repository.clearPackage(pkg)
        val captureA="capture-A:${System.nanoTime()}"
        val captureB="capture-B:${System.nanoTime()}"
        try{
            assertTrue(repository.stage(pkg,captureA,"Test reason"))
            assertTrue(repository.counts.first().none{it.packageName==pkg})
            repository.removed(captureA,false)
            assertTrue(repository.counts.first().none{it.packageName==pkg})
            assertTrue(repository.stage(pkg,captureB,"Test reason"))
            repository.removed(captureB,true)
            repository.removed(captureB,true)
            val saved=DigestRepository(target).counts.first().single{it.packageName==pkg}
            assertEquals(1,saved.count)
            assertEquals(mapOf("Test reason" to 1),saved.reasons)
            assertFalse(repository.stage(pkg,captureB,"Test reason"))
            repository.acknowledge(listOf(saved))
            assertEquals(1,repository.counts.first().single{it.packageName==pkg}.announced)
        }finally{repository.clearPackage(pkg)}
    }
    @Test fun olderPublicationCannotAnnounceANewGroupCreatedAfterClearing()=runBlocking {
        val target=InstrumentationRegistry.getInstrumentation().targetContext
        val repository=DigestRepository(target)
        val pkg="test.digest.generation"
        val suffix=System.nanoTime()
        repository.clearPackage(pkg)
        try{
            assertTrue(repository.stage(pkg,"old:$suffix","Old group",System.currentTimeMillis()-1_000))
            repository.removed("old:$suffix",true)
            val snapshot=repository.counts.first().single{it.packageName==pkg}
            repository.clearPackage(pkg)
            assertTrue(repository.stage(pkg,"new:$suffix","New group"))
            repository.removed("new:$suffix",true)
            val replacement=DigestRepository(target).counts.first().single{it.packageName==pkg}
            assertNotEquals("Clearing starts a different retained group",snapshot.retainedAt,replacement.retainedAt)
            repository.acknowledge(listOf(snapshot))
            assertEquals("An old worker must leave the unseen replacement pending",0,repository.counts.first().single{it.packageName==pkg}.announced)
            repository.acknowledge(listOf(replacement))
            assertEquals(1,repository.counts.first().single{it.packageName==pkg}.announced)
        }finally{repository.clearPackage(pkg)}
    }
}
