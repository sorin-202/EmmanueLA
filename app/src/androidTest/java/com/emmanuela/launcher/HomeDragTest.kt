package com.emmanuela.launcher

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

/** Real MotionEvents exercise the normal Home surface, including Android ACTION_CANCEL. */
@RunWith(AndroidJUnit4::class)
class HomeDragTest {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private fun nodes(node:AccessibilityNodeInfo?):List<AccessibilityNodeInfo> =
        if(node==null)emptyList()else listOf(node)+(0 until node.childCount).flatMap{nodes(node.getChild(it))}

    private fun clockCenter():PointF {
        val deadline=SystemClock.uptimeMillis()+5000
        while(SystemClock.uptimeMillis()<deadline){
            val node=nodes(instrumentation.uiAutomation.rootInActiveWindow).firstOrNull{it.isVisibleToUser&&it.contentDescription?.toString()=="Clock"}
            if(node!=null){val rect=Rect();node.getBoundsInScreen(rect);return PointF(rect.exactCenterX(),rect.exactCenterY())}
            SystemClock.sleep(50)
        }
        throw AssertionError("Clock not found on normal Home")
    }

    private fun drag(from:PointF,to:PointF,cancel:Boolean=false,hold:Boolean=true,moving:()->Unit={}) {
        val down=SystemClock.uptimeMillis()
        fun inject(action:Int,point:PointF){
            val properties=MotionEvent.PointerProperties().apply{id=0;toolType=MotionEvent.TOOL_TYPE_FINGER}
            val coordinates=MotionEvent.PointerCoords().apply{x=point.x;y=point.y;pressure=1f;size=1f}
            val event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,1,arrayOf(properties),arrayOf(coordinates),0,0,1f,1f,0,0,InputDevice.SOURCE_TOUCHSCREEN,0)
            try{assertTrue(instrumentation.uiAutomation.injectInputEvent(event,true))}finally{event.recycle()}
        }
        inject(MotionEvent.ACTION_DOWN,from)
        if(hold)SystemClock.sleep(ViewConfiguration.getLongPressTimeout()+100L)
        for(step in 1..15){val n=step/15f;inject(MotionEvent.ACTION_MOVE,PointF(from.x+(to.x-from.x)*n,from.y+(to.y-from.y)*n));SystemClock.sleep(16)}
        moving()
        inject(if(cancel)MotionEvent.ACTION_CANCEL else MotionEvent.ACTION_UP,to)
        instrumentation.waitForIdleSync()
    }

    private fun fixture(gesturesEnabled:Boolean=false)=LauncherData(settings=Preferences(showDate=false,showBattery=false,favoriteCount=0,
        ui=UiPreferences(bottomShortcuts=false,experience=ExperiencePreferences(language="en",autoLaunch=false),v2=V2Preferences(gesturesEnabled=gesturesEnabled))))

    @Test fun longPressDragReleaseCommitsOnlyAfterDropAndSurvivesRecreation()=runBlocking<Unit> {
        val repository=ConfigurationRepository(instrumentation.targetContext)
        val original=repository.data.first()
        var observer:Job?=null
        try {
            repository.restore(fixture(gesturesEnabled=true))
            val observed=MutableStateFlow<LauncherData?>(null)
            val emissions=java.util.concurrent.atomic.AtomicInteger()
            observer=launch(Dispatchers.IO){repository.data.collect{emissions.incrementAndGet();observed.value=it}}
            withTimeout(5000){observed.filterNotNull().first()}
            ActivityScenario.launch(MainActivity::class.java).use{scenario->
                val from=clockCenter();val count=emissions.get()
                drag(from,PointF(from.x+50f,from.y+100f)){
                    assertEquals("No DataStore emission while moving",count,emissions.get())
                    assertTrue("Visual placement follows held pointer",clockCenter().y>from.y+30f)
                }
                val saved=withTimeout(5000){observed.filterNotNull().first{it.settings.ui.v2.widgetPositions["clock"]!=null}}.settings.ui.v2.widgetPositions.getValue("clock")
                assertEquals("One completed placement",count+1,emissions.get())
                val end=clockCenter();assertTrue(end.y>from.y)
                scenario.recreate()
                assertEquals(saved,ConfigurationRepository(instrumentation.targetContext).data.first().settings.ui.v2.widgetPositions["clock"])
                assertEquals(end.x,clockCenter().x,1f);assertEquals(end.y,clockCenter().y,1f)
            }
        }finally{observer?.cancelAndJoin();repository.restore(original)}
    }

    @Test fun longPressDragCancelRestoresOriginalPositionWithoutSaving()=runBlocking<Unit> {
        val repository=ConfigurationRepository(instrumentation.targetContext)
        val original=repository.data.first()
        try {
            repository.restore(fixture())
            ActivityScenario.launch(MainActivity::class.java).use{
                val from=clockCenter()
                drag(from,PointF(from.x+40f,from.y+100f),cancel=true){assertTrue(clockCenter().y>from.y+30f)}
                assertEquals(from.x,clockCenter().x,1f);assertEquals(from.y,clockCenter().y,1f)
                assertTrue(repository.data.first().settings.ui.v2.widgetPositions.isEmpty())
                // A quick swipe over a widget must never acquire placement.
                drag(from,PointF(from.x+40f,from.y+100f),hold=false)
                assertTrue(repository.data.first().settings.ui.v2.widgetPositions.isEmpty())
                // Moving first, then pausing, is still a scroll rather than a hold-and-drag.
                drag(from,PointF(from.x+40f,from.y+20f),hold=false){SystemClock.sleep(ViewConfiguration.getLongPressTimeout()+100L)}
                assertEquals(from.x,clockCenter().x,1f);assertEquals(from.y,clockCenter().y,1f)
                assertTrue(repository.data.first().settings.ui.v2.widgetPositions.isEmpty())
            }
        }finally{repository.restore(original)}
    }
}
