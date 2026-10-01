package com.emmanuela.launcher.platform

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

/** Per-package union of foreground activity intervals: multiple activities cannot double-count time. */
data class AppUsageEvent(val at: Long, val packageName: String, val activity: String, val resumed: Boolean, val clearAll: Boolean = false)
object AppUsageMath {
    fun totals(events: List<AppUsageEvent>, start: Long, end: Long): Map<String, Long> {
        val active = mutableMapOf<String, MutableSet<String>>()
        val totals = mutableMapOf<String, Long>()
        var previous = start
        for (e in events.sortedBy { it.at }) {
            if (e.at > end) break
            val time = e.at.coerceAtLeast(start)
            active.filterValues { it.isNotEmpty() }.keys.forEach { pkg -> totals[pkg] = (totals[pkg] ?: 0L) + (time - previous).coerceAtLeast(0) }
            if (e.clearAll) active.clear()
            else if (e.resumed) active.getOrPut(e.packageName) { mutableSetOf() }.add(e.activity)
            else active[e.packageName]?.remove(e.activity)
            previous = time
        }
        active.filterValues { it.isNotEmpty() }.keys.forEach { pkg -> totals[pkg] = (totals[pkg] ?: 0L) + (end - previous).coerceAtLeast(0) }
        return totals
    }
}
@Suppress("DEPRECATION")
suspend fun Context.appUsageToday(): Map<String, Long>? = withContext(Dispatchers.IO) {
    if (!hasUsageAccess()) return@withContext null
    val end = System.currentTimeMillis()
    val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val events = getSystemService(UsageStatsManager::class.java).queryEvents(start - 86_400_000L, end) ?: return@withContext null
    val event = UsageEvents.Event()
    val rows = mutableListOf<AppUsageEvent>()
    while (events.hasNextEvent()) {
        events.getNextEvent(event)
        when (event.eventType) {
            UsageEvents.Event.MOVE_TO_FOREGROUND -> rows += AppUsageEvent(event.timeStamp, event.packageName.orEmpty(), event.className.orEmpty(), true)
            UsageEvents.Event.MOVE_TO_BACKGROUND -> rows += AppUsageEvent(event.timeStamp, event.packageName.orEmpty(), event.className.orEmpty(), false)
            UsageEvents.Event.SCREEN_NON_INTERACTIVE, UsageEvents.Event.DEVICE_SHUTDOWN, UsageEvents.Event.DEVICE_STARTUP ->
                if (Build.VERSION.SDK_INT >= 28) rows += AppUsageEvent(event.timeStamp, "", "", false, true)
        }
    }
    AppUsageMath.totals(rows, start, end)
}
