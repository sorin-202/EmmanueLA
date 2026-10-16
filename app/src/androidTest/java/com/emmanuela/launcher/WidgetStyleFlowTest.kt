package com.emmanuela.launcher

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.IntentFilter
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.content.FileProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.emmanuela.launcher.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Exercises the options route, Android document-result callback, preview and real DataStore. */
@RunWith(AndroidJUnit4::class)
class WidgetStyleFlowTest {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private fun nodes(node:AccessibilityNodeInfo?):List<AccessibilityNodeInfo> =
        if(node==null)emptyList()else listOf(node)+(0 until node.childCount).flatMap{nodes(node.getChild(it))}
    private fun find(text:String,scroll:Boolean=false):AccessibilityNodeInfo {
        val deadline=SystemClock.uptimeMillis()+8000
        var nextScroll=0L
        while(SystemClock.uptimeMillis()<deadline){
            val tree=nodes(instrumentation.uiAutomation.rootInActiveWindow)
            (tree.firstOrNull{it.isVisibleToUser&&it.text?.toString()?.lineSequence()?.any{line->line==text}==true}
                ?:tree.firstOrNull{it.isVisibleToUser&&it.contentDescription?.toString()==text})?.let{return it}
            if(scroll&&SystemClock.uptimeMillis()>=nextScroll){
                tree.asReversed().firstOrNull{it.isScrollable&&it.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)}
                nextScroll=SystemClock.uptimeMillis()+400
            }
            SystemClock.sleep(50)
        }
        throw AssertionError("Missing visible control: $text")
    }
    private fun click(text:String,scroll:Boolean=false){
        var node:AccessibilityNodeInfo?=find(text,scroll)
        while(node!=null&&!node.isClickable)node=node.parent
        assertTrue("Clickable: $text",node?.performAction(AccessibilityNodeInfo.ACTION_CLICK)==true)
        instrumentation.waitForIdleSync()
    }
    @Test fun customHomeTextScaleHasMatchingPreviewAndHomeHeight()=runBlocking<Unit>{
        val repository=ConfigurationRepository(instrumentation.targetContext)
        val original=repository.data.first()
        try{
            repository.restore(LauncherData(settings=Preferences(showDate=false,showBattery=false,favoriteCount=0,
                ui=UiPreferences(experience=ExperiencePreferences(language="en",autoLaunch=false,homeStyleOverride=true,homeTextScale=1.4f,homeFont="Monospace"),
                    v2=V2Preferences(clockSeconds=true,widgetStyles=mapOf("clock" to WidgetStyle(font="Monospace",scale=1.5f,width=280f)))))))
            ActivityScenario.launch(MainActivity::class.java).use{
                fun clockBounds(label:String):android.graphics.Rect {
                    val node=find(label)
                    assertTrue("$label preserves the configured seconds format",nodes(node).any{it.text?.toString()?.matches(Regex("\\d{2}:\\d{2}:\\d{2}"))==true})
                    return android.graphics.Rect().also{node.getBoundsInScreen(it)}
                }
                val home=clockBounds("Clock")
                val density=instrumentation.targetContext.resources.displayMetrics.density
                // This narrow, enlarged HH:mm:ss clock wraps over three lines. Its
                // measured height must leave room for the enlarged text on every line.
                assertTrue("Enlarged multiline clock must preserve readable line spacing",home.height()/density>=350f)
                click("Launcher settings");click("Home");click("Manage widgets",scroll=true);click("Clock");click("Style")
                val preview=clockBounds("Clock style preview")
                assertEquals("Preview uses the real Home text size and line height",home.height().toFloat(),preview.height().toFloat(),1f)
                assertEquals(home.width().toFloat(),preview.width().toFloat(),1f)
            }
        }finally{repository.restore(original)}
    }
    @Test fun importIsPreviewedBeforeApplyAndPersistsOnlySelectedWidget()=runBlocking<Unit>{
        val context=instrumentation.targetContext
        val repository=ConfigurationRepository(context)
        val original=repository.data.first()
        val sibling=WidgetStyle(font="Monospace",color=0xFFA6C9A8L)
        val file=File(context.cacheDir,"shared/widget-test.json").apply{parentFile?.mkdirs();writeText("""{"schema":"emmanuela.widget-style","version":1,"widget":"clock","style":{"font":"Serif","weight":"Bold","width":280,"alignment":"Center","borderColor":"#A6C9A8","borderWidth":2}}""")}
        val uri=FileProvider.getUriForFile(context,"${context.packageName}.files",file)
        var monitor:Instrumentation.ActivityMonitor?=null
        var observer:Job?=null
        val observed=MutableStateFlow<LauncherData?>(null)
        try{
            repository.restore(LauncherData(settings=Preferences(ui=UiPreferences(experience=ExperiencePreferences(language="en",autoLaunch=false),v2=V2Preferences(widgetStyles=mapOf("date" to sibling))))))
            observer=launch(Dispatchers.IO){repository.data.collect{observed.value=it}}
            withTimeout(5000){observed.filterNotNull().first()}
            val filter=IntentFilter(Intent.ACTION_OPEN_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);addDataType("*/*")}
            monitor=instrumentation.addMonitor(filter,Instrumentation.ActivityResult(Activity.RESULT_OK,Intent().setData(uri)),true)
            ActivityScenario.launch(MainActivity::class.java).use{scenario->
                click("Launcher settings");click("Home");click("Manage widgets",scroll=true);click("Clock");click("Style")
                find("Preview")
                click("Import JSON style",scroll=true);find("Import Clock style")
                assertFalse("Preview does not write",repository.data.first().settings.ui.v2.widgetStyles.containsKey("clock"))
                click("Cancel")
                assertFalse("Cancel does not write",repository.data.first().settings.ui.v2.widgetStyles.containsKey("clock"))
                click("Import JSON style",scroll=true);find("Import Clock style");click("Apply style")
                val saved=withTimeout(5000){observed.filterNotNull().first{it.settings.ui.v2.widgetStyles.containsKey("clock")}}.settings
                assertEquals("Serif",saved.styleForWidget("clock").font)
                assertEquals("Center",saved.styleForWidget("clock").alignment)
                assertEquals(2f,saved.styleForWidget("clock").borderWidth,0f)
                assertEquals(sibling,saved.styleForWidget("date"))
                assertTrue("Android picker contract was invoked",requireNotNull(monitor).hits>=2)
                scenario.recreate()
                assertEquals(saved.ui.v2.widgetStyles,ConfigurationRepository(context).data.first().settings.ui.v2.widgetStyles)
            }
        }finally{observer?.cancel();monitor?.let(instrumentation::removeMonitor);file.delete();repository.restore(original)}
    }
}
