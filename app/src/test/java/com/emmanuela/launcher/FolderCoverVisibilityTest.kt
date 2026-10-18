package com.emmanuela.launcher

import com.emmanuela.launcher.data.AppFolder
import com.emmanuela.launcher.data.ConfigurationCodec
import com.emmanuela.launcher.data.FolderCodec
import com.emmanuela.launcher.data.LauncherData
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class FolderCoverVisibilityTest {
    @Test fun existingFolderConfigurationsKeepTheirVisibleIconAndName() {
        val old = JSONObject(FolderCodec.encode(listOf(AppFolder("old", "Existing", "work", emptyList(),
            imageUri = "content://photos/existing"))))
        old.getJSONArray("folders").getJSONObject(0).apply {
            remove("showCoverIcon")
            remove("showCoverName")
        }
        val restored = FolderCodec.decode(old.toString()).single()
        assertTrue(restored.showCoverIcon)
        assertTrue(restored.showCoverName)
        assertEquals("Existing", restored.name)
        assertEquals("work", restored.icon)
    }

    @Test fun configurationRoundTripKeepsIndependentCoverChoicesAndFolderIdentity() {
        val picture = AppFolder("photo", "Holiday", "sun", listOf("photos/.Main", "maps/.Main"),
            imageUri = "content://photos/holiday", manualOrder = true, deviceProtected = true,
            showCoverIcon = false, showCoverName = false)
        val folders = listOf(picture,
            AppFolder("icon", "Work", "work", emptyList(), imageUri = "content://photos/work", showCoverName = false),
            AppFolder("name", "Music", "music", emptyList(), imageUri = "content://photos/music", showCoverIcon = false),
            AppFolder("default", "Default", "folder", emptyList()))
        val restored = ConfigurationCodec.decode(ConfigurationCodec.encode(LauncherData(folders = folders)))
        assertEquals(folders, restored.folders)
        assertTrue(restored.folders.first().isProtected)
        assertEquals(picture.apps, restored.folders.first().apps)
        assertEquals("Holiday", restored.folders.first().name)
        assertEquals("sun", restored.folders.first().icon)
    }
}
