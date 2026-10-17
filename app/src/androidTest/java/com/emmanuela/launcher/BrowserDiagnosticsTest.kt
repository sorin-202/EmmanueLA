package com.emmanuela.launcher

import android.app.UiAutomation
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.accessibility.AccessibilityNodeInfo
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
        val pageClasses=setOf("android.webkit.WebView","org.mozilla.geckoview.GeckoView","org.mozilla.gecko.GeckoView")
        val firefoxCoachmarkText="Shake your device to summarize this page in seconds. \u26A1\uFE0F"
        var dismissedFirefoxCoachmark=false
        // Firefox can show this first-run hint after the external preflight has finished.
        // Finish only that observed fixture prompt; production never clicks browser UI.
        fun finishDelayedFirefoxFirstRunHint(){
            if(browser!="org.mozilla.firefox"||dismissedFirefoxCoachmark)return
            val root=automation.rootInActiveWindow?:return
            val dismissControls=mutableListOf<AccessibilityNodeInfo>()
            var hintCount=0
            var remaining=256
            var truncated=false
            fun visit(node:AccessibilityNodeInfo,depth:Int){
                if(remaining--<=0||depth>20){truncated=true;return}
                if(node.className?.toString() in pageClasses||node.packageName?.toString()!=browser)return
                if(node.isVisibleToUser&&node.className?.toString()=="android.widget.TextView"&&node.text?.toString()==firefoxCoachmarkText)hintCount++
                if(node.viewIdResourceName=="cfr.dismiss"){
                    @Suppress("DEPRECATION")
                    dismissControls+=AccessibilityNodeInfo.obtain(node)
                }
                for(index in 0 until node.childCount){
                    if(remaining<=0){truncated=true;break}
                    val child=node.getChild(index)?:continue
                    try{visit(child,depth+1)}finally{@Suppress("DEPRECATION") child.recycle()}
                }
            }
            try{
                visit(root,0)
                if(truncated||hintCount!=1||dismissControls.size!=1)return
                val dismiss=dismissControls.single()
                if(dismiss.className?.toString()!="android.widget.Button"||dismiss.contentDescription?.toString()!="Dismiss"||
                    !dismiss.isVisibleToUser||!dismiss.isEnabled||!dismiss.isClickable)return
                assertEquals("Fixture dismissal must belong to official Firefox", "org.mozilla.firefox",dismiss.packageName?.toString())
                assertEquals("Fixture dismissal must use the observed native control", "cfr.dismiss",dismiss.viewIdResourceName)
                assertTrue("Firefox hint must expose Android ACTION_CLICK",dismiss.actionList.any{it.id==AccessibilityNodeInfo.ACTION_CLICK})
                dismissedFirefoxCoachmark=true
                assertTrue("Android must accept the one Firefox fixture-hint dismissal",dismiss.performAction(AccessibilityNodeInfo.ACTION_CLICK))
                android.util.Log.i("BrowserFixture","Dismissed the exact delayed Firefox first-run hint once: id=cfr.dismiss; text=$firefoxCoachmarkText")
            }finally{
                dismissControls.forEach{@Suppress("DEPRECATION") it.recycle()}
                @Suppress("DEPRECATION") root.recycle()
            }
        }
        fun failureChrome():String = runCatching {
            val report=StringBuilder()
            val addressIds=BrowserAdapters.ids.getValue(browser).toSet()
            val coachmarkLabels=setOf("Dismiss",firefoxCoachmarkText)
            var remaining=512
            fun visit(node:AccessibilityNodeInfo,depth:Int){
                if(remaining--<=0||depth>40)return
                val owner=node.packageName?.toString()
                val klass=node.className?.toString()
                if(klass in pageClasses){report.appendLine("depth=$depth SKIP_PAGE class=$klass");return}
                if(owner!=browser)return
                val id=node.viewIdResourceName.orEmpty()
                report.append("depth=$depth class=$klass id=$id visible=${node.isVisibleToUser}")
                if(id in addressIds||addressIds.any{id=="$browser:id/$it"}){
                    report.append(" addressText=${node.text?.toString()?.take(256)} addressDescription=${node.contentDescription?.toString()?.take(256)} editable=${node.isEditable} focused=${node.isFocused}")
                }else{
                    node.text?.toString()?.takeIf{it in coachmarkLabels}?.let{report.append(" coachmark=$it")}
                }
                report.appendLine()
                for(index in 0 until node.childCount){
                    if(remaining<=0)break
                    val child=node.getChild(index)?:continue
                    try{visit(child,depth+1)}finally{@Suppress("DEPRECATION") child.recycle()}
                }
            }
            fun describe(label:String,root:AccessibilityNodeInfo?){
                report.appendLine("$label owner=${root?.packageName} window=${root?.windowId}")
                if(root?.packageName?.toString()==browser){
                    report.appendLine("$label adapter=${BrowserAdapters.read(root,browser)}")
                    visit(root,0)
                }
            }
            val active=automation.rootInActiveWindow
            try{describe("active",active)}finally{@Suppress("DEPRECATION") active?.recycle()}
            val windows=automation.windows
            try{
                windows.take(8).forEach{window->
                    val root=window.root
                    try{describe("window id=${window.id} type=${window.type} active=${window.isActive} focused=${window.isFocused} layer=${window.layer}",root)}
                    finally{@Suppress("DEPRECATION") root?.recycle()}
                }
                if(windows.size>8)report.appendLine("Chrome diagnostic window budget exhausted")
            }finally{windows.forEach{@Suppress("DEPRECATION") it.recycle()}}
            if(remaining<=0)report.appendLine("Chrome diagnostic node budget exhausted")
            report.toString()
        }.getOrElse{"Chrome diagnostic query failed: ${it.javaClass.simpleName}: ${it.message}"}
        suspend fun waitFor(stage:String,prepareFixture:()->Unit={},predicate:()->Boolean){
            val passed=withTimeoutOrNull(20_000){
                while(true){
                    prepareFixture()
                    ensureActive()
                    if(predicate())break
                    delay(100)
                }
                true
            }==true
            val message=if(passed)stage else "$stage: ${EnforcementBridge.status.value}\n${failureChrome()}"
            if(!passed)android.util.Log.e("BrowserFixture",message)
            assertTrue(message,passed)
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
            waitFor("supported navigated address readable",prepareFixture=::finishDelayedFirefoxFirstRunHint){
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
