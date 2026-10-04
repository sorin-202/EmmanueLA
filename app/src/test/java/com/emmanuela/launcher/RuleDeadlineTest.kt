package com.emmanuela.launcher

import com.emmanuela.launcher.data.*
import java.time.ZonedDateTime
import org.junit.Assert.*
import org.junit.Test

class RuleDeadlineTest {
    @Test fun expiredLimitsNeverScheduleFastPolling() {
        assertEquals(60_000L, RuleDeadline.next(listOf(-100, 0, 60_000, 86_400_000)))
        assertNull(RuleDeadline.next(listOf(0, -1)))
    }
    @Test fun blockedOvernightWindowStillHasAnExpiry() {
        val group=FocusGroup(windows=listOf(FocusWindow(setOf(1), 1320, 420, "Strict block")))
        val now=ZonedDateTime.parse("2026-10-06T06:59:00+03:00[Europe/Bucharest]")
        assertTrue(FocusWindows.strict(group,now))
        assertEquals(60_000L,FocusWindows.nextBoundary(group,now))
        assertFalse(FocusWindows.strict(group,now.plusMinutes(1)))
    }
    @Test fun repeatedDstHourHasBothWindowBoundaries() {
        val group=FocusGroup(windows=listOf(FocusWindow(setOf(7), 210, 225, "Strict block")))
        val first=ZonedDateTime.parse("2026-10-25T03:46:00+03:00[Europe/Bucharest]")
        assertEquals(14*60_000L,FocusWindows.nextBoundary(group,first))
        val repeated=ZonedDateTime.parse("2026-10-25T03:10:00+02:00[Europe/Bucharest]")
        assertEquals(20*60_000L,FocusWindows.nextBoundary(group,repeated))
        assertTrue(FocusWindows.strict(group,repeated.plusMinutes(20)))
    }
    @Test fun springGapReevaluatesAtClockTransition() {
        val group=FocusGroup(windows=listOf(FocusWindow(setOf(7),210,270,"Strict block")))
        val now=ZonedDateTime.parse("2026-03-29T02:59:00+02:00[Europe/Bucharest]")
        assertFalse(FocusWindows.strict(group,now))
        assertEquals(60_000L,FocusWindows.nextBoundary(group,now))
        assertTrue(FocusWindows.strict(group,now.plusMinutes(1)))
    }
    @Test fun emptyScheduleHasNoUnnecessaryWakeup() {
        assertNull(FocusWindows.nextBoundary(FocusGroup(),ZonedDateTime.now()))
    }
}
