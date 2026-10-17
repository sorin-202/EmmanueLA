package com.emmanuela.launcher

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.SystemClock
import android.graphics.Rect
import android.view.InputDevice
import android.view.MotionEvent
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
import java.util.concurrent.CopyOnWriteArrayList

/** Exercises the options route, Android document-result callback, preview and real DataStore. */
@RunWith(AndroidJUnit4::class)
class WidgetStyleFlowTest {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private val clicks=mutableListOf<String>()
    private var safeViewport:Rect?=null
    private fun nodes(node:AccessibilityNodeInfo?):List<AccessibilityNodeInfo> =
        if(node==null)emptyList()else listOf(node)+(0 until node.childCount).flatMap{nodes(node.getChild(it))}
    private fun find(text:String,scroll:Boolean=false):AccessibilityNodeInfo {
        val deadline=SystemClock.uptimeMillis()+8000
        var nextScroll=SystemClock.uptimeMillis()+400
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
        throw AssertionError("Missing visible control: $text. Visible text: "+nodes(instrumentation.uiAutomation.rootInActiveWindow).filter{it.isVisibleToUser}.mapNotNull{it.text?.toString()}.joinToString(" | "))
    }
    private fun click(text:String,scroll:Boolean=false){
        val deadline=SystemClock.uptimeMillis()+8000
        var nextScroll=SystemClock.uptimeMillis()+400
        var previous:Rect?=null
        var settled=0
        var target:Rect?=null
        var requestedOnScreen=false
        while(SystemClock.uptimeMillis()<deadline){
            val tree=nodes(instrumentation.uiAutomation.rootInActiveWindow)
            val node=tree.firstOrNull{candidate->
                // The outgoing Home root also describes itself as "Home", but its
                // tap is deliberately a no-op. Wait for the actual settings row.
                val matches=if(text=="Launcher settings")candidate.contentDescription?.toString()==text
                    else candidate.text?.toString()?.lineSequence()?.any{it==text}==true
                var control:AccessibilityNodeInfo?=candidate
                while(control!=null&&!control.isClickable)control=control.parent
                matches&&candidate.isVisibleToUser&&candidate.packageName?.toString()==instrumentation.targetContext.packageName&&control?.isEnabled==true
            }
            if(node!=null){
                var control:AccessibilityNodeInfo=node
                while(!control.isClickable){control=control.parent?:break}
                if(scroll&&!requestedOnScreen){
                    val shown=control.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.id)
                    clicks.add("$text show-on-screen=$shown")
                    requestedOnScreen=true
                    // Bringing a partially visible row into view can replace its
                    // semantics node. Resolve the current full button again.
                    previous=null;settled=0
                    SystemClock.sleep(50)
                    continue
                }
                val bounds=Rect().also{control.getBoundsInScreen(it)}
                settled=if(bounds==previous&&!bounds.isEmpty)settled+1 else 0
                previous=bounds
                if(settled==3){target=bounds;break}
            }else{
                settled=0;previous=null
                if(scroll&&SystemClock.uptimeMillis()>=nextScroll){
                    tree.asReversed().firstOrNull{it.isScrollable&&it.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)}
                    nextScroll=SystemClock.uptimeMillis()+400
                }
            }
            SystemClock.sleep(50)
        }
        val bounds=requireNotNull(target){"Clickable control must settle: $text. Visible text: "+nodes(instrumentation.uiAutomation.rootInActiveWindow).filter{it.isVisibleToUser}.mapNotNull{it.text?.toString()}.joinToString(" | ")}
        clicks.add("$text button=$bounds safeViewport=$safeViewport")
        safeViewport?.let{assertTrue("$text touch center must be inside the unobscured viewport: ${clicks.joinToString("; ")}",it.contains(bounds.centerX(),bounds.centerY()))}
        // A real touch avoids retaining a semantics ID across navigation/recomposition.
        val down=SystemClock.uptimeMillis()
        for(action in listOf(MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP)){
            val pointer=MotionEvent.PointerProperties().apply{id=0;toolType=MotionEvent.TOOL_TYPE_FINGER}
            val coordinates=MotionEvent.PointerCoords().apply{x=bounds.exactCenterX();y=bounds.exactCenterY();pressure=1f;size=1f}
            val event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,1,arrayOf(pointer),arrayOf(coordinates),0,0,1f,1f,0,0,InputDevice.SOURCE_TOUCHSCREEN,0)
            try{assertTrue("Inject touch for $text",instrumentation.uiAutomation.injectInputEvent(event,true))}finally{event.recycle()}
        }
        instrumentation.waitForIdleSync()
    }
    private fun captureViewport(scenario:ActivityScenario<MainActivity>){
        scenario.onActivity{activity->
            val decor=activity.window.decorView
            val insets=androidx.core.view.ViewCompat.getRootWindowInsets(decor)
            assertNotNull("Platform window insets are available",insets)
            val bars=insets!!.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            val origin=IntArray(2);decor.getLocationOnScreen(origin)
            safeViewport=Rect(origin[0]+bars.left,origin[1]+bars.top,origin[0]+decor.width-bars.right,origin[1]+decor.height-bars.bottom)
        }
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
        val requests=CopyOnWriteArrayList<Intent>()
        val observed=MutableStateFlow<LauncherData?>(null)
        try{
            repository.restore(LauncherData(settings=Preferences(ui=UiPreferences(experience=ExperiencePreferences(language="en",autoLaunch=false),v2=V2Preferences(widgetStyles=mapOf("date" to sibling))))))
            observer=launch(Dispatchers.IO){repository.data.collect{observed.value=it}}
            withTimeout(5000){observed.filterNotNull().first()}
            monitor=object:Instrumentation.ActivityMonitor(){
                override fun onStartActivity(intent:Intent):Instrumentation.ActivityResult?{
                    if(intent.action!=Intent.ACTION_OPEN_DOCUMENT)return null
                    requests.add(Intent(intent))
                    return Instrumentation.ActivityResult(Activity.RESULT_OK,Intent().setData(uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                }
            }.also(instrumentation::addMonitor)
            ActivityScenario.launch(MainActivity::class.java).use{scenario->
                click("Launcher settings");click("Home");click("Manage widgets",scroll=true);click("Clock");click("Style")
                find("Preview")
                captureViewport(scenario)
                fun importPreview(){
                    val before=requests.size
                    click("Import JSON style",scroll=true)
                    try{find("Import Clock style")}catch(failure:AssertionError){
                        throw AssertionError("Import preview did not appear. Picker requests before=$before after=${requests.size}; monitor hits=${monitor?.hits}; requests=${requests.map{it.toUri(0)}}; touches=${clicks.joinToString("; ")}",failure)
                    }
                    assertEquals("Each import tap invokes Android's document contract once",before+1,requests.size)
                    assertEquals("*/*",requests.last().type)
                    assertTrue("JSON is an accepted document type",requests.last().getStringArrayExtra(Intent.EXTRA_MIME_TYPES)?.contains("application/json")==true)
                }
                importPreview()
                assertFalse("Preview does not write",repository.data.first().settings.ui.v2.widgetStyles.containsKey("clock"))
                click("Cancel")
                assertFalse("Cancel does not write",repository.data.first().settings.ui.v2.widgetStyles.containsKey("clock"))
                importPreview();click("Apply style")
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
