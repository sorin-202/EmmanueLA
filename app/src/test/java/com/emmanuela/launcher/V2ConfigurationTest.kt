package com.emmanuela.launcher

import com.emmanuela.launcher.data.AppPolicy
import com.emmanuela.launcher.data.ConfigurationCodec
import com.emmanuela.launcher.data.LauncherData
import com.emmanuela.launcher.data.Preferences
import com.emmanuela.launcher.data.UiPreferences
import com.emmanuela.launcher.data.V2Codec
import com.emmanuela.launcher.data.V2Preferences
import com.emmanuela.launcher.data.WidgetPlacement


import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class V2ConfigurationTest {
    @Test fun newStateRoundTripsWithCoordinatesAndBadgeRule() {
        val v=V2Preferences(gesturesEnabled=false,holdAction="clock",foldersEnabled=false,shortcutMode="Icon",
            widgetPositions=mapOf("clock" to WidgetPlacement(32f,64f)),weatherEnabled=true,latitude="44.17",longitude="28.64",
            wallpaperSwitchMinute=7*60,cropX=-.4f,cropY=.7f,cropZoom=1.5f,recentTags=listOf("work","social"))
        val data=LauncherData(settings=Preferences(theme="Custom",ui=UiPreferences(searchStyle="Pill",alphabetAnimation="Wave",v2=v)),
            policies=mapOf("org.example" to AppPolicy(badgesMuted=true)))
        assertEquals(data,ConfigurationCodec.decode(ConfigurationCodec.encode(data)))
    }
    @Test fun legacyVersionFiveDefaultsNewFields() {
        val json=JSONObject(ConfigurationCodec.encode(LauncherData()))
        json.put("version",5);json.getJSONObject("settings").getJSONObject("ui").remove("v2")
        assertEquals(V2Preferences(),ConfigurationCodec.decode(json.toString()).settings.ui.v2)
    }
    @Test fun portableBackupDropsAlbumGrantButKeepsPlacement() {
        val state=LauncherData(settings=Preferences(ui=UiPreferences(v2=V2Preferences(wallpaperAlbum="content://album",widgetPositions=mapOf("date" to WidgetPlacement(8f,16f))))))
        val portable=ConfigurationCodec.portable(state)
        assertEquals("",portable.settings.ui.v2.wallpaperAlbum)
        assertEquals(state.settings.ui.v2.widgetPositions,portable.settings.ui.v2.widgetPositions)
    }
    @Test(expected=IllegalArgumentException::class) fun rejectOffscreenUnboundedCoordinate() {
        V2Codec.decode(JSONObject().put("widgetPositions",JSONObject().put("clock",JSONObject().put("x",-1).put("y",0))))
    }
    @Test(expected=IllegalArgumentException::class) fun rejectInvalidWeatherLatitude() {
        V2Codec.decode(JSONObject().put("latitude","91"))
    }
}
