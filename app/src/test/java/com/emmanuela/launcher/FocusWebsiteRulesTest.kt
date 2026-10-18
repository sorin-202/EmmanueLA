package com.emmanuela.launcher

import com.emmanuela.launcher.data.*
import org.junit.Assert.*
import org.junit.Test

class FocusWebsiteRulesTest {
    private val supported = setOf("org.mozilla.firefox", "com.android.chrome")

    @Test fun addingAnAppCannotBypassBrowserValidationForWebTargets() {
        val mixed = FocusGroup(packages = setOf("video.app"), websites = setOf("youtube.com/shorts"))
        assertNotNull(FocusWebsiteRules.configurationError(mixed, supported))
        assertNotNull(FocusWebsiteRules.configurationError(mixed.copy(browsers = setOf("unsupported")), supported))
        assertNull(FocusWebsiteRules.configurationError(mixed.copy(browsers = setOf("org.mozilla.firefox")), supported))
        assertNull(FocusWebsiteRules.configurationError(mixed.copy(websites = emptySet()), supported))
    }

    @Test fun keywordOnlyRulesAlsoRequireASupportedBrowser() {
        val group = FocusGroup(keywords = setOf("/shorts/"))
        assertTrue(FocusWebsiteRules.hasWebTargets(group))
        assertNotNull(FocusWebsiteRules.configurationError(group, supported))
        assertNull(FocusWebsiteRules.configurationError(group.copy(browsers = supported), supported))
        assertTrue(FocusWebsiteRules.needsUrlDetails(group))
        assertTrue(FocusWebsiteRules.needsUrlDetails(FocusGroup(websites = setOf("youtube.com/shorts"))))
        assertFalse(FocusWebsiteRules.needsUrlDetails(FocusGroup(websites = setOf("youtube.com"))))
    }

    @Test fun siteCanonicalizationPreservesItsPathInsteadOfBlockingTheWholeDomain() {
        assertEquals("youtube.com/shorts", FocusWebsiteRules.site(" HTTPS://YouTube.COM:443/shorts/ "))
        assertEquals("youtube.com", FocusWebsiteRules.site("https://youtube.com/"))
        assertEquals("xn--bcher-kva.de/books", FocusWebsiteRules.site("https://bücher.de/books"))
        listOf("", "javascript://youtube.com/shorts", "https://user:password@youtube.com/shorts",
            "youtube.com/results?search_query=shorts", "youtube.com/watch#shorts", "*.youtube.com",
            "youtube.com/has space").forEach { assertNull(it, FocusWebsiteRules.site(it)) }
    }

    @Test fun normalizedTargetsPersistWithoutLosingTheShortsPathOrOtherRuleSettings() {
        val group = FocusGroup(name = "Shorts", websites = setOf("HTTPS://YouTube.COM/shorts/"),
            keywords = setOf(" /shorts/ "), browsers = setOf("org.mozilla.firefox"), blockAlways = true)
        val normalized = FocusWebsiteRules.normalized(group)
        val restored = FocusCodec.decode(FocusCodec.encode(listOf(normalized))).single()
        assertEquals(setOf("youtube.com/shorts"), restored.websites)
        assertEquals(setOf("/shorts/"), restored.keywords)
        assertEquals(group.copy(websites = restored.websites, keywords = restored.keywords), restored)
    }

    @Test fun pathsUseSegmentBoundariesAndDomainBoundaries() {
        val sites = setOf("youtube.com/shorts")
        listOf("https://youtube.com/shorts", "https://www.youtube.com/shorts/id", "https://m.youtube.com/%73horts/id")
            .forEach { assertTrue(it, FocusWebsiteRules.matchesAnySite(sites, it)) }
        listOf("https://youtube.com/watch?v=id", "https://youtube.com/shorts-music", "https://youtube.com/Shorts/id",
            "https://youtube.com/watch?next=/shorts/id", "https://notyoutube.com/shorts/id", "https://youtube.com.evil.test/shorts/id")
            .forEach { assertFalse(it, FocusWebsiteRules.matchesAnySite(sites, it)) }
        assertTrue(FocusWebsiteRules.matchesAnySite(setOf("youtube.com"), "https://www.youtube.com/watch?v=id"))
    }

    @Test fun invalidOrEmptyKeywordsAreRejectedWithoutDiscardingOtherTargets() {
        assertNull(FocusWebsiteRules.keyword("   "))
        assertNull(FocusWebsiteRules.keyword("video\nshorts"))
        assertEquals("focus time", FocusWebsiteRules.keyword(" focus time "))
        assertNotNull(FocusWebsiteRules.configurationError(FocusGroup(packages = setOf("app"), keywords = setOf(" ")), supported))
    }
}
