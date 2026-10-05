package com.emmanuela.launcher

import android.app.UiAutomation
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.emmanuela.launcher.data.*
import com.emmanuela.launcher.platform.EnforcementBridge
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Optional external fixture: official Firefox 157 with onboarding completed. */
@RunWith(AndroidJUnit4::class)
class BrowserDiagnosticsTest {
    @Test fun firefox157ReportsUnavailableAddressAndRetainsDiagnosticAtHome()=runBlocking {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val target=instrumentation.targetContext
        val automation=instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        fun shell(command:String)=automation.executeShellCommand(command).use {
            android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes().toString(Charsets.UTF_8)
        }
        assumeTrue("Install official Firefox 157 and complete onboarding",shell("dumpsys package org.mozilla.firefox").contains("versionName=157.0"))
        val repository=ConfigurationRepository(target)
        val original=repository.data.first()
        val resolver=target.contentResolver
        val originalServices=Settings.Secure.getString(resolver,Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val originalEnabled=Settings.Secure.getInt(resolver,Settings.Secure.ACCESSIBILITY_ENABLED,0)
        val component="${target.packageName}/com.emmanuela.launcher.platform.RuleEnforcementService"
        suspend fun waitFor(stage:String,predicate:()->Boolean){
            assertTrue("$stage: ${EnforcementBridge.status.value}",withTimeoutOrNull(20_000){while(!predicate())delay(100);true}==true)
        }
        try {
            repository.restore(LauncherData(settings=Preferences(ui=UiPreferences(v2=V2Preferences(backgroundRules=true))),
                focusGroups=listOf(FocusGroup(id="browser-fixture",name="Browser fixture",browsers=setOf("org.mozilla.firefox"),websites=setOf("example.com"),blockAlways=true,dailyMinutes=0,pause=false))))
            automation.adoptShellPermissionIdentity("android.permission.WRITE_SECURE_SETTINGS")
            Settings.Secure.putString(resolver,Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,(originalServices.orEmpty().split(':').filter{it.isNotBlank()}+component).distinct().joinToString(":"))
            Settings.Secure.putInt(resolver,Settings.Secure.ACCESSIBILITY_ENABLED,1)
            automation.dropShellPermissionIdentity()
            waitFor("accessibility connected"){EnforcementBridge.connected}
            target.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://example.com")).setPackage("org.mozilla.firefox").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            waitFor("actual Firefox window"){automation.rootInActiveWindow?.packageName?.toString()=="org.mozilla.firefox"}
            waitFor("unreadable address reported"){EnforcementBridge.status.value.let{it.browserPackage=="org.mozilla.firefox"&&it.addressReadable==false&&it.issue!=null}}
            // Firefox 157's display bar exposes a localized description, not a plain URL.
            // Do not misrepresent this as a successful website block.
            delay(2000)
            assertEquals("org.mozilla.firefox",automation.rootInActiveWindow?.packageName?.toString())
            val diagnostic=EnforcementBridge.status.value
            target.startActivity(Intent(target,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            waitFor("launcher returned"){automation.rootInActiveWindow?.packageName?.toString()==target.packageName}
            assertEquals(diagnostic,EnforcementBridge.status.value)
        } finally {
            repository.restore(original)
            automation.adoptShellPermissionIdentity("android.permission.WRITE_SECURE_SETTINGS")
            Settings.Secure.putString(resolver,Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,originalServices)
            Settings.Secure.putInt(resolver,Settings.Secure.ACCESSIBILITY_ENABLED,originalEnabled)
            automation.dropShellPermissionIdentity()
        }
    }
}
