package com.emmanuela.launcher

import com.emmanuela.launcher.data.*
import org.junit.Assert.*
import org.junit.Test
import java.time.ZonedDateTime

class V26RulesTest {
    private fun at(value:String)=ZonedDateTime.parse(value)
    @Test fun overnightWindowUsesPreviousDay(){
        val window=FocusWindow(setOf(1),1320,420,"Strict block")
        assertTrue(FocusWindows.active(window,at("2026-09-29T06:59:00Z")))
        assertFalse(FocusWindows.active(window,at("2026-09-29T07:00:00Z")))
        assertFalse(FocusWindows.active(window,at("2026-09-30T06:00:00Z")))
        assertEquals(3_600_000L,FocusWindows.nextBoundary(FocusGroup(windows=listOf(window)),at("2026-09-29T06:00:00Z")))
    }
    @Test fun strictBlockWinsAndLimitsDoNotLeakIntoBreaks(){
        val group=FocusGroup(name="Focus",dailyMinutes=30,windows=listOf(FocusWindow(start=540,end=1020),FocusWindow(start=600,end=660,mode="Strict block")))
        assertEquals("Focus: strict block",FocusWindows.reason(group,at("2026-10-01T10:30:00Z"),0,0))
        assertNull(FocusWindows.reason(group,at("2026-10-01T18:00:00Z"),1_800_000,100))
        assertEquals("Focus: daily limit reached",FocusWindows.reason(group,at("2026-10-01T12:00:00Z"),1_800_000,0))
    }
    @Test fun hybridSessionAndOpenBudgetsAreIndependent(){
        val group=FocusGroup(name="Focus",dailyMinutes=30,maxOpens=6,sessionMinutes=5)
        val now=at("2026-10-01T12:00:00Z")
        assertNull(FocusWindows.reason(group,now,299_999,5,299_999))
        assertEquals("Focus: session limit reached",FocusWindows.reason(group,now,300_000,5,300_000))
        assertEquals("Focus: daily opens reached",FocusWindows.reason(group,now,300_000,6,0))
        assertEquals("Enable Usage Access for daily limits",FocusWindows.reason(group,now,null,0))
    }
    @Test fun domainBlockingDoesNotMatchUnrelatedHosts(){
        val group=FocusGroup(websites=setOf("example.com"),keywords=setOf("restricted"))
        assertTrue(FocusWindows.matches(group,"https://m.example.com/path"))
        assertFalse(FocusWindows.matches(group,"https://example.com.evil.org"))
        assertFalse(FocusWindows.matches(group,"https://notexample.com"))
        assertTrue(FocusWindows.matches(group,"https://other.org/restricted"))
    }
    @Test fun allDayAndPerAppCounts(){
        val group=FocusGroup(id="group",windows=listOf(FocusWindow(start=0,end=0,mode="Strict block")))
        assertEquals(43_200_000L,FocusWindows.nextBoundary(group,at("2026-10-01T12:00:00Z")))
        assertTrue(FocusWindows.strict(group,at("2026-10-01T12:00:00Z")))
        assertEquals("2026-10-01:group",FocusWindows.countKey(group,"app","2026-10-01"))
        assertEquals("2026-10-01:group:app",FocusWindows.countKey(group.copy(perApp=true),"app","2026-10-01"))
    }
    @Test fun denseGridSpansNeverOverlapAndMovedFolderWins(){
        val folders=listOf(AppFolder("a","A","star",emptyList(),gridCell=0,widthUnits=4,heightUnits=2),AppFolder("b","B","star",emptyList(),gridCell=0,widthUnits=2,heightUnits=4),AppFolder("c","C","star",emptyList(),gridCell=1))
        val cells=FolderGridLayout.pack(folders,6,2,2,"b")
        assertEquals(0,cells.first{it.folder.id=="b"}.column)
        assertEquals(0,cells.first{it.folder.id=="b"}.row)
        val occupied=mutableSetOf<Pair<Int,Int>>()
        cells.forEach{c->assertTrue(c.column+c.width<=6);for(x in c.column until c.column+c.width)for(y in c.row until c.row+c.height)assertTrue(occupied.add(x to y))}
    }
    @Test fun newPreferencesAndDeviceOnlyFoldersRoundTrip(){
        val state=LauncherData(settings=Preferences(ui=UiPreferences(v2=V2Preferences(treeBranch=true,backgroundRules=true,clockSeconds=true,clockAmPm=true,clockFormat="Hands",folderWidthUnits=4,folderHeightUnits=1))),folders=listOf(AppFolder("private","Private","star",emptyList(),deviceProtected=true,widthUnits=3,heightUnits=4)))
        assertEquals(state,ConfigurationCodec.decode(ConfigurationCodec.encode(state)))
        assertTrue(state.folders.single().isProtected)
    }
    @Test fun rulesRoundTripWithoutDiscardingOlderFields(){
        val group=FocusGroup(name="Focus",packages=setOf("app"),windows=listOf(FocusWindow()),websites=setOf("example.com"),browsers=setOf("com.android.chrome"),perApp=true,cooldownSeconds=120)
        assertEquals(group,FocusCodec.decode(FocusCodec.encode(listOf(group))).single())
    }
}
