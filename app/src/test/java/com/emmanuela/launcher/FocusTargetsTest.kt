package com.emmanuela.launcher

import com.emmanuela.launcher.data.*
import org.junit.Assert.*
import org.junit.Test
import java.time.ZonedDateTime

class FocusTargetsTest {
    private val now=ZonedDateTime.parse("2026-10-05T12:00:00Z")
    private val group=FocusGroup(id="focus",name="Focus",packages=setOf("social"),browsers=setOf("browser"),websites=setOf("example.com"),dailyMinutes=1,pause=false)

    @Test fun aMixedAppAndWebsiteGroupCountsCurrentBrowserAndGroupAppsOnce(){
        val usage=mapOf("social" to 25_000L,"browser" to 40_000L,"unrelated" to 900_000L)
        assertEquals(setOf("social","browser"),FocusTargets.usagePackages(group,"browser"))
        assertEquals(65_000L,FocusTargets.used(group,"browser",usage))
        assertEquals("Focus: daily limit reached",FocusWindows.reason(group,now,FocusTargets.used(group,"browser",usage),0))
        assertEquals(25_000L,FocusTargets.used(group,"social",usage))
        assertEquals(40_000L,FocusTargets.used(group.copy(perApp=true),"browser",usage))
        assertNull(FocusTargets.used(group,"browser",null))
    }

    @Test fun browserRulesNeedBothSelectedBrowserAndMatchingNavigatedAddress(){
        assertEquals(listOf(group),FocusTargets.groups(listOf(group),"social",""))
        assertEquals(listOf(group),FocusTargets.groups(listOf(group),"browser","https://sub.example.com/a"))
        assertTrue(FocusTargets.groups(listOf(group),"otherBrowser","https://example.com").isEmpty())
        listOf("", "https://notexample.com", "https://example.com.attacker.test", "javascript://example.com", "https://example.com@attacker.test").forEach {
            assertTrue("Must not match $it",FocusTargets.groups(listOf(group),"browser",it).isEmpty())
        }
    }

    @Test fun activeRuleChangeBreakAndScheduleAreEvaluatedForAnAlreadyOpenApp(){
        val strict=group.copy(blockAlways=true,breakStartedAt=now.toInstant().toEpochMilli(),breakUntil=now.plusMinutes(5).toInstant().toEpochMilli())
        assertNull(FocusWindows.reason(strict,now,100_000L,0))
        assertEquals("Focus: strict block",FocusWindows.reason(strict,now.plusMinutes(5),100_000L,0))
        val scheduled=strict.copy(windows=listOf(FocusWindow(setOf(1),720,730,"Strict block")))
        assertEquals("Focus: strict block",FocusWindows.reason(scheduled,now,0L,0))
        assertEquals(300_000L,FocusWindows.nextBoundary(strict,now))
    }

    @Test fun urlKeywordsMatchEncodedNavigationOnlyInConfiguredBrowser(){
        val keyword=group.copy(websites=emptySet(),keywords=setOf("shorts"))
        assertEquals(listOf(keyword),FocusTargets.groups(listOf(keyword),"browser","https://video.example/watch/%73horts"))
        assertTrue(FocusTargets.groups(listOf(keyword),"browser","https://video.example/watch").isEmpty())
    }

    @Test fun normalizesDomainCaseTrailingDotUnicodeAndRejectsExecutableOrCredentialUrls(){
        assertEquals("example.com",FocusWindows.host("HTTPS://EXAMPLE.COM.:443/a"))
        assertEquals("xn--bcher-kva.de",FocusWindows.host("https://bücher.de/path"))
        listOf("https://user:pass@example.com", "javascript://example.com", "file://example.com", "https://example.com\\@evil.com", "*.example.com", "https://example.com/has space", "https://example.com:not-a-port", "https://example.com:65536").forEach {
            assertNull(it,FocusWindows.host(it))
        }
    }
}
