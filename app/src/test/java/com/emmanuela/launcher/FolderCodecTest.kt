package com.emmanuela.launcher

import com.emmanuela.launcher.data.AppFolder
import com.emmanuela.launcher.data.FolderCodec


import org.junit.Assert.*
import org.junit.Test

class FolderCodecTest {
    @Test fun roundTripKeepsNamesIconsAndActivityIdentities() {
        val folders = listOf(
            AppFolder("one", "Muncă & \"Study\"", "work", listOf("a.b/.Main", "a.b/.Other")),
            AppFolder("two", "音楽", "music", emptyList())
        )
        assertEquals(folders, FolderCodec.decode(FolderCodec.encode(folders)))
    }
    @Test fun absentPreferencesStartEmpty() { assertEquals(emptyList<AppFolder>(), FolderCodec.decode(null)) }
    @Test fun duplicateMembershipIsNormalized() {
        val folder = AppFolder("id", "Work", "work", listOf("a/.Main", "a/.Main"))
        assertEquals(listOf("a/.Main"), FolderCodec.decode(FolderCodec.encode(listOf(folder))).single().apps)
    }
    @Test(expected = IllegalArgumentException::class)
    fun futureSchemaIsRejectedInsteadOfDiscarded() { FolderCodec.decode("""{"version":2,"folders":[]}""") }
    @Test(expected = org.json.JSONException::class)
    fun corruptDataIsNotSilentlyReset() { FolderCodec.decode("broken") }
    @Test(expected = IllegalArgumentException::class)
    fun duplicateFolderIdsAreRejected() {
        val f = AppFolder("id", "Same", "folder", emptyList())
        FolderCodec.decode(FolderCodec.encode(listOf(f, f)))
    }
}
