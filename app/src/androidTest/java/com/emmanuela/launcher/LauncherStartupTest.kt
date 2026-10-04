package com.emmanuela.launcher

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Executes generated Compose code on Android, catching packaging failures JVM tests miss. */
@RunWith(AndroidJUnit4::class)
class LauncherStartupTest {
    @Test fun homeRendersAndSurvivesActivityRecreation() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            instrumentation.waitForIdleSync()
            scenario.onActivity { activity ->
                assertFalse(activity.isFinishing)
                assertTrue(activity.window.decorView.isAttachedToWindow)
            }
            scenario.recreate()
            instrumentation.waitForIdleSync()
            scenario.onActivity { assertFalse(it.isFinishing) }
        }
    }
}
