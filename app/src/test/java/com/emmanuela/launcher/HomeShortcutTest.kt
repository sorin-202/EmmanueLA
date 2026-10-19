package com.emmanuela.launcher

import com.emmanuela.launcher.data.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class HomeShortcutTest {
    private val shortcut=HomeShortcut("com.example/.Main",WidgetPlacement(16f,320f))

    @Test fun configurationRoundTripPersistsIconsAndWidgetPositionsIndependently() {
        val data=LauncherData(settings=Preferences(ui=UiPreferences(v2=V2Preferences(
            homeShortcuts=listOf(shortcut),widgetPositions=mapOf("clock" to WidgetPlacement(64f,32f))))))
        assertEquals(data,ConfigurationCodec.decode(ConfigurationCodec.encode(data)))
        val encoded=V2Codec.encode(data.settings.ui.v2).apply{remove("homeShortcuts")}
        assertTrue("Existing homes migrate without icons",V2Codec.decode(encoded).homeShortcuts.isEmpty())
    }
    @Test fun hiddenAppsAreNeitherShownNorOfferedAndPrivateAppsUseNormalSelection() {
        val hidden=LaunchableApp("com.hidden/.Main","Hidden","com.hidden")
        val privateApp=LaunchableApp("com.private/.Main","Private","com.private")
        val saved=LaunchableApp(shortcut.appId,"Saved","com.example")
        val policies=mapOf("com.hidden" to AppPolicy(hidden=true),"com.private" to AppPolicy(privateApp=true))
        assertFalse(HomeShortcuts.visible(hidden.id,policies))
        assertEquals(listOf(privateApp),HomeShortcuts.candidates(listOf(hidden,privateApp,saved),listOf(shortcut),policies))
        // The Home UI passes this app to secureLaunch; it never constructs an Intent.
        assertTrue(HomeShortcuts.visible(privateApp.id,policies))
    }
    @Test fun defaultPlacementUsesAnotherVisibleSlot() {
        val first=HomeShortcuts.availablePosition(emptyList(),360f,640f)
        val second=HomeShortcuts.availablePosition(listOf(first),360f,640f)
        assertNotEquals(first,second)
        assertTrue(second.x in 0f..284f && second.y in 0f..550f)
        assertEquals(WidgetPlacement(0f,0f),HomeShortcuts.availablePosition(emptyList(),50f,50f))
    }
    @Test(expected=IllegalArgumentException::class) fun rejectsDuplicateActivity() {
        HomeShortcutCodec.decode(HomeShortcutCodec.encode(listOf(shortcut,shortcut)))
    }
    @Test(expected=IllegalArgumentException::class) fun rejectsNonfiniteCoordinate() {
        HomeShortcutCodec.decode(JSONArray().put(JSONObject().put("appId",shortcut.appId).put("x","NaN").put("y",0)))
    }
    @Test(expected=IllegalArgumentException::class) fun rejectsNonActivityShortcut() {
        HomeShortcutCodec.decode(JSONArray().put(JSONObject().put("appId","https://example.org").put("x",0).put("y",0)))
    }
    @Test(expected=IllegalArgumentException::class) fun enforcesBoundedCount() {
        HomeShortcutCodec.decode(HomeShortcutCodec.encode(List(HomeShortcuts.MAX_COUNT+1){shortcut.copy(appId="com.example/.Activity$it")}))
    }
}
