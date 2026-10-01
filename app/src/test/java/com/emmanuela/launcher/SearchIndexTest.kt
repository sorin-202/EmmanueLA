package com.emmanuela.launcher

import com.emmanuela.launcher.data.AppSearchIndex
import com.emmanuela.launcher.data.LaunchableApp


import org.junit.Assert.*
import org.junit.Test
class SearchIndexTest {
    private val apps = listOf(
        LaunchableApp("chat/.A","Messages","chat","Signal",listOf("social-media","work")),
        LaunchableApp("web/.A","Browser","web","Firefox",listOf("work")),
        LaunchableApp("clock/.A","Clock","clock"))
    @Test fun leadingHashSwitchesEntireQueryToTagsAndOffersPrefixes() {
        val index = AppSearchIndex(apps)
        assertEquals(listOf("chat/.A"),index.search("#soc").apps.map { it.id })
        assertEquals(2,index.search("#").apps.size)
        assertTrue(index.search("#work mess").apps.isEmpty())
        assertEquals(listOf("chat/.A"),index.search("#work social-media ").apps.map { it.id })
        assertEquals(listOf("social-media"),index.suggestions("#soc"))
    }
    @Test fun indexOnlyContainsVisibleResultSections() {
        val index = AppSearchIndex(apps)
        assertEquals(listOf("B","C","M"),index.all.sections.map { it.label })
        assertEquals(listOf("M"),index.search("signal").sections.map { it.label })
        assertTrue(index.search("nonexistent").sections.isEmpty())
    }
    @Test fun disabledTagModeDoesNotMatchMetadata() { assertTrue(AppSearchIndex(apps).search("#work",false).apps.isEmpty()) }
}
