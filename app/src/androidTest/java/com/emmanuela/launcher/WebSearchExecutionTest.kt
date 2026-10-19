package com.emmanuela.launcher

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.emmanuela.launcher.data.*
import com.emmanuela.launcher.platform.WebSearch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the real AllApps -> ViewModel -> Android startActivity path, intercepting the browser. */
@RunWith(AndroidJUnit4::class)
class WebSearchExecutionTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private fun nodes(node: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> =
        if (node == null) emptyList() else listOf(node) + (0 until node.childCount).flatMap { nodes(node.getChild(it)) }

    private fun find(matches: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo {
        val deadline = SystemClock.uptimeMillis() + 5_000
        while (SystemClock.uptimeMillis() < deadline) {
            nodes(instrumentation.uiAutomation.rootInActiveWindow).firstOrNull { it.isVisibleToUser && matches(it) }?.let { return it }
            SystemClock.sleep(50)
        }
        throw AssertionError("Search control did not appear")
    }

    @Test fun slashEnterPersistsQueryAndStartsConfiguredEngineWithoutResultTap() = runBlocking {
        val context = instrumentation.targetContext
        val target = context.packageManager.resolveActivity(WebSearch.browserIntent("test"), PackageManager.MATCH_DEFAULT_ONLY)
        assumeTrue("This Android API integration test requires an installed default HTTPS browser", target != null && target.activityInfo.packageName != "android")
        val repository = ConfigurationRepository(context)
        val original = repository.data.first()
        val history = WebSearchHistoryRepository(context)
        val originalHistory = history.queries.first()
        val originalEngine = history.engine.first()
        val launched = MutableStateFlow<Intent?>(null)
        val monitor = object : Instrumentation.ActivityMonitor() {
            override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
                if (intent.action == Intent.ACTION_VIEW && intent.data?.host == "www.google.com") {
                    launched.value = Intent(intent)
                    return Instrumentation.ActivityResult(Activity.RESULT_OK, null)
                }
                return null
            }
        }
        instrumentation.addMonitor(monitor)
        try {
            history.clear()
            history.setEngine(WebSearchEngine.GOOGLE)
            repository.restore(LauncherData(settings = Preferences(ui = UiPreferences(experience = ExperiencePreferences(language = "en", autoLaunch = false)))))
            ActivityScenario.launch(MainActivity::class.java).use {
                var apps: AccessibilityNodeInfo? = find { it.text?.toString() == "All Apps" }
                while (apps != null && !apps.isClickable) apps = apps.parent
                assertTrue(apps?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true)
                val field = find { it.className?.toString() == "android.widget.EditText" }
                assertTrue(field.performAction(AccessibilityNodeInfo.ACTION_CLICK))
                assertTrue(field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "/celestial navigation")
                }))
                find { it.text?.toString() == "Type after / and press Search" }
                find { it.text?.toString() == "Search engine: Google" }
                instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_ENTER)
                val intent = withTimeout(5_000) { launched.filterNotNull().first() }
                assertEquals("https", intent.data?.scheme)
                assertEquals("celestial navigation", intent.data?.getQueryParameter("q"))
                assertEquals(listOf("celestial navigation"), history.queries.first())
            }
        } finally {
            instrumentation.removeMonitor(monitor)
            repository.restore(original)
            history.clear()
            originalHistory.asReversed().forEach { history.record(it) }
            history.setEngine(originalEngine)
        }
    }
}
