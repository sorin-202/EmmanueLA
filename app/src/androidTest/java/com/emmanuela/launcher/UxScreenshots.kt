package com.emmanuela.launcher

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream

/** Archives the real Android display for visual review; no UI is reconstructed or mocked. */
fun captureUxScreenshot(name: String) {
    require(name.matches(Regex("[a-z0-9_-]{1,80}")))
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.waitForIdleSync()
    val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot()) {
        "Android did not provide the screenshot for $name"
    }
    try {
        val directory = checkNotNull(instrumentation.targetContext.getExternalFilesDir("ux-screenshots"))
        check(directory.isDirectory || directory.mkdirs()) { "Cannot create screenshot directory" }
        FileOutputStream(File(directory, "$name.png")).use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) { "Could not encode $name" }
        }
    } finally { bitmap.recycle() }
}
