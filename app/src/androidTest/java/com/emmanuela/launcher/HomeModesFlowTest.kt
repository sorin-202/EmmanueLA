package com.emmanuela.launcher

import android.content.ComponentName
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.emmanuela.launcher.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeModesFlowTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private fun nodes(node: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> =
        if (node == null) emptyList() else listOf(node) + (0 until node.childCount).flatMap { nodes(node.getChild(it)) }

    private fun find(label: String, slider: Boolean = false): AccessibilityNodeInfo {
        val deadline = SystemClock.uptimeMillis() + 5000
        while (SystemClock.uptimeMillis() < deadline) {
            nodes(instrumentation.uiAutomation.rootInActiveWindow).firstOrNull {
                it.isVisibleToUser && (it.contentDescription?.toString() == label || it.text?.toString() == label) &&
                    (!slider || it.rangeInfo != null)
            }?.let { return it }
            SystemClock.sleep(50)
        }
        throw AssertionError("Missing Home mode control: $label. Visible: " +
            nodes(instrumentation.uiAutomation.rootInActiveWindow).filter { it.isVisibleToUser }
                .mapNotNull { it.contentDescription?.toString() ?: it.text?.toString() }.joinToString(" | "))
    }

    private fun click(label: String) {
        var previous: Rect? = null
        var stable = 0
        val deadline = SystemClock.uptimeMillis() + 5000
        while (SystemClock.uptimeMillis() < deadline) {
            val node = find(label)
            val bounds = Rect().also(node::getBoundsInScreen)
            stable = if (bounds == previous && !bounds.isEmpty) stable + 1 else 0
            if (stable >= 2) {
                val downTime = SystemClock.uptimeMillis()
                listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP).forEach { action ->
                    val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action,
                        bounds.exactCenterX(), bounds.exactCenterY(), 0).apply { source = InputDevice.SOURCE_TOUCHSCREEN }
                    try { assertTrue(instrumentation.uiAutomation.injectInputEvent(event, true)) } finally { event.recycle() }
                }
                instrumentation.waitForIdleSync()
                return
            }
            previous = bounds
            SystemClock.sleep(75)
        }
        throw AssertionError("Home mode control did not settle: $label")
    }

    private fun installedSettings(): LaunchableApp {
        val info = context.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER).setPackage("com.android.settings"), 0).first().activityInfo
        return LaunchableApp(ComponentName(info.packageName, info.name).flattenToString(),
            info.loadLabel(context.packageManager).toString(), info.packageName)
    }

    private fun fixture(mode: String? = null) = LauncherData(settings = Preferences(showClock = false,
        showDate = false, showBattery = false, favoriteCount = 3, ui = UiPreferences(bottomShortcuts = false,
            v2 = V2Preferences(gesturesEnabled = false), experience = ExperiencePreferences(language = "en",
                autoLaunch = false, homeMode = mode))))

    @Test fun settingsSliderPersistsEachLayoutAndHomeRendersItAfterRecreation() = runBlocking<Unit> {
        val repository = ConfigurationRepository(context)
        val original = repository.data.first()
        val priority = listOf("com.android.dialer", "com.android.messaging", "com.android.camera2",
            "com.android.deskclock", "com.android.documentsui", "com.android.settings")
        val favorites = context.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER), 0).map { it.activityInfo }
            .filter { it.packageName != context.packageName }.distinctBy { it.packageName }
            .sortedWith(compareBy({ priority.indexOf(it.packageName).takeIf { index -> index >= 0 } ?: priority.size }, { it.packageName }))
            .take(6).map { Favorite(ComponentName(it.packageName, it.name).flattenToString()) }
        val representativeHome = fixture().let { it.copy(settings = it.settings.copy(showClock = true, favoriteCount = 6), favorites = favorites) }
        val observed = MutableStateFlow<LauncherData?>(null)
        var observer: Job? = null
        var stage = "prepare fixture"
        fun checkpoint(value: String) {
            stage = value
            android.util.Log.i("HomeModesFixture", "stage=$stage; persistedMode=${observed.value?.settings?.ui?.experience?.homeMode}")
        }
        try {
            repository.restore(representativeHome)
            // Observe before the Android slider action so its UI-triggered write
            // cannot race the start of a new cold DataStore collector.
            observer = launch(Dispatchers.IO) { repository.data.collect { observed.value = it } }
            withTimeout(5000) { observed.filterNotNull().first() }
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                checkpoint("open Home settings")
                find("Home layout: Minimalist")
                click("Launcher settings")
                click("Home")
                for (mode in listOf(HomeModes.NORMAL, HomeModes.ALPHABET, HomeModes.MINIMALIST)) {
                    checkpoint("settle slider before selecting $mode")
                    val deadline = SystemClock.uptimeMillis() + 5000
                    var previous: Pair<Rect, Float>? = null
                    var stable = 0
                    var slider: AccessibilityNodeInfo? = null
                    while (SystemClock.uptimeMillis() < deadline) {
                        val candidate = find("Home layout", slider = true)
                        val bounds = Rect().also(candidate::getBoundsInScreen)
                        val sample = bounds to candidate.rangeInfo.current
                        stable = if (!bounds.isEmpty && sample == previous) stable + 1 else 0
                        previous = sample
                        if (stable >= 2) { slider = candidate; break }
                        SystemClock.sleep(75)
                    }
                    assertNotNull("The actual Home slider must settle before requesting $mode: $previous", slider)
                    val settled = requireNotNull(slider)
                    android.util.Log.i("HomeModesFixture", "Request $mode: bounds=${previous?.first}; current=${settled.rangeInfo.current}; range=${settled.rangeInfo.min}..${settled.rangeInfo.max}")
                    assertTrue(settled.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS.id,
                        Bundle().apply { putFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE, HomeModes.values.indexOf(mode).toFloat()) }))
                    checkpoint("await persisted $mode after slider action")
                    withTimeout(5000) { observed.filterNotNull().first { it.settings.ui.experience.homeMode == mode } }
                    assertEquals("Slider changes must persist in the repository", mode, repository.data.first().settings.ui.experience.homeMode)
                    checkpoint("verify Home renders $mode")
                    click("Back")
                    click("Back")
                    find("Home layout: $mode")
                    scenario.recreate()
                    find("Home layout: $mode")
                    assertEquals(mode, HomeModes.effective(repository.data.first().settings.ui.experience))
                    captureUxScreenshot("home-${mode.lowercase(java.util.Locale.ROOT)}")
                    click("Launcher settings")
                    click("Home")
                }
            }
        } catch (failure: Throwable) {
            val visible = nodes(instrumentation.uiAutomation.rootInActiveWindow).filter { it.isVisibleToUser }
                .mapNotNull { it.contentDescription?.toString() ?: it.text?.toString() }.joinToString(" | ")
            throw AssertionError("Home slider workflow failed: stage=$stage; persistedMode=${observed.value?.settings?.ui?.experience?.homeMode}; UI=$visible", failure)
        } finally { observer?.cancelAndJoin(); repository.restore(original) }
    }

    @Test fun everyLayoutKeepsHiddenFavoritesHiddenAndUsesBlockedLaunch() = runBlocking<Unit> {
        val repository = ConfigurationRepository(context)
        val original = repository.data.first()
        val app = installedSettings()
        val alias = "AAA protected favorite"
        try {
            HomeModes.values.forEach { mode ->
                repository.restore(fixture(mode).copy(favorites = listOf(Favorite("com.hidden/.Main"), Favorite(app.id)),
                    appMetadata = mapOf("com.hidden/.Main" to AppMetadata("HIDDEN FAVORITE"), app.id to AppMetadata(alias)),
                    policies = mapOf("com.hidden" to AppPolicy(hidden = true), app.packageName to AppPolicy(blocked = true))))
                ActivityScenario.launch(MainActivity::class.java).use {
                    find("Home layout: $mode")
                    find(alias)
                    assertFalse(nodes(instrumentation.uiAutomation.rootInActiveWindow).any { node ->
                        node.isVisibleToUser && (node.contentDescription?.toString() == "HIDDEN FAVORITE" || node.text?.toString() == "HIDDEN FAVORITE")
                    })
                    click(alias)
                    find("App unavailable in EmmanueLA")
                    assertEquals(context.packageName, instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString())
                    click("Close")
                }
            }
        } finally { repository.restore(original) }
    }

    @Test fun normalIconFavoriteLaunchesInstalledApplication() = runBlocking<Unit> {
        val repository = ConfigurationRepository(context)
        val original = repository.data.first()
        val app = installedSettings()
        try {
            repository.restore(fixture(HomeModes.NORMAL).copy(favorites = listOf(Favorite(app.id))))
            ActivityScenario.launch(MainActivity::class.java).use {
                find("Home layout: Normal")
                click(app.label)
                val deadline = SystemClock.uptimeMillis() + 5000
                while (instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString() != app.packageName && SystemClock.uptimeMillis() < deadline) SystemClock.sleep(50)
                assertEquals(app.packageName, instrumentation.uiAutomation.rootInActiveWindow?.packageName?.toString())
            }
        } finally { repository.restore(original) }
    }
}
