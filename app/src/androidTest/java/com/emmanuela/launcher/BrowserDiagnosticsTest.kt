package com.emmanuela.launcher

import android.app.UiAutomation
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.emmanuela.launcher.data.*
import com.emmanuela.launcher.platform.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** External fixture: a supported browser installed with first-run onboarding completed. */
@RunWith(AndroidJUnit4::class)
class BrowserDiagnosticsTest {
    @Test fun supportedBrowserNavigationActuallyDisplaysBlockingOverlay()=runBlocking {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val target=instrumentation.targetContext
        val automation=instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        val browser=BrowserAdapters.ids.keys.firstOrNull { pkg ->
            target.packageManager.getLaunchIntentForPackage(pkg)!=null
        }
        assumeTrue("Install Chrome, Firefox, Brave, Edge or Samsung Internet and complete onboarding",browser!=null)
        browser!!
        val repository=ConfigurationRepository(target)
        val original=repository.data.first()
        val resolver=target.contentResolver
        val originalServices=Settings.Secure.getString(resolver,Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val originalEnabled=Settings.Secure.getInt(resolver,Settings.Secure.ACCESSIBILITY_ENABLED,0)
        val component=ComponentName(target,RuleEnforcementService::class.java).flattenToString()
        suspend fun waitFor(stage:String,predicate:()->Boolean){
            assertTrue("$stage: ${EnforcementBridge.status.value}",withTimeoutOrNull(20_000){while(!predicate())delay(100);true}==true)
        }
        try {
            repository.restore(LauncherData(settings=Preferences(ui=UiPreferences(v2=V2Preferences(backgroundRules=true))),
                focusGroups=listOf(FocusGroup(id="browser-fixture",name="Browser fixture",browsers=setOf(browser),websites=setOf("example.com"),blockAlways=true,dailyMinutes=0,pause=false))))
            automation.adoptShellPermissionIdentity("android.permission.WRITE_SECURE_SETTINGS")
            try {
                Settings.Secure.putString(resolver,Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,(originalServices.orEmpty().split(':').filter{it.isNotBlank()}+component).distinct().joinToString(":"))
                Settings.Secure.putInt(resolver,Settings.Secure.ACCESSIBILITY_ENABLED,1)
            }finally { automation.dropShellPermissionIdentity() }
            waitFor("accessibility connected"){EnforcementBridge.connected}
            // The address bar is available even if this public example page cannot load.
            target.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://example.com/focus-fixture")).setPackage(browser).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            waitFor("supported navigated address readable"){
                EnforcementBridge.status.value.let { it.browserPackage==browser&&it.addressReadable==true }
            }
            waitFor("real website-rule overlay attached"){
                EnforcementBridge.status.value.let { it.foregroundPackage==browser&&it.outcome==EnforcementOutcome.OVERLAY_SHOWN&&it.reason=="Browser fixture: strict block" }
            }
            fun containsReason(node:android.view.accessibility.AccessibilityNodeInfo?):Boolean =
                node!=null&&(node.text?.toString()=="Browser fixture: strict block"||(0 until node.childCount).any { containsReason(node.getChild(it)) })
            waitFor("blocking overlay is observable in Android UI"){containsReason(automation.rootInActiveWindow)}
            val diagnostic=EnforcementBridge.status.value
            target.startActivity(Intent(target,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            waitFor("launcher returned"){automation.rootInActiveWindow?.packageName?.toString()==target.packageName&&EnforcementBridge.status.value.foregroundPackage!=browser}
            assertEquals(diagnostic.browserPackage,EnforcementBridge.status.value.browserPackage)
            assertEquals(diagnostic.addressReadable,EnforcementBridge.status.value.addressReadable)
            assertNull(EnforcementBridge.status.value.issue)
        }finally {
            repository.restore(original)
            automation.adoptShellPermissionIdentity("android.permission.WRITE_SECURE_SETTINGS")
            try {
                Settings.Secure.putString(resolver,Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,originalServices)
                Settings.Secure.putInt(resolver,Settings.Secure.ACCESSIBILITY_ENABLED,originalEnabled)
            }finally { automation.dropShellPermissionIdentity() }
        }
    }
}
