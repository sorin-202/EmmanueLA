package com.emmanuela.launcher

import com.emmanuela.launcher.platform.ScreenEvent
import com.emmanuela.launcher.platform.ScreenTime


import org.junit.Assert.assertEquals
import org.junit.Test
class ScreenTimeTest {
    @Test fun clipsSessionAcrossMidnight() {
        assertEquals(50L, ScreenTime.duration(listOf(ScreenEvent(10, ScreenTime.ON), ScreenEvent(20, ScreenTime.UNLOCK), ScreenEvent(150, ScreenTime.OFF)), 100, 200))
    }
    @Test fun excludesLockScreenAndAddsOpenSession() {
        val events = listOf(ScreenEvent(10, ScreenTime.ON), ScreenEvent(20, ScreenTime.UNLOCK), ScreenEvent(40, ScreenTime.LOCK), ScreenEvent(50, ScreenTime.UNLOCK))
        assertEquals(70L, ScreenTime.duration(events, 0, 100))
    }
    @Test fun duplicateEventsDoNotDoubleCount() {
        val events = listOf(ScreenEvent(10, ScreenTime.UNLOCK), ScreenEvent(20, ScreenTime.UNLOCK), ScreenEvent(70, ScreenTime.OFF))
        assertEquals(60L, ScreenTime.duration(events, 0, 100))
    }
    @Test fun noHistoryDoesNotInventUsage() { assertEquals(0L, ScreenTime.duration(emptyList(), 0, 100)) }
}
