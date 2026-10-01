package com.emmanuela.launcher

import com.emmanuela.launcher.data.AppMetadata
import com.emmanuela.launcher.data.AppNaming
import com.emmanuela.launcher.data.AppSearch
import com.emmanuela.launcher.data.LaunchableApp


import org.junit.Assert.*
import org.junit.Test

class AppMetadataTest {
    private val chat = LaunchableApp("org.chat/.Main", "Signal", "org.chat")
    private val browser = LaunchableApp("org.web/.Main", "Firefox", "org.web")
    @Test fun aliasesChangeSortingWithoutChangingLaunchIdentity() {
        val apps = AppNaming.decorate(listOf(chat, browser), mapOf(chat.id to AppMetadata("A chat")))
        assertEquals(chat.id, apps.first().id)
        assertEquals("A chat", apps.first().label)
        assertEquals("Signal", apps.first().originalLabel)
        assertEquals("org.chat", apps.first().packageName)
    }
    @Test fun emptyAliasRestoresOriginalLabel() {
        val aliased = chat.copy(label = "Messages")
        assertEquals("Signal", AppNaming.decorate(listOf(aliased), emptyMap()).single().label)
    }
    @Test fun tagsAreCanonicalAndDeduplicated() {
        assertEquals(listOf("social-media", "work"), AppNaming.tags("#Work, social-media WORK"))
        assertEquals(listOf("muncă"), AppNaming.tags("MUNCĂ"))
    }
    @Test fun searchMatchesAliasOriginalAndPackage() {
        val apps = AppNaming.decorate(listOf(chat), mapOf(chat.id to AppMetadata("Messages")))
        for (query in listOf("mess", "SIGnal", "org.chat")) assertEquals(apps, AppSearch.filter(apps, query))
    }
    @Test fun tagModeUsesPrefixesAndIntersectsOnlyTags() {
        val apps = AppNaming.decorate(listOf(chat, browser), mapOf(
            chat.id to AppMetadata("Messages", listOf("social-media", "work")),
            browser.id to AppMetadata(tags = listOf("work"))
        ))
        assertEquals(listOf(chat.id), AppSearch.filter(apps, "#SOCIAL-MEDIA #work ").map { it.id })
        assertEquals(1, AppSearch.filter(apps, "#social").size)
        assertEquals(2, AppSearch.filter(apps, "#").size)
        assertTrue(AppSearch.filter(apps, "social-media").isEmpty()) // Tags are opt-in via #.
    }
    @Test fun duplicateAliasesKeepBothDistinctEntries() {
        val apps = AppNaming.decorate(listOf(chat, browser), mapOf(chat.id to AppMetadata("Read"), browser.id to AppMetadata("Read")))
        assertEquals(2, apps.size)
        assertEquals(setOf(chat.id, browser.id), apps.map { it.id }.toSet())
    }
    @Test(expected = IllegalArgumentException::class)
    fun invalidTagIsRejected() { AppNaming.tags("social/media") }
    @Test(expected = IllegalArgumentException::class)
    fun controlCharactersInAliasAreRejected() { AppNaming.alias("one\ntwo") }
}
