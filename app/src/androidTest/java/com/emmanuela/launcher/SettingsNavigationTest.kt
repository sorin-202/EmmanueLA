package com.emmanuela.launcher

import android.os.Build
import android.os.SystemClock
import android.graphics.Rect
import android.view.InputDevice
import android.view.MotionEvent
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.emmanuela.launcher.data.ConfigurationRepository
import com.emmanuela.launcher.data.FocusGroup
import com.emmanuela.launcher.data.ExperiencePreferences
import com.emmanuela.launcher.data.LauncherData
import com.emmanuela.launcher.data.Preferences
import com.emmanuela.launcher.data.UiPreferences
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

/** Navigation must expose the same working settings without changing hidden configuration. */
@RunWith(AndroidJUnit4::class)
class SettingsNavigationTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private fun locate(label: String, clickable: Boolean = false): SemanticsNodeInteraction {
        fun matcher(text: Boolean): SemanticsMatcher {
            val labelMatcher = if (text) hasText(label) else hasContentDescription(label)
            return if (clickable) labelMatcher and hasClickAction() else labelMatcher
        }
        val text = compose.onAllNodes(matcher(text = true))
        val description = compose.onAllNodes(matcher(text = false))
        compose.waitUntil(5000) {
            text.fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty() ||
                description.fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
        }
        // Home's underlying launcher surface also exposes "Home". Its settings
        // card has actual text; prefer that before any content-description match.
        return if (text.fetchSemanticsNodes().isNotEmpty()) text.onFirst() else description.onFirst()
    }

    private fun find(label: String, scroll: Boolean = false): SemanticsNodeInteraction =
        locate(label).also { if (scroll) it.performScrollTo(); it.assertIsDisplayed() }

    private fun control(label: String, scroll: Boolean = false): SemanticsNodeInteraction =
        locate(label, clickable = true).also { if (scroll) it.performScrollTo(); it.assertIsDisplayed() }

    private fun screenBounds(node: SemanticsNode): Rect = Rect(
        node.positionOnScreen.x.roundToInt(), node.positionOnScreen.y.roundToInt(),
        (node.positionOnScreen.x + node.size.width).roundToInt(),
        (node.positionOnScreen.y + node.size.height).roundToInt())

    private fun visibleTapBounds(node: SemanticsNode): Rect {
        // boundsInWindow includes ancestor clipping, unlike the bounds of a
        // virtual AccessibilityNodeInfo after a disclosure expands offscreen.
        val offset = node.positionOnScreen - node.positionInWindow
        val clipped = node.boundsInWindow
        val visible = Rect((clipped.left + offset.x).roundToInt(), (clipped.top + offset.y).roundToInt(),
            (clipped.right + offset.x).roundToInt(), (clipped.bottom + offset.y).roundToInt())
        val appFrame = Rect()
        instrumentation.runOnMainSync {
            val activity = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED).filterIsInstance<MainActivity>().singleOrNull()
            assertNotNull("Launcher must be resumed before injecting a settings tap", activity)
            val decor = activity!!.window.decorView
            decor.getWindowVisibleDisplayFrame(appFrame)
            val insets = ViewCompat.getRootWindowInsets(decor)
                ?.getInsets(WindowInsetsCompat.Type.systemBars())
            if (insets != null) {
                val location = IntArray(2).also(decor::getLocationOnScreen)
                val safeFrame = Rect(location[0] + insets.left, location[1] + insets.top,
                    location[0] + decor.width - insets.right, location[1] + decor.height - insets.bottom)
                if (!appFrame.intersect(safeFrame)) appFrame.setEmpty()
            }
        }
        if (!visible.intersect(appFrame)) visible.setEmpty()
        return visible
    }

    private fun scrollWithinSafeViewport(node: SemanticsNode, bounds: Rect) {
        val scrollable = generateSequence(node.parent) { it.parent }
            .firstOrNull { hasScrollAction().matches(it) }
        assertNotNull("Clipped settings control must have a scrollable ancestor", scrollable)
        val viewport = visibleTapBounds(scrollable!!)
        assertFalse("Scrollable settings body must be visible", viewport.isEmpty)
        val upward = bounds.bottom > viewport.bottom
        assertTrue("Vertical scrolling must be able to reveal $bounds inside $viewport",
            upward || bounds.top < viewport.top)
        val distance = minOf(300f, viewport.height() * .4f)
        val fromY = viewport.exactCenterY() + if (upward) distance / 2 else -distance / 2
        val toY = viewport.exactCenterY() + if (upward) -distance / 2 else distance / 2
        val downTime = SystemClock.uptimeMillis()
        fun pointer(action: Int, y: Float) {
            val event = MotionEvent.obtain(downTime, SystemClock.uptimeMillis(), action,
                viewport.exactCenterX(), y, 0).apply { source = InputDevice.SOURCE_TOUCHSCREEN }
            try { assertTrue(instrumentation.uiAutomation.injectInputEvent(event, true)) } finally { event.recycle() }
        }
        android.util.Log.i("SettingsNavigation", "Native scroll in $viewport from $fromY to $toY to reveal $bounds")
        pointer(MotionEvent.ACTION_DOWN, fromY)
        for (step in 1..12) {
            pointer(MotionEvent.ACTION_MOVE, fromY + (toY - fromY) * step / 12f)
            SystemClock.sleep(16)
        }
        // Pause the finger before lifting so the viewport does not fling past
        // this control while the helper checks its visible bounds again.
        repeat(6) { pointer(MotionEvent.ACTION_MOVE, toY); SystemClock.sleep(16) }
        pointer(MotionEvent.ACTION_UP, toY)
        compose.waitForIdle()
    }

    private fun click(label: String, scroll: Boolean = false) {
        val target = control(label, scroll)
        val deadline = SystemClock.uptimeMillis() + 5000
        var previous: Rect? = null
        var stable = 0
        while (SystemClock.uptimeMillis() < deadline) {
            compose.waitForIdle()
            val node = target.fetchSemanticsNode()
            val bounds = screenBounds(node)
            val visible = visibleTapBounds(node)
            android.util.Log.i("SettingsNavigation", "Target $label semantics=${node.id} bounds=$bounds viewport=$visible")
            if (visible.isEmpty || visible != bounds) {
                assertTrue("Settings control must fit its visible viewport: $label ($bounds, $visible)", scroll)
                // The semantic scroll viewport can include Android's navigation
                // region. Another performScrollTo() is then a no-op; use the
                // actual visible body for a normal native scroll instead.
                scrollWithinSafeViewport(node, bounds)
                previous = null
                stable = 0
                SystemClock.sleep(50)
                continue
            }
            stable = if (bounds == previous && !bounds.isEmpty) stable + 1 else 0
            if (stable >= 2) {
                android.util.Log.i("SettingsNavigation", "Native tap $label at $bounds in visible viewport $visible")
                val down = SystemClock.uptimeMillis()
                for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
                    val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action,
                        bounds.exactCenterX(), bounds.exactCenterY(), 0).apply { source = InputDevice.SOURCE_TOUCHSCREEN }
                    try { assertTrue(instrumentation.uiAutomation.injectInputEvent(event, true)) } finally { event.recycle() }
                }
                compose.waitForIdle()
                return
            }
            previous = bounds
            SystemClock.sleep(75)
        }
        throw AssertionError("Settings control did not settle: $label")
    }

    @Test fun groupedRoutesAndExplicitMoreOptionsKeepExistingValuesAfterRecreation() = runBlocking<Unit> {
        val repository = ConfigurationRepository(instrumentation.targetContext)
        val original = repository.data.first()
        val fixture = LauncherData(settings = Preferences(ui = UiPreferences(
            bottomBarWidth = .73f, bottomBarHeight = 67f, bottomBarPadding = 19f,
            experience = ExperiencePreferences(language = "en", advanced = false, autoLaunch = false))))
        val observed = MutableStateFlow<LauncherData?>(null)
        var observer: Job? = null
        try {
            repository.restore(fixture)
            // Subscribe before UI writes, as in LauncherFlowTest: DataStore 1.1.7
            // can race a newly started cold collector with a concurrent edit.
            observer = launch(Dispatchers.IO) { repository.data.collect { observed.value = it } }
            withTimeout(5000) { observed.filterNotNull().first() }
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                click("Launcher settings")
                find("SETTINGS")
                captureUxScreenshot("settings-overview")
                click("App & system", scroll = true)
                click("System", scroll = true)
                find("SYSTEM")
                click("Back")
                find("SETTINGS")
                click("More options")
                withTimeout(5000) { observed.filterNotNull().first { it.settings.ui.experience.advanced } }
                assertTrue(repository.data.first().settings.ui.experience.advanced)
                scenario.recreate()
                find("SETTINGS")
                if (Build.VERSION.SDK_INT >= 30) control("More options").assert(
                    SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Shown"))
                val expected = fixture.settings.copy(ui = fixture.settings.ui.copy(
                    experience = fixture.settings.ui.experience.copy(advanced = true)))
                assertEquals("Showing extra controls must preserve every existing setting", expected, repository.data.first().settings)
                click("More options")
                withTimeout(5000) { observed.filterNotNull().first { !it.settings.ui.experience.advanced } }
                assertEquals(fixture.settings, repository.data.first().settings)
                click("Personalize", scroll = true)
                click("Appearance", scroll = true)
                find("APPEARANCE")
                click("Back")
                click("App & system", scroll = true)
                click("Apps", scroll = true)
                find("APPS")
                click("Notifications")
                find("NOTIFICATIONS")
                assertEquals("Visiting reorganized pages must not alter configuration", fixture.settings, repository.data.first().settings)
            }
        } finally {
            observer?.cancelAndJoin()
            repository.restore(original)
        }
    }

    @Test fun appListDisclosureRestoresItsStateAndKeepsAnEditedSearchPreference() = runBlocking<Unit> {
        val repository = ConfigurationRepository(instrumentation.targetContext)
        val original = repository.data.first()
        val observed = MutableStateFlow<LauncherData?>(null)
        var observer: Job? = null
        try {
            repository.restore(LauncherData(settings = Preferences(autoKeyboard = false, ui = UiPreferences(
                experience = ExperiencePreferences(language = "en", autoLaunch = false)))))
            observer = launch(Dispatchers.IO) { repository.data.collect { observed.value = it } }
            withTimeout(5000) { observed.filterNotNull().first() }
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                click("Launcher settings")
                click("App list")
                find("APP LIST")
                captureUxScreenshot("settings-app-list")
                click("Search", scroll = true)
                click("Auto keyboard", scroll = true)
                withTimeout(5000) { observed.filterNotNull().first { it.settings.autoKeyboard } }
                assertTrue(repository.data.first().settings.autoKeyboard)
                click("Search", scroll = true)
                scenario.recreate()
                find("APP LIST")
                if (Build.VERSION.SDK_INT >= 30) control("Search", scroll = true).assert(
                    SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Collapsed"))
                assertTrue(ConfigurationRepository(instrumentation.targetContext).data.first().settings.autoKeyboard)
                click("Search", scroll = true)
                control("Auto keyboard", scroll = true).assertIsOn()
            }
        } finally {
            observer?.cancelAndJoin()
            repository.restore(original)
        }
    }

    @Test fun homeAndFocusSettingsRemainReachableWithRepresentativeSavedRules() = runBlocking<Unit> {
        val repository = ConfigurationRepository(instrumentation.targetContext)
        val original = repository.data.first()
        val fixture = LauncherData(settings = Preferences(ui = UiPreferences(
            experience = ExperiencePreferences(language = "en", autoLaunch = false))),
            focusGroups = listOf(FocusGroup(id = "ux-sample-focus", name = "Study time",
                packages = setOf("com.android.settings"), websites = setOf("youtube.com/shorts"),
                browsers = setOf("org.mozilla.firefox"), dailyMinutes = 30)))
        try {
            repository.restore(fixture)
            ActivityScenario.launch(MainActivity::class.java).use {
                click("Launcher settings")
                click("Home")
                find("HOME")
                captureUxScreenshot("settings-home")
                click("Back")
                find("SETTINGS")
                click("Live the Moment", scroll = true)
                find("LIVE THE MOMENT")
                captureUxScreenshot("settings-live-moment")
                click("Study time", scroll = true)
                find("STUDY TIME")
                find("Blocklist")
                captureUxScreenshot("settings-focus-editor")
                assertEquals("Opening new menus must preserve saved rules", fixture.focusGroups, repository.data.first().focusGroups)
                assertEquals("Opening new menus must preserve settings", fixture.settings, repository.data.first().settings)
            }
        } finally { repository.restore(original) }
    }

}
