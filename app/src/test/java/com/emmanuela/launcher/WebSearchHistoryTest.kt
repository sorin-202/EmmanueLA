package com.emmanuela.launcher

import com.emmanuela.launcher.data.WebSearchHistory
import com.emmanuela.launcher.data.WebSearchEngine
import org.junit.Assert.*
import org.junit.Test

class WebSearchHistoryTest {
    @Test fun slashInputSubmitsOnlyNonemptyWebQueries() {
        assertEquals("celestial navigation", WebSearchHistory.fromInput("  /celestial navigation  "))
        assertEquals("moon landing", WebSearchHistory.fromInput("/ moon\t landing"))
        listOf("", "/", " / \n ", "navigation", "@navigation", "#navigation").forEach {
            assertNull(WebSearchHistory.fromInput(it))
        }
        assertNull(WebSearchHistory.fromInput("/" + "x".repeat(WebSearchHistory.MAX_QUERY_LENGTH + 1)))
    }

    @Test fun retainsExactlyTheFiveNewestDistinctQueries() {
        var history = emptyList<String>()
        (1..7).forEach { history = WebSearchHistory.record(history, "query $it") }
        assertEquals(listOf("query 7", "query 6", "query 5", "query 4", "query 3"), history)
        history = WebSearchHistory.record(history, " Query \t 5 ")
        assertEquals(listOf("Query 5", "query 7", "query 6", "query 4", "query 3"), history)
        assertEquals(history, WebSearchHistory.decode(WebSearchHistory.encode(history)))
    }

    @Test fun localStorageHandlesCorruptAndInvalidEntries() {
        listOf(null, "", "not json", "{}", "[true,null,{},42]").forEach { assertTrue(WebSearchHistory.decode(it).isEmpty()) }
        assertEquals(listOf("Stars", "Moon"), WebSearchHistory.decode("[\"Stars\",\"stars\",\" \",\"Moon\"]"))
        assertThrows(IllegalArgumentException::class.java) { WebSearchHistory.record(emptyList(), " ") }
        assertThrows(IllegalArgumentException::class.java) { WebSearchHistory.record(emptyList(), "x".repeat(WebSearchHistory.MAX_QUERY_LENGTH + 1)) }
        assertEquals(emptyList<String>(), WebSearchHistory.decode(WebSearchHistory.encode(emptyList())))
    }

    @Test fun onlyKnownSearchEnginesAreRestored() {
        WebSearchEngine.entries.forEach { assertEquals(it, WebSearchEngine.fromStored(it.name)) }
        listOf(null, "", "https://untrusted.invalid", "unknown").forEach {
            assertEquals(WebSearchEngine.ANDROID_DEFAULT, WebSearchEngine.fromStored(it))
        }
    }
}
