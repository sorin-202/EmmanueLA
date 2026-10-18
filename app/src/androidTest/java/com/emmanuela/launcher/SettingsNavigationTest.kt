package com.emmanuela.launcher

import android.os.Build
import android.os.SystemClock
import android.graphics.Rect
import android.view.InputDevice
import android.view.MotionEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.emmanuela.launcher.data.ConfigurationRepository
import com.emmanuela.launcher.data.FocusGroup
import com.emmanuela.launcher.data.ExperiencePreferences
import com.emmanuela.launcher.data.LauncherData
import com.emmanuela.launcher.data.Preferences
import com.emmanuela.launcher.data.UiPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Navigation must expose the same working settings without changing hidden configuration. */
@RunWith(AndroidJUnit4::class)
class SettingsNavigationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun nodes(node: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> =
        if (node == null) emptyList() else listOf(node) + (0 until node.childCount).flatMap { nodes(node.getChild(it)) }

    private fun find(label: String, scroll: Boolean = false): AccessibilityNodeInfo {
        val deadline = SystemClock.uptimeMillis() + 5000
        var nextScroll = SystemClock.uptimeMillis() + 350
        while (SystemClock.uptimeMillis() < deadline) {
            val tree = nodes(instrumentation.uiAutomation.rootInActiveWindow)
            val visible = tree.filter { it.isVisibleToUser && it.packageName?.toString() == instrumentation.targetContext.packageName }
            (visible.firstOrNull { it.contentDescription?.toString() == label }
                ?: visible.firstOrNull { it.text?.toString() == label })?.let { return it }
            // The same disclosure header can be above the edited control. Ask Android
            // to reveal that exact target rather than only scrolling further down.
            val target = tree.firstOrNull { it.contentDescription?.toString() == label }
                ?: tree.firstOrNull { it.text?.toString() == label }
            if (scroll && target?.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.id) == true) {
                nextScroll = SystemClock.uptimeMillis() + 350
            }
            if (scroll && SystemClock.uptimeMillis() >= nextScroll) {
                tree.asReversed().firstOrNull { it.isScrollable && it.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) }
                nextScroll = SystemClock.uptimeMillis() + 350
            }
            SystemClock.sleep(50)
        }
        throw AssertionError("Missing settings control: $label. Visible: " +
            nodes(instrumentation.uiAutomation.rootInActiveWindow).filter { it.isVisibleToUser }
                .mapNotNull { it.contentDescription?.toString() ?: it.text?.toString() }.joinToString(" | "))
    }

    private fun click(label: String, scroll: Boolean = false) {
        val deadline = SystemClock.uptimeMillis() + 5000
        var previous: Rect? = null
        var stable = 0
        var requestedOnScreen = false
        while (SystemClock.uptimeMillis() < deadline) {
            var node: AccessibilityNodeInfo? = find(label, scroll)
            while (node != null && !node.isClickable) node = node.parent
            assertNotNull("Settings control must be clickable: $label", node)
            if (scroll && !requestedOnScreen) {
                node!!.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.id)
                requestedOnScreen = true
                instrumentation.waitForIdleSync()
                continue
            }
            val bounds = Rect().also { node!!.getBoundsInScreen(it) }
            stable = if (bounds == previous && !bounds.isEmpty) stable + 1 else 0
            if (stable >= 2) {
                val down = SystemClock.uptimeMillis()
                for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
                    val event = MotionEvent.obtain(down, SystemClock.uptimeMillis(), action,
                        bounds.exactCenterX(), bounds.exactCenterY(), 0).apply { source = InputDevice.SOURCE_TOUCHSCREEN }
                    try { assertTrue(instrumentation.uiAutomation.injectInputEvent(event, true)) } finally { event.recycle() }
                }
                instrumentation.waitForIdleSync()
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
        try {
            repository.restore(fixture)
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
                withTimeout(5000) { repository.data.first { it.settings.ui.experience.advanced } }
                scenario.recreate()
                find("SETTINGS")
                if (Build.VERSION.SDK_INT >= 30) assertEquals("Shown", find("More options").stateDescription?.toString())
                val expected = fixture.settings.copy(ui = fixture.settings.ui.copy(
                    experience = fixture.settings.ui.experience.copy(advanced = true)))
                assertEquals("Showing extra controls must preserve every existing setting", expected, repository.data.first().settings)
                click("More options")
                withTimeout(5000) { repository.data.first { !it.settings.ui.experience.advanced } }
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
        } finally { repository.restore(original) }
    }

    @Test fun appListDisclosureRestoresItsStateAndKeepsAnEditedSearchPreference() = runBlocking<Unit> {
        val repository = ConfigurationRepository(instrumentation.targetContext)
        val original = repository.data.first()
        try {
            repository.restore(LauncherData(settings = Preferences(autoKeyboard = false, ui = UiPreferences(
                experience = ExperiencePreferences(language = "en", autoLaunch = false)))))
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                click("Launcher settings")
                click("App list")
                find("APP LIST")
                captureUxScreenshot("settings-app-list")
                click("Search", scroll = true)
                click("Auto keyboard", scroll = true)
                withTimeout(5000) { repository.data.first { it.settings.autoKeyboard } }
                click("Search", scroll = true)
                scenario.recreate()
                find("APP LIST")
                if (Build.VERSION.SDK_INT >= 30) assertEquals("Collapsed", find("Search", scroll = true).stateDescription?.toString())
                assertTrue(ConfigurationRepository(instrumentation.targetContext).data.first().settings.autoKeyboard)
                click("Search", scroll = true)
                assertTrue("Edited search preference stays enabled after reopening", find("Auto keyboard", scroll = true).isChecked)
            }
        } finally { repository.restore(original) }
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
