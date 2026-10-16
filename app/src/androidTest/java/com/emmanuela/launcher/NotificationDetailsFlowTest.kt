package com.emmanuela.launcher

import android.app.Application
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.emmanuela.launcher.data.*
import com.emmanuela.launcher.platform.DigestRepository
import com.emmanuela.launcher.platform.NotificationDiagnostics
import com.emmanuela.launcher.platform.notificationAccess
import com.emmanuela.launcher.ui.apps.AppDetailsContent
import com.emmanuela.launcher.ui.components.LocalLauncherLanguage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The quick AppDetails selector must activate real digest delivery, not only save a label. */
@RunWith(AndroidJUnit4::class)
class NotificationDetailsFlowTest {
    @get:Rule val compose=createComposeRule()

    @Test fun selectingDigestEnablesAndPersistsTheMasterSettingAndFiltersAnActualNotification()=runBlocking {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val target=instrumentation.targetContext
        val publisher=instrumentation.context
        val repository=ConfigurationRepository(target)
        val original=repository.data.first()
        val wasGranted=target.notificationAccess()
        val component="${target.packageName}/${target.packageName}.EmmanuelaNotificationListener"
        val receiver="${publisher.packageName}/com.emmanuela.launcher.NotificationPublisher"
        val digest=DigestRepository(target)
        val store=ViewModelStore()
        val showDetails=mutableStateOf(true)
        var model:LauncherViewModel?=null
        fun shell(command:String)=instrumentation.uiAutomation.executeShellCommand(command).use{
            android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes().toString(Charsets.UTF_8)
        }
        fun publish(arguments:String):String=shell("am broadcast -f 32 -n $receiver $arguments").also{
            check(it.contains("data=\"|")){"Publisher did not return notification IDs: $it"}
        }
        try{
            publish("--es operation clear")
            digest.clearPackage(publisher.packageName)
            repository.restore(LauncherData(settings=Preferences(ui=UiPreferences(
                v2=V2Preferences(notificationFilter=true),
                experience=ExperiencePreferences(advanced=true,digestEnabled=false,language="en")
            ))))
            assertFalse(repository.data.first().settings.ui.experience.digestEnabled)
            assertEquals(NotificationMode.NORMAL,repository.data.first().policies[publisher.packageName]?.notifications?:NotificationMode.NORMAL)
            if(Build.VERSION.SDK_INT>=33)shell("pm grant ${publisher.packageName} android.permission.POST_NOTIFICATIONS")
            shell("cmd notification allow_listener $component")
            withTimeout(10_000){NotificationDiagnostics.status.first{it.connected}}
            instrumentation.runOnMainSync{
                model=ViewModelProvider(store,ViewModelProvider.AndroidViewModelFactory(target.applicationContext as Application))[LauncherViewModel::class.java]
            }
            val realModel=requireNotNull(model)
            val app=LaunchableApp("${publisher.packageName}/com.emmanuela.launcher.FocusFixtureActivity","Notification test publisher",publisher.packageName)
            compose.setContent{
                val data by realModel.data.collectAsState()
                MaterialTheme{
                    CompositionLocalProvider(LocalLauncherLanguage provides "en"){
                        if(showDetails.value)Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){
                            AppDetailsContent(app,data,realModel)
                        }
                    }
                }
            }
            compose.waitUntil(5_000){realModel.ready.value}
            compose.onNodeWithText("Notifications").performScrollTo().performClick()
            compose.onNodeWithText("DIGEST").performClick()
            val saved=withTimeout(5_000){repository.data.first{
                it.settings.ui.experience.digestEnabled&&it.policies[publisher.packageName]?.notifications==NotificationMode.DIGEST
            }}
            assertTrue(saved.settings.ui.experience.digestEnabled)
            assertEquals(NotificationMode.DIGEST,saved.policies.getValue(publisher.packageName).notifications)
            val persisted=ConfigurationRepository(target).data.first()
            assertTrue("The master switch must survive repository recreation",persisted.settings.ui.experience.digestEnabled)
            assertEquals(NotificationMode.DIGEST,persisted.policies.getValue(publisher.packageName).notifications)
            compose.onNodeWithText("DIGEST").assertIsDisplayed()
            publish("--es operation post --ei id 6120 --es text appdetails_digest_regression")
            val captured=withTimeout(5_000){digest.counts.first{rows->rows.any{it.packageName==publisher.packageName&&it.count==1}}}
                .single{it.packageName==publisher.packageName}
            assertEquals(mapOf("Notification digest rule" to 1),captured.reasons)
            assertFalse("Android must actually dismiss the original notification",publish("--es operation list").contains("|6120|"))
        }finally{
            if(model!=null)instrumentation.runOnMainSync{showDetails.value=false;store.clear()}
            publish("--es operation clear")
            digest.clearPackage(publisher.packageName)
            repository.restore(original)
            if(!wasGranted)shell("cmd notification disallow_listener $component")
        }
    }
}
