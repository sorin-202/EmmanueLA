package com.emmanuela.launcher

import com.emmanuela.launcher.data.*
import org.junit.Assert.*
import org.junit.Test

class SearchRankingTest {
    private fun app(id: String, name: String) = LaunchableApp("$id/.Main", name, id)

    @Test fun labelRelevanceWinsOverAlphabeticalOrderAndPackageMatches() {
        val apps = listOf(app("test.pkg", "AAA"), app("sub", "Contest"), app("word", "A Test Tool"), app("prefix", "Testing"), app("exact", "Test"))
        val index = AppSearchIndex(apps)
        assertEquals(listOf("Test", "Testing", "A Test Tool", "Contest"), index.search("test").apps.map { it.label })
        assertEquals(listOf("Test", "Testing", "A Test Tool", "Contest", "AAA"), index.search("test", searchPackages=true).apps.map { it.label })
    }
    @Test fun caseAccentsAliasesAndMultipleWordsRemainSearchable() {
        val app = LaunchableApp("note/.Main", "Café Notes", "note", "Notebook")
        val index = AppSearchIndex(listOf(app))
        assertEquals(listOf(app), index.search("CAFE no").apps)
        assertEquals(listOf(app), index.search("notebook", searchAliases=false).apps)
        assertTrue(index.search("cafe", searchAliases=false).apps.isEmpty())
        assertTrue(index.search("cafe missing").apps.isEmpty())
    }
    @Test fun tiesAreStableAndHiddenAppsStayExcluded() {
        val a=app("a", "Test"); val b=app("b", "Test")
        assertEquals(listOf(a,b), AppSearchIndex(listOf(b,a)).search("test").apps)
        assertEquals(listOf(b), AppSearchIndex(listOf(a,b), setOf("a")).search("test").apps)
        assertTrue(AppSearchIndex(emptyList()).search("anything").apps.isEmpty())
    }
    @Test fun newConfigurationsDoNotSearchPackagesButSavedChoiceSurvives() {
        assertFalse(ExperiencePreferences().searchPackages)
        val data=LauncherData(settings=Preferences(ui=UiPreferences(experience=ExperiencePreferences(searchPackages=true))))
        assertTrue(ConfigurationCodec.decode(ConfigurationCodec.encode(data)).settings.ui.experience.searchPackages)
    }
    @Test fun preparedIndexPreservesCrossFieldMatchingAndAliasOptOut() {
        val app=LaunchableApp("org.calendar/.Main","Daily Agenda","org.calendar","Planner")
        val index=AppSearchIndex(listOf(app))
        assertEquals(listOf(app),index.search("daily planner").apps)
        assertTrue(index.search("daily planner",searchAliases=false).apps.isEmpty())
        assertEquals(listOf(app),index.search("planner calendar",searchAliases=false,searchPackages=true).apps)
        assertTrue(index.search("daily calendar",searchAliases=false,searchPackages=true).apps.isEmpty())
        assertTrue(index.search("planner calendar",searchPackages=false).apps.isEmpty())
        assertNull(SearchRanking.score("\u2003",listOf("\u2003")))
    }
}
