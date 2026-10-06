package com.emmanuela.launcher

import android.os.Bundle
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.emmanuela.launcher.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherFlowTest {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private fun nodes(node:AccessibilityNodeInfo?):List<AccessibilityNodeInfo> =
        if(node==null)emptyList() else listOf(node)+(0 until node.childCount).flatMap{nodes(node.getChild(it))}
    private fun find(label:String,scroll:Boolean=false,match:((AccessibilityNodeInfo)->Boolean)?=null):AccessibilityNodeInfo {
        val deadline=SystemClock.uptimeMillis()+15_000
        var nextScroll=SystemClock.uptimeMillis()+1000
        while(SystemClock.uptimeMillis()<deadline){
            val tree=nodes(instrumentation.uiAutomation.rootInActiveWindow)
            tree.firstOrNull{it.isVisibleToUser&&(match?.invoke(it) ?: (it.text?.toString()==label||it.contentDescription?.toString()==label))}?.let{return it}
            if(scroll&&SystemClock.uptimeMillis()>=nextScroll){
                // Reach items in nested lists before advancing an enclosing settings page.
                tree.asReversed().firstOrNull{it.isScrollable&&it.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)}
                nextScroll=SystemClock.uptimeMillis()+1000
            }
            SystemClock.sleep(100)
        }
        throw AssertionError("Visible control not found: $label. Visible text: " + nodes(instrumentation.uiAutomation.rootInActiveWindow).filter{it.isVisibleToUser}.mapNotNull{it.text?.toString()}.joinToString(" | "))
    }
    private fun click(label:String,scroll:Boolean=false,match:((AccessibilityNodeInfo)->Boolean)?=null){
        val matches=match ?: { it:AccessibilityNodeInfo ->
            it.packageName?.toString()==instrumentation.targetContext.packageName &&
                it.className?.toString()!="android.widget.EditText" &&
                (it.text?.toString()==label||it.contentDescription?.toString()==label)
        }
        var node:AccessibilityNodeInfo?=find(label,scroll,match={candidate->
            var control:AccessibilityNodeInfo?=candidate
            while(control!=null&&!control.isClickable)control=control.parent
            matches(candidate)&&control?.isEnabled==true
        })
        while(node!=null&&!node.isClickable)node=node.parent
        assertTrue("Clickable control: $label",node?.performAction(AccessibilityNodeInfo.ACTION_CLICK)==true)
        instrumentation.waitForIdleSync()
    }
    private fun drag(from:android.graphics.PointF,to:android.graphics.PointF,hold:Long=650,cancel:Boolean=false,beforeRelease:()->Unit={}) {
        val down=SystemClock.uptimeMillis()
        fun event(action:Int,x:Float,y:Float){
            val pointer=android.view.MotionEvent.PointerProperties().apply{id=0;toolType=android.view.MotionEvent.TOOL_TYPE_FINGER}
            val coordinates=android.view.MotionEvent.PointerCoords().apply{this.x=x;this.y=y;pressure=1f;size=1f}
            val event=android.view.MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,1,arrayOf(pointer),arrayOf(coordinates),0,0,1f,1f,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0)
            try{assertTrue(instrumentation.uiAutomation.injectInputEvent(event,true))}finally{event.recycle()}
        }
        event(android.view.MotionEvent.ACTION_DOWN,from.x,from.y)
        SystemClock.sleep(hold)
        for(step in 1..20){val amount=step/20f;event(android.view.MotionEvent.ACTION_MOVE,from.x+(to.x-from.x)*amount,from.y+(to.y-from.y)*amount);SystemClock.sleep(16)}
        beforeRelease()
        event(if(cancel)android.view.MotionEvent.ACTION_CANCEL else android.view.MotionEvent.ACTION_UP,to.x,to.y)
    }
    private fun center(node:AccessibilityNodeInfo):android.graphics.PointF {
        val bounds=android.graphics.Rect();node.getBoundsInScreen(bounds)
        var stable=0
        val deadline=SystemClock.uptimeMillis()+5000
        while(stable<3&&SystemClock.uptimeMillis()<deadline){
            SystemClock.sleep(100);assertTrue(node.refresh())
            val next=android.graphics.Rect();node.getBoundsInScreen(next)
            stable=if(next==bounds)stable+1 else 0;bounds.set(next)
        }
        assertEquals("Control must settle before touch injection",3,stable)
        return android.graphics.PointF(bounds.exactCenterX(),bounds.exactCenterY())
    }
    @Test fun folderDragCommitsOnReleaseAndSurvivesRecreation()=runBlocking<Unit> {
        val repository=ConfigurationRepository(instrumentation.targetContext)
        val original=repository.data.first()
        try {
            repository.restore(LauncherData(settings=Preferences(ui=UiPreferences(folderLayout="List",experience=ExperiencePreferences(language="en",autoLaunch=false))),
                folders=listOf(AppFolder("a","Alpha folder","apps",emptyList()),AppFolder("b","Beta folder","apps",emptyList()),AppFolder("c","Gamma folder","apps",emptyList()),AppFolder("d","Delta folder","apps",emptyList()))))
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                click("All Apps");click("Organized Folders →")
                drag(center(find("Alpha folder")),center(find("Beta folder")),cancel=true)
                assertEquals(listOf("a","b","c","d"),repository.data.first().folders.map{it.id})
                val scroll=find("Folder grid scroll after cancellation",match={it.isScrollable})
                assertTrue(scroll.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD))
                find("Delta folder")
                assertTrue(scroll.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD))
                val from=center(find("Alpha folder"));val to=center(find("Beta folder"))
                drag(from,to){
                    assertEquals(listOf("a","b","c","d"),runBlocking{repository.data.first().folders.map{it.id}})
                }
                assertNotNull("Drag $from to $to; visible: "+nodes(instrumentation.uiAutomation.rootInActiveWindow).mapNotNull{it.text},kotlinx.coroutines.withTimeoutOrNull(15_000){repository.data.first{it.folders.map{it.id}==listOf("b","a","c","d")}})
                find("Delta folder",scroll=true)
                scenario.recreate()
                assertEquals(listOf("b","a","c","d"),ConfigurationRepository(instrumentation.targetContext).data.first().folders.map{it.id})
            }
        }finally{repository.restore(original)}
    }
    @Test fun folderSearchRequiresPasswordBeforeOpening()=runBlocking<Unit> {
        val repository=ConfigurationRepository(instrumentation.targetContext)
        val original=repository.data.first()
        val password=FolderPassword.create("test-only-password")
        try {
            repository.restore(LauncherData(settings=Preferences(ui=UiPreferences(experience=ExperiencePreferences(language="en",autoLaunch=false))),
                folders=listOf(AppFolder("vault","Vault","apps",emptyList(),passwordSalt=password.first,passwordHash=password.second,biometricUnlock=false))))
            ActivityScenario.launch(MainActivity::class.java).use {
                click("All Apps")
                val field=find("Search field",match={it.className?.toString()=="android.widget.EditText"})
                assertTrue(field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,Bundle().apply{putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,"Vault")}))
                click("▦ Vault");find("Unlock folder")
                val input=find("Password",match={it.className?.toString()=="android.widget.EditText"})
                assertTrue(input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,Bundle().apply{putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,"test-only-password")}))
                click("Unlock");find("Vault")
                assertFalse(ConfigurationCodec.encode(repository.data.first()).contains("test-only-password"))
            }
        }finally{repository.restore(original)}
    }
    @Test fun consecutiveWidgetDragsPersistOnlyCompletedMovement()=runBlocking<Unit> {
        val repository=ConfigurationRepository(instrumentation.targetContext)
        val original=repository.data.first()
        // Keep one observer for the interaction. DataStore 1.1.7 can miss a write
        // when a new cold collector starts concurrently (Android issue 431787506).
        val observed=kotlinx.coroutines.flow.MutableStateFlow<LauncherData?>(null)
        val placements=java.util.Collections.synchronizedList(mutableListOf<WidgetPlacement?>())
        var observer:kotlinx.coroutines.Job?=null
        try {
            repository.restore(LauncherData(settings=Preferences(showDate=false,showBattery=false,ui=UiPreferences(experience=ExperiencePreferences(language="en",autoLaunch=false)))))
            observer=launch(kotlinx.coroutines.Dispatchers.IO){repository.data.collect{placements.add(it.settings.ui.v2.widgetPositions["clock"]);observed.value=it}}
            kotlinx.coroutines.withTimeout(15_000){observed.filterNotNull().first()}
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                click("Launcher settings");click("Home",match={it.text?.toString()=="Home"});click("Arrange widgets",scroll=true)
                fun clock()=center(find("Clock",match={it.text?.toString()?.matches(Regex("\\d{2}:\\d{2}"))==true}))
                clock()
                assertEquals("Only the arrangement clock is exposed",1,nodes(instrumentation.uiAutomation.rootInActiveWindow).count{it.isVisibleToUser&&it.text?.toString()?.matches(Regex("\\d{2}:\\d{2}"))==true})
                assertFalse("Preview Home controls are not interactive",nodes(instrumentation.uiAutomation.rootInActiveWindow).any{it.isVisibleToUser&&it.contentDescription?.toString()=="Launcher settings"})
                var previous:WidgetPlacement?=null
                for(distance in listOf(90f,70f,-60f,50f)){
                    val from=clock()
                    val count=placements.size
                    drag(from,android.graphics.PointF(from.x,from.y+distance),hold=100){
                        assertEquals("No configuration emission while moving",count,placements.size)
                        assertEquals(previous,runBlocking{repository.data.first().settings.ui.v2.widgetPositions["clock"]})
                    }
                    val saved=kotlinx.coroutines.withTimeout(15_000){observed.filterNotNull().first{it.settings.ui.v2.widgetPositions["clock"]!=previous}}.settings.ui.v2.widgetPositions.getValue("clock")
                    assertTrue("Placement follows drag direction",previous==null || (saved.y-previous.y)*distance>0)
                    assertEquals("One completed placement",count+1,placements.size)
                    previous=saved
                }
                val finalCenter=clock()
                val count=placements.size
                drag(finalCenter,android.graphics.PointF(finalCenter.x,finalCenter.y-40),hold=100,cancel=true)
                assertEquals("Cancelled drag restores placement",finalCenter,clock())
                assertEquals("Cancelled drag does not save",count,placements.size)
                click("Done")
                click("Arrange widgets",scroll=true)
                assertEquals(finalCenter,clock())
                assertEquals(previous,repository.data.first().settings.ui.v2.widgetPositions["clock"])
                scenario.recreate()
                assertEquals(previous,ConfigurationRepository(instrumentation.targetContext).data.first().settings.ui.v2.widgetPositions["clock"])
            }
        }finally{observer?.cancel();repository.restore(original)}
    }
    @Test fun configuredSearchActionRequiresTapAndCanBeRemoved()=runBlocking<Unit> {
        val repository=ConfigurationRepository(instrumentation.targetContext)
        val original=repository.data.first()
        try {
            repository.restore(LauncherData(settings=Preferences(ui=UiPreferences(experience=ExperiencePreferences(language="en",autoLaunch=true,searchActions=setOf("settings"))))))
            ActivityScenario.launch(MainActivity::class.java).use {
                click("All Apps")
                val field=find("Search field",match={it.className?.toString()=="android.widget.EditText"})
                assertTrue(field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,Bundle().apply{putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,"Settings")}))
                find("Action: Settings")
                SystemClock.sleep(500)
                click("Action: Settings")
                click("App list",scroll=true)
                click("Search actions",scroll=true)
                find("Choose up to 16 actions to find by name in App List search. Actions always require a tap.")
                click("Settings")
                kotlinx.coroutines.withTimeout(15_000){repository.data.first{it.settings.ui.experience.searchActions.isEmpty()}}
                click("Add search action")
                click("Clock",scroll=true)
                kotlinx.coroutines.withTimeout(15_000){repository.data.first{"clock" in it.settings.ui.experience.searchActions}}
            }
        }finally{repository.restore(original)}
    }
    @Test fun homeAndDrawerSwipesRespectDirectionAndCancellation()=runBlocking<Unit> {
        val repository=ConfigurationRepository(instrumentation.targetContext)
        val original=repository.data.first()
        try {
            repository.restore(LauncherData(settings=Preferences(showClock=false,showDate=false,showBattery=false,favoriteCount=0,ui=UiPreferences(bottomShortcuts=false,experience=ExperiencePreferences(language="en",autoLaunch=false)))))
            ActivityScenario.launch(MainActivity::class.java).use {
                center(find("Launcher settings"))
                val screen=android.graphics.Rect().also{instrumentation.uiAutomation.rootInActiveWindow!!.getBoundsInScreen(it)}
                fun point(x:Float,y:Float)=android.graphics.PointF(screen.left+screen.width()*x,screen.top+screen.height()*y)
                drag(point(.5f,.65f),point(.5f,.3f),hold=0,cancel=true)
                center(find("Launcher settings"))
                assertFalse("Cancelled swipe stays Home",nodes(instrumentation.uiAutomation.rootInActiveWindow).any{it.className?.toString()=="android.widget.EditText"})
                drag(point(.5f,.65f),point(.5f,.3f),hold=0)
                center(find("Search",match={it.className?.toString()=="android.widget.EditText"}))
                drag(point(.8f,.55f),point(.2f,.55f),hold=0)
                center(find("Organized Folders"))
                drag(point(.2f,.55f),point(.8f,.55f),hold=0)
                center(find("Launcher settings"))
                assertFalse(nodes(instrumentation.uiAutomation.rootInActiveWindow).any{it.className?.toString()=="android.widget.EditText"})
            }
        }finally{repository.restore(original)}
    }
    @Test fun manualFolderAppReorderPersistsAfterRelease()=runBlocking<Unit> {
        val target=instrumentation.targetContext
        val repository=ConfigurationRepository(target)
        val original=repository.data.first()
        val catalog=target.packageManager.queryIntentActivities(android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_LAUNCHER),0)
            .filter{it.activityInfo.packageName!=target.packageName}.distinctBy{android.content.ComponentName(it.activityInfo.packageName,it.activityInfo.name).flattenToString()}
        val ids=catalog.map{android.content.ComponentName(it.activityInfo.packageName,it.activityInfo.name).flattenToString()}
        assertTrue("Scrollable installed-app fixture",ids.size>=8)
        val observed=kotlinx.coroutines.flow.MutableStateFlow<LauncherData?>(null)
        var observer:kotlinx.coroutines.Job?=null
        try {
            repository.restore(LauncherData(settings=Preferences(ui=UiPreferences(experience=ExperiencePreferences(language="en",autoLaunch=false))),folders=listOf(AppFolder("manual","Manual apps","apps",ids,layout="Text",manualOrder=true))))
            observer=launch(kotlinx.coroutines.Dispatchers.IO){repository.data.collect{observed.value=it}}
            kotlinx.coroutines.withTimeout(15_000){observed.filterNotNull().first()}
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                click("All Apps");click("Organized Folders →");click("Manual apps");click("Arrange apps")
                val from=center(find(catalog[0].loadLabel(target.packageManager).toString()))
                val to=center(find(catalog[1].loadLabel(target.packageManager).toString()))
                drag(from,to){assertEquals(ids,runBlocking{repository.data.first().folders.single().apps})}
                val reordered=listOf(ids[1],ids[0])+ids.drop(2)
                kotlinx.coroutines.withTimeout(15_000){observed.filterNotNull().first{it.folders.single().apps==reordered}}
                click("Done");scenario.recreate()
                assertEquals(reordered,repository.data.first().folders.single().apps)
            }
        }finally{observer?.cancel();repository.restore(original)}
    }
    @Test fun notificationEditorSavesScheduleKeywordsAndTemporarySuppression()=runBlocking<Unit> {
        val repository=ConfigurationRepository(instrumentation.targetContext)
        val original=repository.data.first()
        val observed=kotlinx.coroutines.flow.MutableStateFlow<LauncherData?>(null)
        var observer:kotlinx.coroutines.Job?=null
        suspend fun saved(predicate:(AppPolicy)->Boolean)=kotlinx.coroutines.withTimeout(15_000){observed.filterNotNull().first{it.policies["com.android.settings"]?.let(predicate)==true}}
        try {
            repository.restore(LauncherData(settings=Preferences(ui=UiPreferences(experience=ExperiencePreferences(language="en",autoLaunch=false)))))
            observer=launch(kotlinx.coroutines.Dispatchers.IO){repository.data.collect{observed.value=it}}
            kotlinx.coroutines.withTimeout(15_000){observed.filterNotNull().first()}
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                click("Launcher settings");click("Apps",scroll=true);click("Notifications");click("Settings",scroll=true)
                click("Delivery");click("DISMISS");click("Apply rule",scroll=true);click("Schedule")
                val keywords=find("Keywords",scroll=true,match={it.className?.toString()=="android.widget.EditText"})
                assertTrue(keywords.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,Bundle().apply{putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,"urgent\nmeeting")}))
                click("Save notification rule",scroll=true)
                saved{it.notifications==NotificationMode.DISMISS&&it.notificationRule.scope==NotificationScope.SCHEDULE&&it.notificationRule.keywords==setOf("urgent","meeting")}
                click("Suppress for 30 minutes",scroll=true)
                saved{it.notificationRule.suppressUntil>System.currentTimeMillis()}
                click("End suppression",scroll=true)
                saved{it.notificationRule.suppressUntil==0L}
                scenario.recreate()
                val policy=repository.data.first().policies.getValue("com.android.settings")
                assertEquals(NotificationMode.DISMISS,policy.notifications)
                assertEquals(setOf("urgent","meeting"),policy.notificationRule.keywords)
            }
        }finally{observer?.cancel();repository.restore(original)}
    }
    @Test fun applicationSearchActionCannotBypassBlockedPolicy()=runBlocking<Unit> {
        val target=instrumentation.targetContext
        val info=target.packageManager.queryIntentActivities(android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_LAUNCHER).setPackage("com.android.settings"),0).first().activityInfo
        val id=android.content.ComponentName(info.packageName,info.name).flattenToString()
        val repository=ConfigurationRepository(target)
        val original=repository.data.first()
        try {
            repository.restore(LauncherData(settings=Preferences(ui=UiPreferences(experience=ExperiencePreferences(language="en",autoLaunch=true,searchActions=setOf("app:$id")))),policies=mapOf("com.android.settings" to AppPolicy(blocked=true))))
            ActivityScenario.launch(MainActivity::class.java).use {
                click("All Apps")
                val field=find("Search field",match={it.className?.toString()=="android.widget.EditText"})
                assertTrue(field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,Bundle().apply{putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,"Settings")}))
                click("Action: Settings")
                find("App unavailable in EmmanueLA")
                assertEquals(target.packageName,instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString())
            }
        }finally{repository.restore(original)}
    }
    @Test fun typedIntentionGatesLaunchAndIsNotPersisted()=runBlocking {
        val repository=ConfigurationRepository(instrumentation.targetContext)
        val original=repository.data.first()
        try {
            repository.restore(LauncherData(settings=Preferences(ui=UiPreferences(experience=ExperiencePreferences(language="en",autoLaunch=false))),
                focusGroups=listOf(FocusGroup(id="intent",name="Intent",packages=setOf("com.android.settings"),dailyMinutes=0,pause=true,pauseSeconds=0,requireIntention=true))))
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                click("All Apps")
                val search=find("Search field",match={it.className?.toString()=="android.widget.EditText"})
                assertTrue(search.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,Bundle().apply{putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,"Settings")}))
                click("Settings app",match={it.contentDescription?.toString()?.contains(", com.android.settings")==true})
                find("Your intention")
                scenario.onActivity { activity ->
                    val model=androidx.lifecycle.ViewModelProvider(activity)[LauncherViewModel::class.java]
                    model.confirmPause("")
                    assertNotNull(model.pendingPause.value)
                }
                val intention=find("Intention field",match={it.className?.toString()=="android.widget.EditText"})
                assertTrue(intention.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,Bundle().apply{putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,"Check device settings")}))
                click("Open app")
                find("Android settings window",match={it.packageName?.toString()=="com.android.settings"})
                assertFalse(ConfigurationCodec.encode(repository.data.first()).contains("Check device settings"))
            }
        }finally{repository.restore(original)}
    }
    @Test fun appListWebIntentAndControlledBreakWorkflowRender()=runBlocking {
        val repository=ConfigurationRepository(instrumentation.targetContext)
        val original=repository.data.first()
        try {
            repository.restore(LauncherData(settings=Preferences(ui=UiPreferences(experience=ExperiencePreferences(language="en",autoLaunch=false))),
                focusGroups=listOf(FocusGroup(id="flow",name="Test Focus",packages=setOf("com.android.camera2"),dailyMinutes=0,pause=false,blockAlways=true))))
            ActivityScenario.launch(MainActivity::class.java).use {
                click("All Apps")
                find("All Apps")
                val field=find("Search field",match={it.className?.toString()=="android.widget.EditText"})
                assertTrue(field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,Bundle().apply{putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,"/example query")}))
                find("Search the web: example query")
                click("Home")
                click("Launcher settings")
                click("Live the Moment",scroll=true)
                click("Test Focus",scroll=true)
                click("Take a Break",scroll=true)
                click("Start 10-minute Break")
                kotlinx.coroutines.withTimeout(15_000){repository.data.first{it.focusGroups.single().breakUntil>System.currentTimeMillis()}}
                find("End Break",scroll=true)
                assertTrue(repository.data.first().focusGroups.single().breakUntil>System.currentTimeMillis())
                click("End Break")
                find("Take a Break")
                assertEquals(0L,repository.data.first().focusGroups.single().breakUntil)
            }
        }finally{repository.restore(original)}
    }
}
