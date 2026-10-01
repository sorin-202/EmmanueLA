package com.emmanuela.launcher.platform

import android.content.Context
import android.database.ContentObserver
import android.media.AudioAttributes
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

/** System touch preference is observed, never polled during finger movement. */
class LauncherHaptics(private val context: Context) {
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31)
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    else context.getSystemService(Vibrator::class.java)
    private val effect = VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE)
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
    private var enabled = true
    private var lastTick = Long.MIN_VALUE
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) { refresh() }
    }
    @Suppress("DEPRECATION")
    private fun refresh() {
        enabled = Settings.System.getInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0
    }
    @Suppress("DEPRECATION")
    fun observe() {
        refresh()
        context.contentResolver.registerContentObserver(Settings.System.getUriFor(Settings.System.HAPTIC_FEEDBACK_ENABLED), false, observer)
    }
    fun close() { context.contentResolver.unregisterContentObserver(observer) }
    fun tick(view: View): String {
        if (!enabled) return "Touch vibration is disabled in Android settings"
        val now = SystemClock.uptimeMillis()
        if (lastTick != Long.MIN_VALUE && now - lastTick < 35) return "Feedback rate limited"
        lastTick = now
        if (view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)) return "Haptic feedback sent"
        val device = vibrator ?: return "This device has no vibrator"
        if (!device.hasVibrator()) return "This device has no vibrator"
        return try {
            device.vibrate(effect, attributes)
            "Vibration sent"
        } catch (_: SecurityException) { "Vibration permission unavailable" }
    }
}

@Composable
fun rememberLauncherHapticTick(): () -> String {
    val context = LocalContext.current.applicationContext
    val view = LocalView.current
    val controller = remember(context) { LauncherHaptics(context) }
    DisposableEffect(controller) { controller.observe(); onDispose { controller.close() } }
    return remember(controller, view) { { controller.tick(view) } }
}

@Composable
fun HapticTestRow() {
    val tick = rememberLauncherHapticTick()
    var result by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    val context = LocalContext.current
    androidx.compose.foundation.layout.Column {
        androidx.compose.material3.TextButton(onClick = { result = tick() }) {
            androidx.compose.material3.Text("Test haptics")
        }
        if (result.isNotEmpty()) androidx.compose.material3.Text(result, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        androidx.compose.material3.TextButton(onClick = {
            runCatching { context.startActivity(android.content.Intent(Settings.ACTION_SOUND_SETTINGS)) }
        }) { androidx.compose.material3.Text("Android sound and vibration settings") }
    }
}
