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
    @Test fun supportedBrowserNavigationActuallyDisplaysBlockingOverlay()=withBrowserFixture(initialGroup={browser->
        FocusGroup(id="browser-fixture",name="Browser fixture",browsers=setOf(browser),websites=setOf("example.com"),blockAlways=true,dailyMinutes=0,pause=false)
    }) {
        // The address bar is available even if this public example page cannot load.
        launch("https://example.com/focus-fixture")
        waitForAddress()
        waitForBlock("Browser fixture: strict block")
        val diagnostic=EnforcementBridge.status.value
        target.startActivity(Intent(target,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        waitFor("launcher returned"){automation.rootInActiveWindow?.packageName?.toString()==target.packageName&&EnforcementBridge.status.value.foregroundPackage!=browser}
        assertEquals(diagnostic.browserPackage,EnforcementBridge.status.value.browserPackage)
        assertEquals(diagnostic.addressReadable,EnforcementBridge.status.value.addressReadable)
        assertNull(EnforcementBridge.status.value.issue)
    }

    @Test fun firefoxUrlKeywordsEnforceNavigationLiveChangesAndAlreadyOpenRebind()=withBrowserFixture(requiredBrowser="org.mozilla.firefox",initialGroup={browser->
        FocusGroup(id="keyword-fixture",name="URL keyword fixture",browsers=setOf(browser),keywords=setOf("shorts","celestial navigation"),blockAlways=true,dailyMinutes=0,pause=false)
    }) {
        var group=FocusGroup(id="keyword-fixture",name="URL keyword fixture",browsers=setOf(browser),keywords=setOf("shorts","celestial navigation"),blockAlways=true,dailyMinutes=0,pause=false)
        val reason="URL keyword fixture: strict block"
        launch("https://example.com/allowed")
        waitForAddress("/allowed")
        waitForAllowed("nonmatching URL remains usable")

        // No domain rule exists: only the URL path can activate this block.
        launch("https://example.com/shorts/keyword-fixture")
        waitForAddress("/shorts/keyword-fixture")
        waitForBlock(reason)
        group=group.copy(keywords=setOf("does-not-match-this-page"));save(group)
        waitForAllowed("removing the matching keyword uncovers the already-open browser")

        group=group.copy(keywords=setOf("celestial navigation"));save(group)
        waitForAllowed("a different keyword does not block the old path")
        launch("https://example.com/celestial%20navigation")
        waitForAddress("/celestial")
        waitForBlock(reason)
        group=group.copy(keywords=setOf("does-not-match-this-page"));save(group)
        waitForAllowed("removing the encoded-path keyword clears the overlay")

        launch("https://example.com/before-query")
        waitForAddress("/before-query")
        waitForAllowed("an unrelated path remains usable")
        group=group.copy(keywords=setOf("celestial navigation"));save(group)
        waitForAllowed("query keyword does not block an unrelated path")
        launch("https://example.com/search?q=celestial%20navigation")
        waitForAddress("search?q=")
        waitForBlock(reason)
        group=group.copy(keywords=setOf("does-not-match-this-page"));save(group)
        waitForAllowed("the query is allowed after its keyword is removed")
        group=group.copy(keywords=setOf("celestial navigation"));save(group)
        waitForBlock(reason)

        // Permission rebind must rediscover the same URL without another navigation.
        service(false)
        waitFor("service unbound and browser visible"){
            !EnforcementBridge.connected&&automation.rootInActiveWindow?.packageName?.toString()==browser
        }
        service(true)
        waitFor("accessibility reconnected"){EnforcementBridge.connected}
        waitForBlock(reason)
    }

    @Test fun pathScopedWebsiteRuleKeepsOrdinaryPagesAccessible()=withBrowserFixture(requiredBrowser="org.mozilla.firefox",initialGroup={browser->
        FocusGroup(id="path-fixture",name="Shorts path fixture",browsers=setOf(browser),websites=setOf("example.com/shorts"),blockAlways=true,dailyMinutes=0,pause=false)
    }) {
        // The same path rule used for youtube.com/shorts is exercised against an
        // address fixture that needs neither a video account nor page playback.
        launch("https://example.com/watch?v=ordinary-video")
        waitForAddress("/watch?v=ordinary-video")
        waitForAllowed("ordinary video-style URL stays accessible")
        launch("https://example.com/shorts/clip")
        waitForAddress("/shorts/clip")
        waitForBlock("Shorts path fixture: strict block")

        // Keep the old overlay attached while an external browser intent navigates
        // away. A stale blocked address must not keep an ordinary page covered.
        launch("https://example.com/watch?v=ordinary-return")
        waitForAddress("/watch?v=ordinary-return")
        waitForAllowed("ordinary URL navigation removes the previous Shorts overlay")
    }

    private interface BrowserFixture {
        val browser:String
        val target:android.content.Context
        val automation:UiAutomation
        fun launch(url:String)
        fun service(enabled:Boolean)
        suspend fun save(group:FocusGroup)
        suspend fun waitFor(stage:String,predicate:()->Boolean)
        suspend fun waitForAddress(expectedFragment:String?=null)
        suspend fun waitForBlock(reason:String)
        suspend fun waitForAllowed(stage:String)
    }

    private fun withBrowserFixture(requiredBrowser:String?=null,initialGroup:(String)->FocusGroup,test:suspend BrowserFixture.()->Unit)=runBlocking {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val target=instrumentation.targetContext
        val automation=instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        automation.serviceInfo=automation.serviceInfo.apply {
            flags=flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        val browser=BrowserAdapters.ids.keys.firstOrNull { pkg ->
            (requiredBrowser==null||pkg==requiredBrowser)&&target.packageManager.getLaunchIntentForPackage(pkg)!=null
        }
        assumeTrue("Install Chrome, Firefox, Brave, Edge or Samsung Internet and complete onboarding",browser!=null)
        browser!!
        val repository=ConfigurationRepository(target)
        val original=repository.data.first()
        val resolver=target.contentResolver
        val originalServices=Settings.Secure.getString(resolver,Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val originalEnabled=Settings.Secure.getInt(resolver,Settings.Secure.ACCESSIBILITY_ENABLED,0)
        val component=ComponentName(target,RuleEnforcementService::class.java).flattenToString()
        val enabledServices=(originalServices.orEmpty().split(':').filter{it.isNotBlank()}+component).distinct().joinToString(":")
        val withoutService=enabledServices.split(':').filterNot{it==component}.joinToString(":")
        fun setService(enabled:Boolean){
            automation.adoptShellPermissionIdentity("android.permission.WRITE_SECURE_SETTINGS")
            try {
                Settings.Secure.putString(resolver,Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,if(enabled)enabledServices else withoutService)
                Settings.Secure.putInt(resolver,Settings.Secure.ACCESSIBILITY_ENABLED,if(enabled||withoutService.isNotEmpty())1 else 0)
            }finally { automation.dropShellPermissionIdentity() }
        }
        suspend fun saveGroup(group:FocusGroup){
            repository.restore(LauncherData(settings=Preferences(ui=UiPreferences(v2=V2Preferences(backgroundRules=true))),focusGroups=listOf(group)))
        }
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
        suspend fun awaitStage(stage:String,prepareFixture:()->Unit={},predicate:()->Boolean){
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
        fun containsReason(node:AccessibilityNodeInfo?,reason:String):Boolean {
            if(node==null)return false
            if(node.text?.toString()==reason)return true
            for(index in 0 until node.childCount){
                val child=node.getChild(index)?:continue
                try{if(containsReason(child,reason))return true}finally{@Suppress("DEPRECATION") child.recycle()}
            }
            return false
        }
        fun actualBrowserAddress():String? {
            val windows=automation.windows
            try {
                val application=windows.filter{it.type==android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION}
                val selected=application.firstOrNull{it.isFocused}?:application.firstOrNull{it.isActive}?:application.maxByOrNull{it.layer}
                val root=selected?.root?:automation.rootInActiveWindow
                try{return BrowserAdapters.read(root,browser).address}
                finally{@Suppress("DEPRECATION") root?.recycle()}
            }finally{windows.forEach{@Suppress("DEPRECATION") it.recycle()}}
        }
        val fixtureBrowser=browser
        val fixtureTarget=target
        val fixtureAutomation=automation
        val fixture=object:BrowserFixture {
            override val browser get()=fixtureBrowser
            override val target get()=fixtureTarget
            override val automation get()=fixtureAutomation
            override fun launch(url:String){
                target.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url)).setPackage(browser).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            override fun service(enabled:Boolean)=setService(enabled)
            override suspend fun save(group:FocusGroup)=saveGroup(group)
            override suspend fun waitFor(stage:String,predicate:()->Boolean)=awaitStage(stage,predicate=predicate)
            override suspend fun waitForAddress(expectedFragment:String?){
                awaitStage("supported navigated address readable${expectedFragment?.let{" ($it)"}.orEmpty()}",prepareFixture=::finishDelayedFirefoxFirstRunHint){
                    val status=EnforcementBridge.status.value
                    if(status.browserPackage!=browser||status.addressReadable!=true)false
                    else {
                        val address=actualBrowserAddress()
                        address!=null&&(expectedFragment==null||address.contains(expectedFragment))
                    }
                }
            }
            override suspend fun waitForBlock(reason:String){
                awaitStage("real website-rule overlay attached ($reason)",prepareFixture=::finishDelayedFirefoxFirstRunHint){
                    EnforcementBridge.status.value.let{it.foregroundPackage==browser&&it.outcome==EnforcementOutcome.OVERLAY_SHOWN&&it.reason==reason}
                }
                awaitStage("blocking overlay is observable in Android UI ($reason)"){
                    val root=automation.rootInActiveWindow
                    try{containsReason(root,reason)}finally{@Suppress("DEPRECATION") root?.recycle()}
                }
            }
            override suspend fun waitForAllowed(stage:String){
                awaitStage(stage,prepareFixture=::finishDelayedFirefoxFirstRunHint){
                    EnforcementBridge.status.value.let{it.foregroundPackage==browser&&it.outcome==EnforcementOutcome.ALLOWED}&&
                        automation.rootInActiveWindow?.let { root ->
                            try{root.packageName?.toString()==browser}finally{@Suppress("DEPRECATION") root.recycle()}
                        }==true
                }
            }
        }
        try {
            saveGroup(initialGroup(browser));setService(true)
            awaitStage("accessibility connected"){EnforcementBridge.connected}
            fixture.test()
        }finally {
            setService(false)
            repository.restore(original)
            automation.adoptShellPermissionIdentity("android.permission.WRITE_SECURE_SETTINGS")
            try {
                Settings.Secure.putString(resolver,Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,originalServices)
                Settings.Secure.putInt(resolver,Settings.Secure.ACCESSIBILITY_ENABLED,originalEnabled)
            }finally { automation.dropShellPermissionIdentity() }
        }
    }
}
