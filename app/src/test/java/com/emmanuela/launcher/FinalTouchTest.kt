package com.emmanuela.launcher

import com.emmanuela.launcher.data.*
import com.emmanuela.launcher.ui.navigation.LauncherSurface
import com.emmanuela.launcher.ui.navigation.PagePolicy
import org.junit.Assert.*
import org.junit.Test

class FinalTouchTest {
    @Test fun disabledDrawerFallsBackToOtherDrawerBeforeEnabledHome(){
        val noApps=Preferences(ui=UiPreferences(appListEnabled=false))
        assertEquals(LauncherSurface.FOLDERS,PagePolicy.resolve(LauncherSurface.APPS,noApps))
        val noFolders=Preferences(ui=UiPreferences(v2=V2Preferences(foldersEnabled=false)))
        assertEquals(LauncherSurface.APPS,PagePolicy.resolve(LauncherSurface.FOLDERS,noFolders))
        val neither=noApps.copy(ui=noApps.ui.copy(v2=V2Preferences(foldersEnabled=false)))
        assertEquals(LauncherSurface.HOME,PagePolicy.drawer(neither))
        assertEquals(LauncherSurface.BLANK,PagePolicy.drawer(neither.copy(ui=neither.ui.copy(experience=ExperiencePreferences(homeEnabled=false)))))
    }
    @Test fun newActionsSurviveFullConfigurationRoundTrip(){
        val original=LauncherData(settings=Preferences(swipeUp="drawer",ui=UiPreferences(leftShortcut="drawer",
            v2=V2Preferences(locationAction="app:com.example.maps/.MainActivity",holdAction="drawer"))))
        assertEquals(original,ConfigurationCodec.decode(ConfigurationCodec.encode(original)))
        val old=org.json.JSONObject(V2Codec.encode(original.settings.ui.v2).toString()).apply{remove("locationAction")}
        assertEquals("none",V2Codec.decode(old).locationAction)
    }
    @Test fun oldCustomSizeMigratesWithoutLosingIndependentAdjustments(){
        val old=UiPreferences(bottomBarHeight=96f,bottomBarWidth=.7f,bottomBarOffset=72f,
            experience=ExperiencePreferences(bottomSize="Custom"))
        val restored=UiPreferencesCodec.decode(UiPreferencesCodec.encode(old))
        assertEquals("Medium",restored.experience.bottomSize)
        assertEquals(96f,restored.bottomBarHeight,0f)
        assertEquals(.7f,restored.bottomBarWidth,0f)
        assertEquals(72f,restored.bottomBarOffset,0f)
    }
}
