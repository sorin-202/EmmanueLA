package com.emmanuela.launcher

import android.Manifest
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.emmanuela.launcher.data.*
import com.emmanuela.launcher.platform.DigestRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationDigestWorkerTest {
    @Test fun dueDigestPostsRealSummaryAndDeniedPermissionRetainsUnannouncedCounts()=runBlocking {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val target=instrumentation.targetContext
        val configuration=ConfigurationRepository(target)
        val original=configuration.data.first()
        val digest=DigestRepository(target)
        val pkg="test.digest.worker"
        val suffix=System.nanoTime()
        val manager=target.getSystemService(NotificationManager::class.java)
        val permissionWasGranted=Build.VERSION.SDK_INT<33||ContextCompat.checkSelfPermission(target,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED
        fun shell(command:String)=instrumentation.uiAutomation.executeShellCommand(command).use{android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes().toString(Charsets.UTF_8)}
        val originalOp=Regex("POST_NOTIFICATION: (allow|ignore|deny|default)").find(shell("cmd appops get ${target.packageName} POST_NOTIFICATION"))?.groupValues?.get(1)?:"default"
        suspend fun capture(identity:String){
            assertTrue(digest.stage(pkg,identity,"Notification digest rule",System.currentTimeMillis()-16*60_000))
            digest.removed(identity,true)
        }
        try{
            if(!permissionWasGranted)shell("pm grant ${target.packageName} android.permission.POST_NOTIFICATIONS")
            shell("cmd appops set ${target.packageName} POST_NOTIFICATION allow")
            digest.clearPackage(pkg)
            configuration.update{it.copy(settings=it.settings.copy(ui=it.settings.ui.copy(v2=it.settings.ui.v2.copy(notificationFilter=true),experience=it.settings.ui.experience.copy(digestEnabled=true))),
                policies=it.policies+(pkg to AppPolicy(notifications=NotificationMode.DIGEST,digestMinutes=15)))}
            capture("worker-first:$suffix")
            val worker=TestListenableWorkerBuilder<com.emmanuela.launcher.platform.DigestWorker>(target).build()
            assertEquals(ListenableWorker.Result.success(),worker.doWork())
            val summary=manager.activeNotifications.firstOrNull{it.id==200}
            assertNotNull("A real Android summary notification must be posted",summary)
            assertTrue(summary!!.notification.extras.getCharSequence("android.title").toString().contains("summary"))
            assertEquals(1,digest.counts.first().single{it.packageName==pkg}.announced)
            manager.cancel(200)
            if(Build.VERSION.SDK_INT>=33){
                shell("cmd appops set ${target.packageName} POST_NOTIFICATION ignore")
                capture("worker-denied:$suffix")
                val denied=TestListenableWorkerBuilder<com.emmanuela.launcher.platform.DigestWorker>(target).build()
                assertEquals(ListenableWorker.Result.success(),denied.doWork())
                val row=digest.counts.first().single{it.packageName==pkg}
                assertEquals(2,row.count)
                assertEquals("Undeliverable counts must remain pending",1,row.announced)
                assertTrue(manager.activeNotifications.none{it.id==200})
            }
        }finally{
            manager.cancel(200)
            digest.clearPackage(pkg)
            configuration.restore(original)
            shell("cmd appops set ${target.packageName} POST_NOTIFICATION $originalOp")
        }
    }
}
