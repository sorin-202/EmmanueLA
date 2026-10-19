package com.emmanuela.launcher

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.ViewCompat
import com.emmanuela.launcher.data.*
import com.emmanuela.launcher.platform.BackgroundRuleAccess
import com.emmanuela.launcher.ui.components.LocalLauncherLanguage
import com.emmanuela.launcher.ui.mindful.FocusGroupEditor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

/** Real editor callbacks must save usable web rules through the real ViewModel/DataStore. */
@RunWith(AndroidJUnit4::class)
class FocusWebsiteEditorTest {
    @get:Rule val compose=createComposeRule()
    private val instrumentation=InstrumentationRegistry.getInstrumentation()

    private inner class Editor(
        val repository:ConfigurationRepository,
        val model:LauncherViewModel,
        val restoration:StateRestorationTester,
        val show:androidx.compose.runtime.MutableState<Boolean>,
        val route:androidx.compose.runtime.MutableState<String>,
        val epoch:androidx.compose.runtime.MutableIntState,
        val saves:AtomicInteger,
        val observed:MutableStateFlow<LauncherData?>
    ) {
        fun tab(value:String){
            compose.onNodeWithText("Blocklist").performScrollTo().performClick()
            compose.onNode(hasText(value) and hasAnyAncestor(isDialog())).performClick()
        }
        fun input(label:String,value:String){
            compose.onNode(hasSetTextAction() and hasText(label)).performScrollTo().performTextReplacement(value)
        }
        fun assertInput(label:String,value:String){
            compose.onNode(hasSetTextAction() and hasText(label)).assert(SemanticsMatcher("Exact editable text is $value"){
                it.config.getOrNull(SemanticsProperties.EditableText)?.text==value
            })
        }
        fun toggle(label:String)=compose.onNodeWithContentDescription(label).performScrollTo().performClick()
        fun save(){
            // performClick injects a finger tap. Reacquire its position after the
            // editor scroll and IME have settled rather than tapping old geometry.
            instrumentation.runOnMainSync{
                val activity=ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).single()
                WindowInsetsControllerCompat(activity.window,activity.window.decorView).hide(WindowInsetsCompat.Type.ime())
            }
            compose.onNodeWithText("Save").performScrollTo()
            var previous:androidx.compose.ui.geometry.Rect?=null
            var stable=0
            var lastSample=0L
            compose.waitUntil(5_000){
                val now=android.os.SystemClock.uptimeMillis()
                if(now-lastSample<100)false else{
                    lastSample=now
                    val bounds=compose.onNodeWithText("Save").fetchSemanticsNode().boundsInRoot
                    var imeHidden=false
                    instrumentation.runOnMainSync{
                        val activity=ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).single()
                        imeHidden=ViewCompat.getRootWindowInsets(activity.window.decorView)?.isVisible(WindowInsetsCompat.Type.ime())==false
                    }
                    stable=if(imeHidden&&!bounds.isEmpty&&bounds==previous)stable+1 else 0
                    previous=bounds
                    stable>=3
                }
            }
            compose.onNodeWithText("Save").assertIsDisplayed().performTouchInput{click()}
        }
        fun add()=compose.onNodeWithText("Add").performScrollTo().performClick()
        fun error(fragment:String){
            compose.onAllNodesWithText(fragment,substring=true).onLast().performScrollTo().assertIsDisplayed()
        }
        suspend fun saved(name:String):FocusGroup{
            val group=withTimeout(5_000){
                observed.filterNotNull().first{state->state.focusGroups.any{it.name==name}}.focusGroups.single{it.name==name}
            }
            withTimeout(5_000){model.data.first{state->state.focusGroups.any{it==group}}}
            compose.waitForIdle()
            return group
        }
        fun reopen(id:String){
            compose.runOnIdle{route.value=id;epoch.intValue++;show.value=true}
        }
    }

    private fun withEditor(test:suspend Editor.()->Unit)=runBlocking {
        val target=instrumentation.targetContext
        val repository=ConfigurationRepository(target)
        val original=repository.data.first()
        val store=ViewModelStore()
        val show=mutableStateOf(true)
        val route=mutableStateOf("new")
        val epoch=mutableIntStateOf(0)
        val saves=AtomicInteger()
        val observed=MutableStateFlow<LauncherData?>(null)
        var observer:Job?=null
        var model:LauncherViewModel?=null
        try{
            // No FocusGroup is injected: every rule below is created by production editor controls.
            repository.restore(LauncherData(settings=Preferences(ui=UiPreferences(
                experience=ExperiencePreferences(language="en")
            ))))
            // Subscribe before interactions; do not race a new cold DataStore collector
            // with a UI write (the same observation pattern as LauncherFlowTest).
            observer=launch(Dispatchers.IO){repository.data.collect{observed.value=it}}
            withTimeout(5_000){observed.filterNotNull().first()}
            instrumentation.runOnMainSync{
                model=ViewModelProvider(store,ViewModelProvider.AndroidViewModelFactory(target.applicationContext as Application))[LauncherViewModel::class.java]
            }
            val realModel=requireNotNull(model)
            compose.waitUntil(5_000){realModel.ready.value}
            val apps=listOf(
                LaunchableApp("org.mozilla.firefox/.App","Firefox fixture","org.mozilla.firefox"),
                LaunchableApp("${instrumentation.context.packageName}/com.emmanuela.launcher.FocusFixtureActivity","Focus fixture",instrumentation.context.packageName)
            )
            val restoration=StateRestorationTester(compose)
            restoration.setContent{
                val data by realModel.data.collectAsState()
                MaterialTheme{
                    CompositionLocalProvider(LocalLauncherLanguage provides "en"){
                        if(show.value)key(route.value,epoch.intValue){
                            Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState())){
                                FocusGroupEditor(route.value,data,apps,realModel){saves.incrementAndGet();show.value=false}
                            }
                        }
                    }
                }
            }
            Editor(repository,realModel,restoration,show,route,epoch,saves,observed).test()
        }finally{
            try{
                if(model!=null){
                    compose.runOnIdle{show.value=false}
                    instrumentation.runOnMainSync{store.clear()}
                }
            }finally{
                observer?.cancelAndJoin()
                repository.restore(original)
            }
        }
    }

    @Test fun pendingPathAndKeywordSaveTogetherAndReloadAfterStateRestoration()=withEditor {
        input("Group name","Scoped video")
        tab("Sites")
        input("Website or URL path","https://YouTube.com/shorts/")
        tab("Keywords")
        input("URL keyword","focus term")
        toggle("Firefox fixture")
        toggle("Block continuously")
        save()
        val group=saved("Scoped video")
        assertEquals(setOf("youtube.com/shorts"),group.websites)
        assertEquals(setOf("focus term"),group.keywords)
        assertEquals(setOf("org.mozilla.firefox"),group.browsers)
        assertTrue(group.blockAlways)
        assertFalse("Saving a rule must not silently enable background enforcement",repository.data.first().settings.ui.v2.backgroundRules)
        assertEquals(group,ConfigurationRepository(instrumentation.targetContext).data.first().focusGroups.single())
        reopen(group.id)
        tab("Sites")
        compose.onNodeWithText("youtube.com/shorts").assertExists()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("youtube.com/shorts").assertExists()
        tab("Keywords")
        compose.onNodeWithText("focus term").assertExists()
        compose.onNodeWithContentDescription("Firefox fixture").assertIsOn()
        compose.onNodeWithContentDescription("Block continuously").assertIsOn()
    }

    @Test fun siteAndKeywordTabsRetainIndependentPendingEntriesAndAddOnlyTheirOwnTarget()=withEditor {
        input("Group name","Separate targets")
        tab("Sites")
        input("Website or URL path","example.com/catalog")
        tab("Keywords")
        input("URL keyword","news+letter")
        tab("Sites")
        assertInput("Website or URL path","example.com/catalog")
        add()
        assertInput("Website or URL path","")
        tab("Keywords")
        assertInput("URL keyword","news+letter")
        add()
        assertInput("URL keyword","")
        toggle("Firefox fixture")
        save()
        val group=saved("Separate targets")
        assertEquals(setOf("example.com/catalog"),group.websites)
        assertEquals(setOf("news+letter"),group.keywords)
        assertFalse(group.blockAlways)
    }

    @Test fun mixedGroupsRequireBrowserAndInvalidPendingEditsDoNotChangeSavedRule()=withEditor {
        input("Group name","Mixed group")
        compose.onNodeWithText("Focus fixture").performScrollTo().performClick()
        tab("Sites")
        input("Website or URL path","example.com/blocked")
        save()
        error("Choose at least one supported browser")
        assertTrue(repository.data.first().focusGroups.isEmpty())
        assertEquals(0,saves.get())
        toggle("Firefox fixture")
        save()
        val baseline=saved("Mixed group")
        assertEquals(setOf(instrumentation.context.packageName),baseline.packages)
        reopen(baseline.id)
        tab("Sites")
        input("Website or URL path","https://youtube.com/shorts?tracking=yes")
        add()
        error("valid website")
        save()
        error("valid website")
        assertEquals(baseline,repository.data.first().focusGroups.single())
        assertEquals(1,saves.get())
        input("Website or URL path","youtube.com/shorts")
        tab("Keywords")
        input("URL keyword","   ")
        save()
        error("URL keyword")
        assertEquals(baseline,repository.data.first().focusGroups.single())
        assertEquals(1,saves.get())
    }

    @Test fun optionalSectionsRetainDraftsAcrossCollapseRestoreSaveAndReopen()=withEditor {
        fun section(title:String)=compose.onNodeWithText(title).performScrollTo().performClick()
        input("Group name","Intentional day")
        compose.onNodeWithText("Focus fixture").performScrollTo().performClick()
        input("Find an app","Firefox")
        compose.onNodeWithText("Focus fixture").assertDoesNotExist()
        compose.onNodeWithText("1 selected").assertExists()
        input("Find an app","")
        // Optional settings start collapsed, but remain available without the global Advanced mode.
        assertFalse(repository.data.first().settings.ui.experience.advanced)
        compose.onNodeWithContentDescription("Daily limit").assertDoesNotExist()
        compose.onNodeWithContentDescription("Enter an intention").assertDoesNotExist()
        section("Usage limits")
        toggle("Daily limit")
        toggle("Session limit")
        toggle("Opens per day")
        section("Usage limits")
        compose.onNodeWithContentDescription("Session limit").assertDoesNotExist()
        section("Usage limits")
        compose.onNodeWithContentDescription("Daily limit").assertIsOff()
        compose.onNodeWithContentDescription("Session limit").assertIsOn()
        compose.onNodeWithContentDescription("Opens per day").assertIsOn()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithContentDescription("Session limit").assertIsOn()
        section("Usage limits")
        section("Pause & intention")
        toggle("Enter an intention")
        toggle("Mindful prompt")
        section("Pause & intention")
        section("Schedules")
        toggle("Scheduled block")
        compose.onNodeWithText("+ Add time window").performScrollTo().performClick()
        compose.onNodeWithText("Window 1").assertExists()
        section("Schedules")
        compose.onNodeWithText("Window 1").assertDoesNotExist()
        save()
        val group=saved("Intentional day")
        assertEquals(setOf(instrumentation.context.packageName),group.packages)
        assertEquals(0,group.dailyMinutes)
        assertEquals(15,group.sessionMinutes)
        assertEquals(10,group.maxOpens)
        assertTrue(group.pause)
        assertTrue(group.requireIntention)
        assertFalse(group.prompt)
        assertTrue(group.schedule)
        assertEquals(listOf(FocusWindow()),group.windows)
        assertEquals(group,ConfigurationRepository(instrumentation.targetContext).data.first().focusGroups.single())
        reopen(group.id)
        section("Usage limits")
        compose.onNodeWithContentDescription("Session limit").assertIsOn()
        section("Usage limits")
        section("Pause & intention")
        compose.onNodeWithContentDescription("Enter an intention").assertIsOn()
        compose.onNodeWithContentDescription("Mindful prompt").assertIsOff()
        section("Pause & intention")
        section("Schedules")
        compose.onNodeWithContentDescription("Scheduled block").assertIsOn()
        compose.onNodeWithText("Window 1").assertExists()
    }

    @Test fun websiteEditorExposesActualPermissionAndExplicitMasterWithoutChangingRuleDefaults()=withEditor {
        val masterLabel="Enforce outside launcher"
        var stage="initial editor"
        fun imeVisible():Boolean?{
            var visible:Boolean?=null
            instrumentation.runOnMainSync{
                val activity=ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).singleOrNull()
                visible=activity?.window?.decorView?.let{ViewCompat.getRootWindowInsets(it)?.isVisible(WindowInsetsCompat.Type.ime())}
            }
            return visible
        }
        fun diagnostics():String {
            val node=runCatching{compose.onNodeWithContentDescription(masterLabel).fetchSemanticsNode()}.getOrNull()
            val name=runCatching{compose.onNode(hasSetTextAction() and hasText("Group name")).fetchSemanticsNode()
                .config.getOrNull(SemanticsProperties.EditableText)?.text}.getOrNull()
            val text=runCatching{compose.onRoot(useUnmergedTree=true).fetchSemanticsNode().let{root->
                fun labels(node:androidx.compose.ui.semantics.SemanticsNode):List<String> =
                    node.config.getOrNull(SemanticsProperties.Text).orEmpty().map{it.text}+node.children.flatMap(::labels)
                labels(root).joinToString(" | ")
            }}.getOrNull()
            return "stage=$stage; observed=${observed.value?.settings?.ui?.v2?.backgroundRules}; model=${model.data.value.settings.ui.v2.backgroundRules}; " +
                "UI=${node?.config?.getOrNull(SemanticsProperties.ToggleableState)}; bounds=${node?.boundsInRoot}; IME=${imeVisible()}; ready=${model.ready.value}; error=${model.error.value}; " +
                "name=$name; saves=${saves.get()}; groups=${observed.value?.focusGroups?.map{it.name}}; editor=$text"
        }
        fun checkpoint(value:String){
            stage=value
            android.util.Log.i("FocusEditorFixture",diagnostics())
        }
        fun touchMaster(value:String){
            checkpoint("$value: before native touch")
            instrumentation.runOnMainSync{
                val activity=ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).single()
                WindowInsetsControllerCompat(activity.window,activity.window.decorView).hide(WindowInsetsCompat.Type.ime())
            }
            compose.onNodeWithContentDescription(masterLabel).performScrollTo()
            var previous:androidx.compose.ui.geometry.Rect?=null
            var stable=0
            var lastSample=0L
            compose.waitUntil(5_000){
                val now=android.os.SystemClock.uptimeMillis()
                if(now-lastSample<100)false else{
                    lastSample=now
                    val bounds=compose.onNodeWithContentDescription(masterLabel).fetchSemanticsNode().boundsInRoot
                    val settled=imeVisible()==false&&!bounds.isEmpty
                    stable=if(settled&&bounds==previous)stable+1 else if(settled)1 else 0
                    previous=bounds
                    stable>=3
                }
            }
            checkpoint("$value: three stable geometry samples")
            // Reacquire the rendered switch; performTouchInput injects a finger tap,
            // rather than invoking its semantic callback while the IME moves it.
            compose.onNodeWithContentDescription(masterLabel).assertIsDisplayed().performTouchInput{click()}
            checkpoint("$value: after native touch")
        }
        suspend fun awaitMaster(value:String,expected:Boolean){
            checkpoint("$value: persisted state")
            withTimeout(5_000){observed.filterNotNull().first{it.settings.ui.v2.backgroundRules==expected}}
            checkpoint("$value: model state")
            withTimeout(5_000){model.data.first{it.settings.ui.v2.backgroundRules==expected}}
            checkpoint("$value: state confirmed")
        }
        try{
            input("Group name","Daily plan")
            tab("Sites")
            input("Website or URL path","example.com")
            toggle("Firefox fixture")
            val permission=BackgroundRuleAccess.enabled(instrumentation.targetContext)
            compose.onNodeWithText(if(permission)"Allowed by Android"else "Permission required").assertExists()
            compose.onAllNodesWithText("60 minutes",substring=true).onFirst().assertExists()
            compose.onNodeWithContentDescription("Enforce outside launcher").assertIsOff()
            touchMaster("enable master")
            awaitMaster("enable master",true)
            compose.onNodeWithContentDescription("Enforce outside launcher").assertIsOn()
            assertEquals("The master switch must not grant Android Accessibility permission",permission,BackgroundRuleAccess.enabled(instrumentation.targetContext))
            touchMaster("disable master")
            awaitMaster("disable master",false)
            compose.onNodeWithContentDescription("Enforce outside launcher").assertIsOff()
            checkpoint("save default daily rule")
            save()
            val group=saved("Daily plan")
            assertFalse(group.blockAlways)
            assertEquals(60,group.dailyMinutes)
            assertTrue(group.pause)
            assertFalse(repository.data.first().settings.ui.v2.backgroundRules)
            checkpoint("all permission and default-rule assertions passed")
        }catch(failure:Throwable){
            val details=diagnostics()
            android.util.Log.e("FocusEditorFixture",details,failure)
            throw AssertionError("Website editor permission/master workflow failed: $details",failure)
        }
    }
}
