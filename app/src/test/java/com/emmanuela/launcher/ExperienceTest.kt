package com.emmanuela.launcher

import com.emmanuela.launcher.data.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.ZonedDateTime

class ExperienceTest {
    @Test fun upgradePreservesAliasesFoldersAndNewSettings() {
        val app="org.test/.Main"
        val data=LauncherData(folders=listOf(AppFolder("f","Work","folder",listOf(app),layout="Grid")),
            appMetadata=mapOf(app to AppMetadata("Mail",listOf("work"))),
            settings=Preferences(ui=UiPreferences(searchCursorBlinkMs=0,experience=ExperiencePreferences(advanced=true,widgetScales=mapOf("clock" to 1.5f)))),
            focusGroups=listOf(FocusGroup(name="Evening",packages=setOf("org.test"))))
        assertEquals(data,ConfigurationCodec.decode(ConfigurationCodec.encode(data)))
    }
    @Test fun oldDigestRulesRemainEnabled() {
        val state=LauncherData(policies=mapOf("org.test" to AppPolicy(notifications=NotificationMode.DIGEST)))
        val raw=JSONObject(ConfigurationCodec.encode(state));raw.put("version",6)
        raw.getJSONObject("settings").getJSONObject("ui").remove("experience")
        val decoded=ConfigurationCodec.decode(raw.toString())
        assertTrue(decoded.settings.ui.experience.digestEnabled)
    }
    @Test fun overnightScheduleUsesPreviousSelectedDay() {
        val group=FocusGroup(name="Night",packages=setOf("org.test"),schedule=true,days=setOf(1),startMinute=1320,endMinute=420)
        assertTrue(FocusCodec.scheduled(group,ZonedDateTime.parse("2026-10-06T02:00:00Z")))
        assertFalse(FocusCodec.scheduled(group,ZonedDateTime.parse("2026-10-07T02:00:00Z")))
    }
    @Test fun hiddenAppsAreNeverInUnfilteredDrawer() {
        val app=LaunchableApp("org.test/.Main","Mail","org.test")
        val searchable=AppSearchIndex(listOf(app),setOf("org.test"),false)
        assertTrue(searchable.all.apps.isEmpty())
        assertEquals(listOf(app),searchable.search("Mail").apps)
        assertTrue(AppSearchIndex(listOf(app),setOf("org.test"),true).search("Mail").apps.isEmpty())
    }
    @Test fun aliasAndPackageSearchCanBeDisabledIndependently() {
        val app=LaunchableApp("org.test/.Main","Email","org.test","Mail",listOf("work"))
        val index=AppSearchIndex(listOf(app))
        assertTrue(index.search("Email",searchAliases=false).apps.isEmpty())
        assertEquals(1,index.search("Mail",searchAliases=false).apps.size)
        assertTrue(index.search("org.test",searchPackages=false).apps.isEmpty())
        assertEquals(1,index.search("#work",searchAliases=false,searchPackages=false).apps.size)
    }
    @Test(expected=IllegalArgumentException::class) fun invalidCursorDoesNotOverwriteConfiguration(){ExperienceCodec.decode(JSONObject().put("cursorStyle","invalid"))}
    @Test(expected=IllegalArgumentException::class) fun invalidGroupTimerDoesNotOverwriteConfiguration(){FocusCodec.decode(FocusCodec.encode(listOf(FocusGroup(name="Night",packages=setOf("org.test"),dailyMinutes=-1))))}
}
