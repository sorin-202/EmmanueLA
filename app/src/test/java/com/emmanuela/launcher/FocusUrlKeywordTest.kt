package com.emmanuela.launcher

import com.emmanuela.launcher.data.FocusGroup
import com.emmanuela.launcher.data.FocusWindows
import org.junit.Assert.*
import org.junit.Test

class FocusUrlKeywordTest {
    private fun matches(keyword: String, url: String) =
        FocusWindows.matches(FocusGroup(keywords = setOf(keyword)), url)

    @Test fun percentEncodedPathPreservesLiteralPlus() {
        assertTrue(matches("foo+bar", "https://example.com/%66oo+bar"))
        assertTrue(matches("foo+bar", "https://example.com/foo%2Bbar"))
        assertFalse(matches("foo bar", "https://example.com/foo+bar"))
    }

    @Test fun queryFormSpacesDoNotChangePathOrFragmentSemantics() {
        assertTrue(matches("focus time", "https://example.com/search?q=focus+time"))
        assertTrue(matches("focus time", "https://example.com/search?q=focus%20time"))
        assertFalse(matches("focus time", "https://example.com/focus+time#chapter"))
        assertFalse(matches("focus time", "https://example.com/page#focus+time"))
    }

    @Test fun unicodeKeywordsMatchEquivalentEncodedAndDisplayForms() {
        assertTrue(matches("școală", "https://example.com/s%CC%A6coala%CC%86"))
        assertTrue(matches("S\u0326COALA\u0306", "https://example.com/%C8%99coal%C4%83"))
        assertTrue(matches("școală", "https://example.com/ȘCOALĂ"))
    }

    @Test fun keywordsDoNotMakeForeignOrNonWebUrlsEligible() {
        assertFalse(matches("shorts", "javascript://example.com/shorts"))
        assertFalse(matches("shorts", "https://user:password@example.com/shorts"))
        assertFalse(matches("shorts", "https://example.com/regular"))
        assertTrue(matches("shorts", "https://example.com/watch?mode=%73HORTS"))
    }

    @Test fun aYoutubeShortsPathRuleKeepsOrdinaryYoutubeAvailable() {
        val shorts = FocusGroup(websites = setOf("youtube.com/shorts"))
        assertTrue(FocusWindows.matches(shorts, "https://www.youtube.com/shorts/abc123"))
        assertTrue(FocusWindows.matches(shorts, "https://m.youtube.com/shorts/abc123?feature=share"))
        assertFalse(FocusWindows.matches(shorts, "https://www.youtube.com/"))
        assertFalse(FocusWindows.matches(shorts, "https://www.youtube.com/watch?v=abc123"))
        assertFalse(FocusWindows.matches(shorts, "https://www.youtube.com/results?search_query=shorts"))
        assertFalse(FocusWindows.matches(shorts, "https://www.youtube.com/shorts-music"))
        assertFalse(FocusWindows.matches(shorts, "https://youtube.com.attacker.test/shorts/abc123"))
    }
}
