package com.emmanuela.launcher

import com.emmanuela.launcher.data.*
import java.time.ZonedDateTime
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class FocusBreakTest {
    private val now=ZonedDateTime.parse("2026-10-05T12:00:00Z")
    private val started=now.toInstant().toEpochMilli()
    private val group=FocusGroup(id="focus",name="Focus",packages=setOf("app"),blockAlways=true,dailyMinutes=1)
    @Test fun continuousBlockingDoesNotDependOnProtectionToggle() {
        assertEquals("Focus: strict block",FocusWindows.reason(group,now,0,0))
        assertNull(FocusWindows.reason(group.copy(blockAlways=false,strict=true),now,0,0))
    }
    @Test fun temporaryBreakExpiresWithoutResettingAllowances() {
        val paused=group.copy(breakStartedAt=started,breakUntil=started+300_000)
        assertNull(FocusWindows.reason(paused,now,600_000,100))
        assertFalse(FocusWindows.limited(paused,now))
        assertEquals(300_000L,FocusWindows.nextBoundary(paused,now))
        assertEquals("Focus: strict block",FocusWindows.reason(paused,now.plusMinutes(5),600_000,100))
    }
    @Test fun strictScheduleWinsOverManualAndScheduledBreaks() {
        val paused=group.copy(breakStartedAt=started,breakUntil=started+300_000,
            windows=listOf(FocusWindow(setOf(1),0,0,"Break"),FocusWindow(setOf(1),600,780,"Strict block")))
        assertFalse(FocusWindows.onBreak(paused,now))
        assertTrue(FocusWindows.strict(paused,now))
        assertTrue(FocusWindows.onBreak(paused,now.plusHours(2)))
    }
    @Test fun clockRollbackDoesNotGrantFutureBreak() {
        val paused=group.copy(breakStartedAt=started,breakUntil=started+300_000)
        assertFalse(FocusWindows.onBreak(paused,now.minusSeconds(1)))
    }
    @Test fun configurationSurvivesRestartAndReadsOlderBackups() {
        val data=LauncherData(focusGroups=listOf(group.copy(breakStartedAt=started,breakUntil=started+300_000)))
        val encoded=ConfigurationCodec.encode(data)
        assertEquals(8,JSONObject(encoded).getInt("version"))
        assertEquals(data,ConfigurationCodec.decode(encoded))
        val old=JSONObject(encoded).put("version",7)
        val oldGroup=old.getJSONArray("focusGroups").getJSONObject(0)
        listOf("blockAlways","breakStartedAt","breakUntil").forEach(oldGroup::remove)
        val restored=ConfigurationCodec.decode(old.toString()).focusGroups.single()
        assertFalse(restored.blockAlways)
        assertEquals(0L,restored.breakUntil)
    }
    @Test(expected=IllegalArgumentException::class) fun rejectsUnboundedBreakImport() {
        FocusCodec.decode(FocusCodec.encode(listOf(group.copy(breakStartedAt=started,breakUntil=started+86_400_001))))
    }
}
