package com.emmanuela.launcher

import com.emmanuela.launcher.data.FocusCodec
import com.emmanuela.launcher.data.FocusGroup
import com.emmanuela.launcher.platform.AppUsageEvent
import com.emmanuela.launcher.platform.SessionMath
import org.junit.Assert.*
import org.junit.Test

class FocusSessionTest {
    private val packages=setOf("social.a","social.b")
    @Test fun switchingWithinGroupPreservesSessionButRestResetsIt(){
        val rows=listOf(
            AppUsageEvent(100_000,"social.a","Main",true),
            AppUsageEvent(130_000,"social.a","Main",false),
            AppUsageEvent(130_010,"social.b","Main",true),
            AppUsageEvent(160_000,"social.b","Main",false))
        val session=SessionMath.recent(rows,packages,170_000)
        assertEquals(59_990L,session.milliseconds)
        assertEquals(160_000L,session.lastActiveAt)
        assertFalse(session.active)
        val restarted=SessionMath.recent(rows+AppUsageEvent(230_000,"social.a","Main",true),packages,240_000)
        assertEquals(10_000L,restarted.milliseconds)
        assertTrue(restarted.active)
    }
    @Test fun multipleActivitiesDoNotDoubleCount(){
        val rows=listOf(AppUsageEvent(100_000,"social.a","Main",true),AppUsageEvent(110_000,"social.a","Dialog",true),AppUsageEvent(120_000,"social.a","Main",false))
        assertEquals(30_000L,SessionMath.recent(rows,packages,130_000).milliseconds)
    }
    @Test fun screenOffEndsActiveSession(){
        val rows=listOf(AppUsageEvent(100_000,"social.a","Main",true),AppUsageEvent(130_000,"","",false,true))
        assertEquals(30_000L,SessionMath.recent(rows,packages,200_000).milliseconds)
        assertFalse(SessionMath.recent(rows,packages,200_000).active)
    }
    @Test fun escalationIsBoundedAndDisabledPauseStaysDisabled(){
        val group=FocusGroup(name="Social",packages=packages,pauseSeconds=10,escalatingPause=true)
        assertEquals(10,FocusCodec.pauseDelay(group,0))
        assertEquals(20,FocusCodec.pauseDelay(group,3))
        assertEquals(60,FocusCodec.pauseDelay(group,1000))
        assertEquals(0,FocusCodec.pauseDelay(group.copy(pause=false),1000))
    }
    @Test fun extendedRulesSurviveBackup(){
        val group=FocusGroup(name="Social",packages=packages,sessionMinutes=15,escalatingPause=true,requireAuthentication=false)
        assertEquals(listOf(group),FocusCodec.decode(FocusCodec.encode(listOf(group))))
    }
}
