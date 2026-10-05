package com.emmanuela.launcher

import com.emmanuela.launcher.data.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class SearchActionsTest {
    @Test fun optionalConfigurationRoundTripsAndOldPreferencesRemainEmpty(){
        val p=ExperiencePreferences(searchActions=setOf("settings","app:com.example/.Main"))
        assertEquals(p,ExperienceCodec.decode(ExperienceCodec.encode(p)))
        assertTrue(ExperienceCodec.decode(JSONObject()).searchActions.isEmpty())
    }
    @Test fun deterministicRankingAndReservedModes(){
        val labels=mapOf("battery" to "Battery settings","settings" to "Settings","other" to "Reset settings")
        assertEquals(listOf("settings","battery","other"),SearchActions.matches("settings",labels))
        listOf("","/settings","@settings","#settings","unrelated").forEach{assertTrue(SearchActions.matches(it,labels).isEmpty())}
    }
    @Test fun importedActionsAreBoundedAndCannotContainArbitraryIntents(){
        listOf("intent://unsafe","app:bad","none","app:pkg/Activity --extra").forEach{assertFalse(SearchActions.valid(it))}
        assertThrows(IllegalArgumentException::class.java){SearchActions.decode(JSONArray(List(17){"settings"}))}
        assertThrows(IllegalArgumentException::class.java){SearchActions.decode(JSONArray(listOf("intent://unsafe")))}
    }
}
