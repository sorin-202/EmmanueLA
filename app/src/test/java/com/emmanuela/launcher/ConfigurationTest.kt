package com.emmanuela.launcher

import com.emmanuela.launcher.data.AppFolder
import com.emmanuela.launcher.data.AppMetadata
import com.emmanuela.launcher.data.ConfigurationCodec
import com.emmanuela.launcher.data.Favorite
import com.emmanuela.launcher.data.LauncherData
import com.emmanuela.launcher.data.Preferences


import org.junit.Assert.*
import org.junit.Test

class ConfigurationTest {
    @Test fun roundTripAllSettingsFavoritesAndFolders() {
        val original = LauncherData(
            Preferences(theme = "Light", bold = true, textScale = 1.3f, font = "Serif", notificationBar = false,
                showClock = false, showDate = false, showBattery = false, showScreenTime = true, alignment = "Left", favoriteCount = 8,
                autoKeyboard = true, wallpapers = listOf("content://photos/1"), dailyWallpaper = true, dim = .6f,
                swipeLeft = "app:a.b/.Camera", swipeRight = "folders", swipeDown = "settings", doubleTapAction = "lock",
                tileSize = 200, symbolSize = 36, symbolPlacement = "Left", searchAccent = true, note = "Salut! 音楽"),
            listOf(Favorite("a.b/.Camera")), listOf(AppFolder("one", "Work", "star", listOf("a.b/.Camera"), 0xFFE8A9A9)),
            mapOf("a.b/.Camera" to AppMetadata("My camera", listOf("photo", "work"))))
        assertEquals(original, ConfigurationCodec.decode(ConfigurationCodec.encode(original)))
    }
    @Test fun migratesV2FavoriteLabelsToGlobalAliases() {
        val root = org.json.JSONObject(ConfigurationCodec.encode(LauncherData(favorites = listOf(Favorite("a/.Main")))))
        root.put("version", 2)
        root.remove("appMetadata")
        root.getJSONObject("settings").put("doubleTap", true)
        root.getJSONArray("favorites").getJSONObject(0).put("label", "Messages")
        val migrated = ConfigurationCodec.decode(root.toString())
        assertEquals("Messages", migrated.appMetadata.getValue("a/.Main").alias)
        assertEquals("lock", migrated.settings.doubleTapAction)
        assertEquals(migrated, ConfigurationCodec.decode(ConfigurationCodec.encode(migrated)))
    }
    @Test fun metadataSurvivesPortableBackupEvenForMissingApps() {
        val metadata = mapOf("gone.app/.Main" to AppMetadata("Chat", listOf("social")))
        val original = LauncherData(appMetadata = metadata)
        assertEquals(metadata, ConfigurationCodec.decode(ConfigurationCodec.encode(ConfigurationCodec.portable(original))).appMetadata)
    }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsInvalidStoredTagBeforeWriting() {
        ConfigurationCodec.decode(ConfigurationCodec.encode(LauncherData(appMetadata = mapOf("a/.Main" to AppMetadata(tags = listOf("bad tag"))))))
    }
    @Test fun portableBackupDropsDeviceBoundMediaOnly() {
        val data = LauncherData(Preferences(font = "Custom", customFont = "123", wallpapers = listOf("content://photo"), dailyWallpaper = true, note = "keep"))
        val portable = ConfigurationCodec.portable(data)
        assertEquals("Sans", portable.settings.font)
        assertTrue(portable.settings.wallpapers.isEmpty())
        assertFalse(portable.settings.dailyWallpaper)
        assertEquals("keep", portable.settings.note)
    }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsOutOfRangeTextSize() {
        ConfigurationCodec.decode(ConfigurationCodec.encode(LauncherData(Preferences(textScale = 100f))))
    }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnknownActions() {
        ConfigurationCodec.decode(ConfigurationCodec.encode(LauncherData(Preferences(swipeLeft = "arbitrary"))))
    }
    @Test(expected = IllegalArgumentException::class)
    fun rejectsDuplicateFavorites() {
        ConfigurationCodec.decode(ConfigurationCodec.encode(LauncherData(favorites = listOf(Favorite("a/.A"), Favorite("a/.A")))))
    }
}
