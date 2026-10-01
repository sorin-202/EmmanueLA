package com.emmanuela.launcher

import com.emmanuela.launcher.data.AppFolder
import com.emmanuela.launcher.data.AppPolicy
import com.emmanuela.launcher.data.ConfigurationCodec
import com.emmanuela.launcher.data.FolderPassword
import com.emmanuela.launcher.data.FolderPlacement
import com.emmanuela.launcher.data.LauncherData
import com.emmanuela.launcher.data.NotificationMode
import com.emmanuela.launcher.data.PolicyRules
import com.emmanuela.launcher.data.Preferences
import com.emmanuela.launcher.data.UiPreferences
import com.emmanuela.launcher.platform.AppUsageEvent
import com.emmanuela.launcher.platform.AppUsageMath


import java.time.ZonedDateTime
import org.junit.Assert.*
import org.junit.Test

class AppPolicyTest {
    @Test fun overnightScheduleBelongsToStartDay() {
        val policy = AppPolicy(scheduleEnabled = true, startMinute = 22 * 60, endMinute = 7 * 60, days = setOf(1))
        assertTrue(PolicyRules.scheduled(policy, ZonedDateTime.parse("2026-09-28T23:00:00+03:00")))
        assertTrue(PolicyRules.scheduled(policy, ZonedDateTime.parse("2026-09-29T06:59:00+03:00")))
        assertFalse(PolicyRules.scheduled(policy, ZonedDateTime.parse("2026-09-29T07:00:00+03:00")))
        assertFalse(PolicyRules.scheduled(policy, ZonedDateTime.parse("2026-09-29T23:00:00+03:00")))
    }
    @Test fun limitBlocksAtBoundaryAndFailsClosedWhenUsageMissing() {
        val p = AppPolicy(dailyLimitMinutes = 30)
        val now = ZonedDateTime.parse("2026-09-28T12:00:00Z")
        assertNull(PolicyRules.reason(p, now, 29 * 60_000L))
        assertNotNull(PolicyRules.reason(p, now, 30 * 60_000L))
        assertNotNull(PolicyRules.reason(p, now, null))
    }
    @Test fun policyAndUiSurviveConfigurationRoundTrip() {
        val original = LauncherData(settings = Preferences(favoriteCount = 0, ui = UiPreferences(appListEnabled = false, searchPosition = "Bottom", alphabetAnimation = "None", tripleTapAction = "lock", folderFill = true, folderLayout = "Freeform")),
            policies = mapOf("org.example" to AppPolicy(hidden = true, dailyLimitMinutes = 20, notifications = NotificationMode.DIGEST)))
        assertEquals(original, ConfigurationCodec.decode(ConfigurationCodec.encode(original)))
    }
    @Test fun activeActivitiesWithinOnePackageDoNotDoubleCount() {
        val events = listOf(AppUsageEvent(0,"pkg","A",true), AppUsageEvent(10,"pkg","B",true), AppUsageEvent(20,"pkg","A",false), AppUsageEvent(30,"pkg","B",false))
        assertEquals(30L, AppUsageMath.totals(events,0,100)["pkg"])
    }
    @Test fun usageClipsMidnightAndScreenOff() {
        val events = listOf(AppUsageEvent(0,"pkg","A",true), AppUsageEvent(90,"","",false,true))
        assertEquals(40L, AppUsageMath.totals(events,50,100)["pkg"])
    }
    @Test fun freeformPlacementPreservesGapsAndAllIds() {
        val a = AppFolder("a","A","star",emptyList(),gridCell=4)
        val b = AppFolder("b","B","heart",emptyList())
        val cells = FolderPlacement.cells(listOf(a,b))
        assertEquals(a,cells[4]); assertEquals(b,cells[0]); assertFalse(cells.containsKey(2))
    }
    @Test fun passwordsAreSaltedAndVerified() {
        val first = FolderPassword.create("correct horse")
        val second = FolderPassword.create("correct horse")
        assertNotEquals(first.first,second.first)
        assertTrue(FolderPassword.matches("correct horse",first.first,first.second))
        assertFalse(FolderPassword.matches("wrong",first.first,first.second))
    }
}
