package com.emmanuela.launcher

import com.emmanuela.launcher.data.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class HomeModesTest {
    @Test fun olderTextAndAlphabetHomesRetainTheirLayout() {
        assertEquals(HomeModes.MINIMALIST, HomeModes.effective(ExperienceCodec.decode(JSONObject())))
        val oldAlphabet = ExperienceCodec.decode(JSONObject().put("homeAlphabet", true))
        assertNull(oldAlphabet.homeMode)
        assertEquals(HomeModes.ALPHABET, HomeModes.effective(oldAlphabet))
        assertEquals(HomeModes.ALPHABET, HomeModes.effective(ExperiencePreferences().copy(homeAlphabet = true)))
        assertFalse(ExperienceCodec.encode(oldAlphabet).has("homeMode"))
    }

    @Test fun eachExplicitModePersistsWithFavoritesShortcutsAndWidgetPositions() {
        val original = LauncherData(favorites = listOf(Favorite("com.example/.Main")),
            settings = Preferences(ui = UiPreferences(v2 = V2Preferences(
                homeShortcuts = listOf(HomeShortcut("com.example/.Other", WidgetPlacement(18f, 200f))),
                widgetPositions = mapOf("clock" to WidgetPlacement(40f, 20f))))))
        HomeModes.values.forEach { mode ->
            val data = original.copy(settings = original.settings.copy(ui = original.settings.ui.copy(
                experience = HomeModes.select(original.settings.ui.experience, mode))))
            val restored = ConfigurationCodec.decode(ConfigurationCodec.encode(data))
            assertEquals(data, restored)
            assertEquals(mode, HomeModes.effective(restored.settings.ui.experience))
            assertEquals(mode == HomeModes.ALPHABET, restored.settings.ui.experience.homeAlphabet)
            assertEquals(original.favorites, restored.favorites)
            assertEquals(original.settings.ui.v2, restored.settings.ui.v2)
        }
    }

    @Test fun explicitModeWinsOverLegacyBackupFlag() {
        val value = ExperienceCodec.decode(JSONObject().put("homeMode", "Normal").put("homeAlphabet", true))
        assertEquals(HomeModes.NORMAL, HomeModes.effective(value))
        assertEquals(value, ExperienceCodec.decode(ExperienceCodec.encode(value)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidBackupModeIsRejected() { ExperienceCodec.decode(JSONObject().put("homeMode", "WebView")) }

    @Test(expected = IllegalArgumentException::class)
    fun invalidSelectionDoesNotBecomeAStoredMode() { HomeModes.select(ExperiencePreferences(), "Other") }
}
