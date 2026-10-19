package com.emmanuela.launcher

import android.graphics.Bitmap
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Archives the real Android display for visual review; no UI is reconstructed or mocked. */
fun captureUxScreenshot(name: String) {
    require(name.matches(Regex("[a-z0-9_-]{1,80}")))
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.waitForIdleSync()
    // Semantics can arrive before RenderThread commits the new route and system
    // insets. Capture a committed frame, rather than a partly drawn header.
    val committed = CountDownLatch(1)
    instrumentation.runOnMainSync {
        val activity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED).single()
        val decor = activity.window.decorView
        if (Build.VERSION.SDK_INT >= 29 && decor.isHardwareAccelerated) {
            decor.viewTreeObserver.registerFrameCommitCallback { committed.countDown() }
            decor.invalidate()
        } else decor.postOnAnimation { decor.postOnAnimation { committed.countDown() } }
    }
    check(committed.await(5, TimeUnit.SECONDS)) { "Android did not commit the screenshot frame for $name" }
    instrumentation.waitForIdleSync()
    val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot()) {
        "Android did not provide the screenshot for $name"
    }
    try {
        // UTP copies this directory to the host BEFORE its normal APK uninstall.
        // App external files are removed by that uninstall, so a later adb pull loses them.
        val runnerOutput = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
            ?.takeIf { it.isNotBlank() }
        val directory = if (runnerOutput != null) File(runnerOutput, "ux-screenshots")
            else checkNotNull(instrumentation.targetContext.getExternalFilesDir("ux-screenshots"))
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create screenshot directory" }
        FileOutputStream(File(directory, "$name.png")).use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) { "Could not encode $name" }
        }
    } finally { bitmap.recycle() }
}
