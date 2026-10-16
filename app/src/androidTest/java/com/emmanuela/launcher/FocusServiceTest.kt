package com.emmanuela.launcher

import android.app.UiAutomation
import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.emmanuela.launcher.data.*
import com.emmanuela.launcher.platform.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZonedDateTime

@RunWith(AndroidJUnit4::class)
class FocusServiceTest {
    @Test fun detectsAlreadyOpenAppRebindsAndAppliesLiveRules()=runBlocking {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val target=instrumentation.targetContext
        val automation=instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        val resolver=target.contentResolver
        val originalServices=Settings.Secure.getString(resolver,Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val originalEnabled=Settings.Secure.getInt(resolver,Settings.Secure.ACCESSIBILITY_ENABLED,0)
        val component=ComponentName(target,RuleEnforcementService::class.java).flattenToString()
        val fixturePackage=instrumentation.context.packageName
        val repository=ConfigurationRepository(target)
        val original=repository.data.first()
        val appOps=target.getSystemService(android.app.AppOpsManager::class.java)
        @Suppress("DEPRECATION")
        val originalUsageMode=appOps.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,android.os.Process.myUid(),target.packageName)
        fun usageAccess(mode:String){
            automation.executeShellCommand("cmd appops set ${target.packageName} GET_USAGE_STATS $mode").use {
                android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes()
            }
        }
        val enabledServices=(originalServices.orEmpty().split(':').filter { it.isNotBlank() }+component).distinct().joinToString(":")
        val withoutService=enabledServices.split(':').filterNot { it==component }.joinToString(":")
        suspend fun waitFor(stage:String,predicate:()->Boolean){
            assertTrue("$stage: ${EnforcementBridge.status.value}",withTimeoutOrNull(15_000){while(!predicate())delay(100);true}==true)
        }
        fun service(enabled:Boolean){
            automation.adoptShellPermissionIdentity("android.permission.WRITE_SECURE_SETTINGS")
            try {
                Settings.Secure.putString(resolver,Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,if(enabled)enabledServices else withoutService)
                Settings.Secure.putInt(resolver,Settings.Secure.ACCESSIBILITY_ENABLED,if(enabled||withoutService.isNotEmpty())1 else 0)
            }finally { automation.dropShellPermissionIdentity() }
        }
        fun launchFixture(){
            target.startActivity(Intent().setComponent(ComponentName(fixturePackage,FocusFixtureActivity::class.java.name))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
        }
        var group=FocusGroup(id="accessibility-fixture",name="Fixture",packages=setOf(fixturePackage),dailyMinutes=0,pause=false,blockAlways=true)
        suspend fun save(){repository.restore(LauncherData(settings=Preferences(ui=UiPreferences(v2=V2Preferences(backgroundRules=true))),focusGroups=listOf(group)))}
        try {
            service(false)
            waitFor("service unbound"){!EnforcementBridge.connected}
            save();launchFixture()
            waitFor("fixture already open"){automation.rootInActiveWindow?.packageName?.toString()==fixturePackage}
            service(true)
            waitFor("already-open app blocked after permission enable"){
                EnforcementBridge.status.value.let { it.connected&&it.foregroundPackage==fixturePackage&&it.outcome==EnforcementOutcome.OVERLAY_SHOWN&&it.reason?.contains("strict block")==true }
            }
            assertTrue(BackgroundRuleAccess.enabled(target))

            val now=System.currentTimeMillis()
            group=group.copy(breakStartedAt=now,breakUntil=now+60_000);save()
            waitFor("live Break removes overlay"){EnforcementBridge.status.value.outcome==EnforcementOutcome.ALLOWED&&automation.rootInActiveWindow?.packageName?.toString()==fixturePackage}

            // Read a real address control through Android's accessibility tree. The adapter's
            // Firefox contentDescription handling is verified separately from browser support.
            val info=automation.serviceInfo
            info.flags=info.flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            automation.serviceInfo=info
            var fields=emptyList<BrowserAdapters.Field>()
            waitFor("address fixture exposed"){
                fields=BrowserAdapters.readFields(automation.rootInActiveWindow,fixturePackage,listOf("fixture_address"));fields.isNotEmpty()
            }
            assertEquals("https://example.com/path",BrowserAdapters.inspect("org.mozilla.firefox","org.mozilla.firefox",fields).address)

            // A newly-active strict window must win even while the same app remains open.
            val clock=ZonedDateTime.now()
            group=group.copy(windows=listOf(FocusWindow(setOf(clock.dayOfWeek.value),0,0,"Strict block")));save()
            waitFor("scheduled strict overrides Break"){EnforcementBridge.status.value.outcome==EnforcementOutcome.OVERLAY_SHOWN}
            group=group.copy(blockAlways=false,breakStartedAt=0,breakUntil=0,windows=emptyList());save()
            waitFor("schedule removal uncovers current app"){EnforcementBridge.status.value.outcome==EnforcementOutcome.ALLOWED}

            // Usage permission changes while the app stays open must reevaluate immediately.
            usageAccess("ignore")
            group=group.copy(dailyMinutes=1440);save()
            waitFor("revoked Usage Access fails a daily limit visibly"){
                EnforcementBridge.status.value.let { it.outcome==EnforcementOutcome.OVERLAY_SHOWN&&it.reason?.contains("Usage Access")==true }
            }
            usageAccess("allow")
            waitFor("Usage Access restoration removes permission block"){
                EnforcementBridge.status.value.outcome==EnforcementOutcome.ALLOWED
            }

            // Rebinding reconstructs foreground and persistent rules without a launcher launch.
            group=group.copy(blockAlways=true);save()
            waitFor("strict restored"){EnforcementBridge.status.value.outcome==EnforcementOutcome.OVERLAY_SHOWN}
            service(false)
            waitFor("permission revocation clears connection"){!EnforcementBridge.connected}
            launchFixture();waitFor("fixture visible after revoke"){automation.rootInActiveWindow?.packageName?.toString()==fixturePackage}
            service(true)
            waitFor("rebound service applies persisted rule"){EnforcementBridge.status.value.outcome==EnforcementOutcome.OVERLAY_SHOWN}

            // Open directly from a foreign task after returning Home; no EnforcementBridge grant.
            automation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME)
            waitFor("home callback clears foreground"){EnforcementBridge.status.value.foregroundPackage!=fixturePackage}
            launchFixture()
            waitFor("external launch callback blocks"){EnforcementBridge.status.value.let { it.foregroundPackage==fixturePackage&&it.outcome==EnforcementOutcome.OVERLAY_SHOWN }}
        }finally {
            service(false)
            repository.restore(original)
            usageAccess(when(originalUsageMode){
                android.app.AppOpsManager.MODE_ALLOWED->"allow"
                android.app.AppOpsManager.MODE_IGNORED->"ignore"
                android.app.AppOpsManager.MODE_ERRORED->"deny"
                else->"default"
            })
            automation.adoptShellPermissionIdentity("android.permission.WRITE_SECURE_SETTINGS")
            try {
                Settings.Secure.putString(resolver,Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,originalServices)
                Settings.Secure.putInt(resolver,Settings.Secure.ACCESSIBILITY_ENABLED,originalEnabled)
            }finally { automation.dropShellPermissionIdentity() }
            target.startActivity(Intent(target,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
