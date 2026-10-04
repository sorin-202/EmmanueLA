package com.emmanuela.launcher

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.emmanuela.launcher.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConfigurationPersistenceTest {
    @Test fun independentPageChoicesSurviveRepositoryRecreationAndInvalidUpdatesAreAtomic() = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val repository=ConfigurationRepository(context)
        val original=repository.data.first()
        try {
            repository.update { it.copy(settings=it.settings.copy(ui=it.settings.ui.copy(
                appListEnabled=true, experience=it.settings.ui.experience.copy(homeAlphabet=true, searchPackages=false)))) }
            val reloaded=ConfigurationRepository(context).data.first()
            assertTrue(reloaded.settings.ui.appListEnabled)
            assertTrue(reloaded.settings.ui.experience.homeAlphabet)
            assertFalse(reloaded.settings.ui.experience.searchPackages)
            try {
                repository.update { it.copy(folders=listOf(AppFolder("test", "x".repeat(100), "folder", emptyList()))) }
                fail("Invalid data must be rejected before writing")
            } catch (_: IllegalArgumentException) { }
            assertEquals(reloaded, repository.data.first())
        } finally { repository.restore(original) }
    }
}
