package com.emmanuela.launcher

import android.content.ComponentName
import android.content.Intent
import android.graphics.PointF
import android.graphics.Rect
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.emmanuela.launcher.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeShortcutFlowTest {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private val context get()=instrumentation.targetContext
    private fun nodes(node:AccessibilityNodeInfo?):List<AccessibilityNodeInfo> = if(node==null)emptyList()else listOf(node)+(0 until node.childCount).flatMap{nodes(node.getChild(it))}
    private fun find(label:String):AccessibilityNodeInfo {
        val deadline=SystemClock.uptimeMillis()+5000
        while(SystemClock.uptimeMillis()<deadline){
            nodes(instrumentation.uiAutomation.rootInActiveWindow).firstOrNull{it.isVisibleToUser&&(it.contentDescription?.toString()==label||it.text?.toString()==label)}?.let{return it}
            SystemClock.sleep(50)
        }
        throw AssertionError("Home shortcut control not found: $label")
    }
    private fun click(label:String){var node=find(label);while(!node.isClickable&&node.parent!=null)node=node.parent;assertTrue(node.performAction(AccessibilityNodeInfo.ACTION_CLICK));instrumentation.waitForIdleSync()}
    private fun center(label:String):PointF {val bounds=Rect();find(label).getBoundsInScreen(bounds);return PointF(bounds.exactCenterX(),bounds.exactCenterY())}
    private fun gesture(from:PointF,to:PointF,cancel:Boolean=false,moving:()->Unit={}) {
        val down=SystemClock.uptimeMillis()
        fun inject(action:Int,point:PointF){
            val properties=MotionEvent.PointerProperties().apply{id=0;toolType=MotionEvent.TOOL_TYPE_FINGER}
            val coordinates=MotionEvent.PointerCoords().apply{x=point.x;y=point.y;pressure=1f;size=1f}
            val event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,1,arrayOf(properties),arrayOf(coordinates),0,0,1f,1f,0,0,InputDevice.SOURCE_TOUCHSCREEN,0)
            try{assertTrue(instrumentation.uiAutomation.injectInputEvent(event,true))}finally{event.recycle()}
        }
        inject(MotionEvent.ACTION_DOWN,from);SystemClock.sleep(ViewConfiguration.getLongPressTimeout()+100L)
        for(step in 1..15){val n=step/15f;inject(MotionEvent.ACTION_MOVE,PointF(from.x+(to.x-from.x)*n,from.y+(to.y-from.y)*n));SystemClock.sleep(16)}
        moving();inject(if(cancel)MotionEvent.ACTION_CANCEL else MotionEvent.ACTION_UP,to);instrumentation.waitForIdleSync()
    }
    private fun installedSettings():LaunchableApp {
        val info=context.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage("com.android.settings"),0).first().activityInfo
        return LaunchableApp(ComponentName(info.packageName,info.name).flattenToString(),info.loadLabel(context.packageManager).toString(),info.packageName)
    }
    private fun fixture()=LauncherData(settings=Preferences(showClock=false,showDate=false,showBattery=false,favoriteCount=0,
        ui=UiPreferences(bottomShortcuts=false,v2=V2Preferences(gesturesEnabled=false),experience=ExperiencePreferences(language="en",autoLaunch=false))))

    @Test fun chooseMoveCancelBlockedLaunchAndRemoveUseExistingHome()=runBlocking<Unit> {
        val repository=ConfigurationRepository(context);val original=repository.data.first();val app=installedSettings()
        var observer:Job?=null
        try {
            repository.restore(fixture().copy(policies=mapOf(app.packageName to AppPolicy(blocked=true))))
            val observed=MutableStateFlow<LauncherData?>(null)
            observer=launch(Dispatchers.IO){repository.data.collect{observed.value=it}}
            withTimeout(5000){observed.filterNotNull().first()}
            ActivityScenario.launch(MainActivity::class.java).use{scenario->
                click("Add app to Home");click("Add ${app.label} to Home")
                withTimeout(5000){observed.filterNotNull().first{it.settings.ui.v2.homeShortcuts.size==1}}
                val label="${app.label}, Home shortcut";val from=center(label)
                val before=observed.value!!.settings.ui.v2.homeShortcuts.single().position
                gesture(from,PointF(from.x+40f,from.y+80f)){
                    assertEquals(before,observed.value!!.settings.ui.v2.homeShortcuts.single().position)
                }
                val moved=withTimeout(5000){observed.filterNotNull().first{it.settings.ui.v2.homeShortcuts.single().position!=before}}.settings.ui.v2.homeShortcuts.single().position
                val next=center(label);gesture(next,PointF(next.x+30f,next.y-50f),cancel=true)
                assertEquals(next.x,center(label).x,1f);assertEquals(next.y,center(label).y,1f)
                assertEquals(moved,repository.data.first().settings.ui.v2.homeShortcuts.single().position)
                scenario.recreate();find(label)
                click(label);find("App unavailable in EmmanueLA");assertEquals(context.packageName,instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString());click("Close")
                val position=center(label);gesture(position,position);click("Remove from Home")
                withTimeout(5000){observed.filterNotNull().first{it.settings.ui.v2.homeShortcuts.isEmpty()}}
                assertNotNull("Removing a shortcut never uninstalls its application",context.packageManager.getActivityInfo(ComponentName.unflattenFromString(app.id)!!,0))
                scenario.recreate();assertTrue(repository.data.first().settings.ui.v2.homeShortcuts.isEmpty())
            }
        }finally{observer?.cancelAndJoin();repository.restore(original)}
    }
    @Test fun homeIconTapLaunchesInstalledActivity()=runBlocking<Unit> {
        val repository=ConfigurationRepository(context);val original=repository.data.first();val app=installedSettings()
        try {
            val state=fixture();repository.restore(state.copy(settings=state.settings.copy(ui=state.settings.ui.copy(v2=state.settings.ui.v2.copy(homeShortcuts=listOf(HomeShortcut(app.id,WidgetPlacement(20f,280f))))))))
            ActivityScenario.launch(MainActivity::class.java).use {
                click("${app.label}, Home shortcut")
                val deadline=SystemClock.uptimeMillis()+5000
                while(instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString()!=app.packageName&&SystemClock.uptimeMillis()<deadline)SystemClock.sleep(50)
                assertEquals(app.packageName,instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString())
            }
        }finally{repository.restore(original)}
    }
    @Test fun hiddenAppShortcutAndPickerEntryStayHidden()=runBlocking<Unit> {
        val repository=ConfigurationRepository(context);val original=repository.data.first();val app=installedSettings()
        try {
            val state=fixture();repository.restore(state.copy(policies=mapOf(app.packageName to AppPolicy(hidden=true)),settings=state.settings.copy(ui=state.settings.ui.copy(v2=state.settings.ui.v2.copy(homeShortcuts=listOf(HomeShortcut(app.id,WidgetPlacement(20f,280f))))))))
            ActivityScenario.launch(MainActivity::class.java).use {
                find("Launcher settings")
                assertFalse(nodes(instrumentation.uiAutomation.rootInActiveWindow).any{it.isVisibleToUser&&it.contentDescription?.toString()=="${app.label}, Home shortcut"})
                click("Add app to Home");find("Find installed app")
                assertFalse(nodes(instrumentation.uiAutomation.rootInActiveWindow).any{it.isVisibleToUser&&it.contentDescription?.toString()=="Add ${app.label} to Home"})
                assertEquals("Hiding retains a reversible saved position",1,repository.data.first().settings.ui.v2.homeShortcuts.size)
            }
        }finally{repository.restore(original)}
    }
}
