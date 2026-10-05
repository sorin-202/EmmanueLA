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
            if(scroll&&SystemClock.uptimeMillis()>=nextScroll){tree.firstOrNull{it.isScrollable}?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);nextScroll=SystemClock.uptimeMillis()+1000}
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
