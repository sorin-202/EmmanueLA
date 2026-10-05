package com.emmanuela.launcher

import com.emmanuela.launcher.data.*
import java.time.ZonedDateTime
import org.junit.Assert.*
import org.junit.Test

class FocusAllowanceTest {
    private val now=ZonedDateTime.parse("2026-10-05T12:00:00Z")
    private val group=FocusGroup(name="Focus",packages=setOf("app"),dailyMinutes=10,sessionMinutes=5,graceMinutes=2,warningMinutes=1)
    @Test fun missingUsageRowIsNotMissingPermission() {
        val policy=AppPolicy(dailyLimitMinutes=10)
        assertNull(PolicyRules.reason(policy,now,emptyMap(),"new.app"))
        assertNotNull(PolicyRules.reason(policy,now,null,"new.app"))
        assertNotNull(PolicyRules.reason(policy,now,mapOf("new.app" to 600_000L),"new.app"))
    }
    @Test fun graceExtendsTimeButNotOpenCount() {
        assertNull(FocusWindows.reason(group,now,719_999,0,419_999))
        assertEquals("Focus: daily limit reached",FocusWindows.reason(group,now,720_000,0))
        assertEquals("Focus: session limit reached",FocusWindows.reason(group,now,0,0,420_000))
        assertEquals("Focus: daily opens reached",FocusWindows.reason(group.copy(maxOpens=1),now,0,1))
    }
    @Test fun warningLeadTimeDoesNotBlock() {
        assertNull(FocusWindows.warning(group,now,539_999,0))
        assertEquals("Focus: daily limit approaching",FocusWindows.warning(group,now,540_000,0))
        assertNull(FocusWindows.reason(group,now,540_000,0))
        assertEquals("Focus: session limit approaching",FocusWindows.warning(group,now,0,0,240_000))
    }
    @Test fun warnActionAllowsContinuedUseButNeverOverridesStrictBlock() {
        val warn=group.copy(limitAction="Warn")
        assertNull(FocusWindows.reason(warn,now,900_000,100,900_000))
        assertEquals("Focus: daily limit reached",FocusWindows.warning(warn,now,900_000,100))
        assertEquals("Focus: strict block",FocusWindows.reason(warn.copy(blockAlways=true),now,null,0))
        assertNull(FocusWindows.warning(warn.copy(blockAlways=true),now,900_000,0))
    }
    @Test fun warningOnlyDegradesWithoutUsagePermission() {
        val warn=group.copy(limitAction="Warn")
        assertNull(FocusWindows.reason(warn,now,null,0))
        assertEquals("Focus: enable Usage Access for warnings",FocusWindows.warning(warn,now,null,0))
        assertNotNull(FocusWindows.reason(group,now,null,0))
    }
    @Test fun breakSuppressesWarningsAndPortableBackupDropsTemporaryGrant() {
        val epoch=now.toInstant().toEpochMilli()
        val paused=group.copy(breakStartedAt=epoch,breakUntil=epoch+60_000)
        assertNull(FocusWindows.warning(paused,now,900_000,0))
        val exported=ConfigurationCodec.portable(LauncherData(focusGroups=listOf(paused)))
        assertEquals(0L,exported.focusGroups.single().breakUntil)
    }
    @Test fun allowanceAndIntentionChoicesSurviveRestart() {
        val configured=group.copy(limitAction="Warn",requireIntention=true)
        assertEquals(configured,FocusCodec.decode(FocusCodec.encode(listOf(configured))).single())
    }
}
