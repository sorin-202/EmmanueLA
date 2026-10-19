package com.emmanuela.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.emmanuela.launcher.data.AppFolder
import com.emmanuela.launcher.data.LaunchableApp
import com.emmanuela.launcher.ui.folders.FolderSurface
import com.emmanuela.launcher.ui.folders.MagneticFolderApps
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FolderUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun perFolderSolidColorRendersEvenWhenGlobalFillIsOffAndLegacyOutlineRemains() {
        val appearance = mutableStateOf(AppFolder("a", "A", "folder", emptyList(), color = 0xff12ab34L, solidBackground = true))
        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(120.dp).background(Color.White)) {
                    FolderSurface(appearance.value, false, Modifier.fillMaxSize().testTag("appearance")) { }
                }
            }
        }
        fun center(): Color {
            val pixels = compose.onNodeWithTag("appearance").captureToImage().toPixelMap()
            return pixels[pixels.width / 2, pixels.height / 2]
        }
        assertEquals(Color(0xff12ab34L), center())
        compose.runOnIdle { appearance.value = appearance.value.copy(solidBackground = false) }
        assertEquals(Color.White, center())
    }

    @Test fun fiveByFourGridRendersFiveColumnsScrollsAndAdaptsWithoutChangingOrder() {
        val entries = List(30) { LaunchableApp("fake.$it/.Main", "App $it", "fake.$it") }
        val folder = mutableStateOf(AppFolder("grid", "Grid", "folder", entries.map { it.id }, layout = "Grid", gridColumns = 5, gridRows = 4, manualOrder = true))
        compose.setContent {
            MaterialTheme {
                val model: LauncherViewModel = viewModel()
                MagneticFolderApps(folder.value, entries, false, false, model, {}, {}, Modifier.width(320.dp).height(400.dp))
            }
        }
        fun top(index: Int) = compose.onNodeWithText("App $index").fetchSemanticsNode().boundsInRoot.top
        assertEquals(top(0), top(4), 1f)
        assertTrue(top(5) > top(0))
        compose.onNodeWithTag("folder-app-grid").performScrollToNode(hasText("App 29"))
        compose.onNodeWithText("App 29").assertIsDisplayed()
        compose.onNodeWithTag("folder-app-grid").performScrollToNode(hasText("App 0"))
        compose.runOnIdle { folder.value = folder.value.copy(gridColumns = 3, gridRows = 3) }
        assertEquals(top(0), top(2), 1f)
        assertTrue(top(3) > top(0))
        assertEquals(entries.map { it.id }, folder.value.apps)
    }
}
