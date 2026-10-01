package com.emmanuela.launcher.platform

import com.emmanuela.launcher.LockAdminReceiver

import android.app.AppOpsManager
import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId


fun Context.lockReady(): Boolean = getSystemService(DevicePolicyManager::class.java)
    .isAdminActive(ComponentName(this, LockAdminReceiver::class.java))
fun Context.lockDevice(): Boolean = try {
    if (lockReady()) { getSystemService(DevicePolicyManager::class.java).lockNow(); true } else false
} catch (_: SecurityException) { false }
fun Context.lockSetupIntent() = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
    .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, ComponentName(this, LockAdminReceiver::class.java))
    .putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "EmmanueLA uses only screen locking. A PIN may be required after locking. Disable this access before uninstalling.")

@Suppress("DEPRECATION")
fun Context.hasUsageAccess(): Boolean = getSystemService(AppOpsManager::class.java)
    .checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED

/** Union of interactive + unlocked intervals. Boundaries are clamped to the requested day. */
data class ScreenEvent(val at: Long, val kind: Int)
object ScreenTime {
    const val ON = 1; const val OFF = 2; const val UNLOCK = 3; const val LOCK = 4
    fun duration(events: List<ScreenEvent>, start: Long, end: Long): Long {
        var interactive = false; var unlocked = false; var last = start; var total = 0L
        for (event in events.sortedBy { it.at }) {
            if (event.at > end) break
            val time = event.at.coerceAtLeast(start)
            if (interactive && unlocked) total += (time - last).coerceAtLeast(0)
            when (event.kind) { ON -> interactive = true; OFF -> interactive = false; UNLOCK -> { unlocked = true; interactive = true }; LOCK -> unlocked = false }
            last = time
        }
        if (interactive && unlocked) total += (end - last).coerceAtLeast(0)
        return total.coerceIn(0, (end - start).coerceAtLeast(0))
    }
}
suspend fun Context.screenTimeToday(): Long? = withContext(Dispatchers.IO) {
    if (!hasUsageAccess() || Build.VERSION.SDK_INT < 28) return@withContext null
    val end = System.currentTimeMillis()
    val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val events = getSystemService(UsageStatsManager::class.java).queryEvents(start - 86_400_000L, end) ?: return@withContext null
    val rows = mutableListOf<ScreenEvent>()
    val event = UsageEvents.Event()
    while (events.hasNextEvent()) {
        events.getNextEvent(event)
        val kind = when (event.eventType) {
            UsageEvents.Event.SCREEN_INTERACTIVE -> ScreenTime.ON
            UsageEvents.Event.SCREEN_NON_INTERACTIVE -> ScreenTime.OFF
            UsageEvents.Event.DEVICE_SHUTDOWN -> ScreenTime.OFF
            UsageEvents.Event.DEVICE_STARTUP -> ScreenTime.LOCK
            UsageEvents.Event.KEYGUARD_HIDDEN -> ScreenTime.UNLOCK
            UsageEvents.Event.KEYGUARD_SHOWN -> ScreenTime.LOCK
            else -> null
        }
        if (kind != null) rows += ScreenEvent(event.timeStamp, kind)
    }
    ScreenTime.duration(rows, start, end)
}
