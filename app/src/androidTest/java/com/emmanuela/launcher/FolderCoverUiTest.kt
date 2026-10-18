package com.emmanuela.launcher

import android.graphics.Bitmap
import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.emmanuela.launcher.data.AppFolder
import com.emmanuela.launcher.data.ConfigurationRepository
import com.emmanuela.launcher.data.LauncherData
import com.emmanuela.launcher.ui.components.LocalLauncherLanguage
import com.emmanuela.launcher.ui.folders.DenseFolderCanvas
import com.emmanuela.launcher.ui.folders.EditableFolderGrid
import com.emmanuela.launcher.ui.folders.FolderAppearanceEditor
import com.emmanuela.launcher.ui.folders.FolderEditor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FolderCoverUiTest {
    @get:Rule val compose = createComposeRule()

    private fun cover(name: String): Pair<File, String> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val image = File(File(context.cacheDir, "shared").apply { mkdirs() }, name)
        val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.RED)
        image.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
        return image to FileProvider.getUriForFile(context, "${context.packageName}.files", image).toString()
    }

    private fun awaitPictureOnly(name: String) {
        compose.waitUntil(5_000) { compose.onAllNodesWithText(name, useUnmergedTree = true).fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText(name, useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun gridCoverHidesLabelsIndependentlyAndStillOpensTheSameFolder() {
        val (document, uri) = cover("folder-cover-grid.png")
        val draft = mutableStateOf(AppFolder("cover", "Holiday", "sun", emptyList(), imageUri = uri,
            showCoverIcon = false, showCoverName = false))
        var opened = ""
        try {
            compose.setContent { MaterialTheme {
                EditableFolderGrid(LauncherData(folders = listOf(draft.value)), Modifier.width(320.dp).height(240.dp), { opened = it }, { _, _, _ -> })
            } }
            awaitPictureOnly("Holiday")
            compose.onNodeWithTag("folder-cover-icon:cover", useUnmergedTree = true).assertDoesNotExist()
            val tile = compose.onNodeWithContentDescription("Folder: Holiday")
            val pixels = tile.captureToImage().toPixelMap()
            assertEquals(Color.Red, pixels[pixels.width / 2, pixels.height / 2])
            tile.performClick()
            compose.runOnIdle { assertEquals("cover", opened); draft.value = draft.value.copy(showCoverIcon = true) }
            compose.onNodeWithTag("folder-cover-icon:cover", useUnmergedTree = true).assertExists()
            compose.onNodeWithText("Holiday", useUnmergedTree = true).assertDoesNotExist()
            compose.runOnIdle { draft.value = draft.value.copy(showCoverIcon = false, showCoverName = true) }
            compose.onNodeWithText("Holiday", useUnmergedTree = true).assertIsDisplayed()
            compose.onNodeWithTag("folder-cover-icon:cover", useUnmergedTree = true).assertDoesNotExist()
        } finally { document.delete() }
    }

    @Test fun canvasCoverRetainsAccessibleProtectedIdentityAndOpeningCallback() {
        val (document, uri) = cover("folder-cover-canvas.png")
        val folder = AppFolder("protected-cover", "Private", "lock", emptyList(), imageUri = uri,
            widthUnits = 3, heightUnits = 3, deviceProtected = true, showCoverIcon = false, showCoverName = false)
        var requested = ""
        try {
            compose.setContent { MaterialTheme {
                DenseFolderCanvas(LauncherData(folders = listOf(folder)), Modifier.width(320.dp).height(240.dp), { requested = it }, { _, _, _ -> })
            } }
            awaitPictureOnly("Private")
            compose.onNodeWithTag("folder-cover-icon:protected-cover", useUnmergedTree = true).assertDoesNotExist()
            compose.onNodeWithText("Locked", useUnmergedTree = true).assertIsDisplayed()
            compose.onNodeWithContentDescription("Locked folder: Private").performClick()
            compose.runOnIdle { assertEquals("protected-cover", requested); assertTrue(folder.isProtected) }
        } finally { document.delete() }
    }

    @Test fun editorTogglesImmediatelyUpdateCoverPreview() {
        val (document, uri) = cover("folder-cover-editor.png")
        val draft = mutableStateOf(AppFolder("preview", "Preview name", "sun", emptyList(), imageUri = uri))
        try {
            compose.setContent { MaterialTheme {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    FolderAppearanceEditor(draft.value, true, false, {}, {}, {}, {}, {},
                        { draft.value = draft.value.copy(showCoverIcon = it) }, { draft.value = draft.value.copy(showCoverName = it) })
                }
            } }
            compose.onNodeWithContentDescription("Show folder icon on cover").performScrollTo().performClick()
            compose.onNodeWithContentDescription("Show folder name on cover").performScrollTo().performClick()
            compose.onNodeWithTag("folder-appearance-preview").performScrollTo()
            awaitPictureOnly("Preview name")
            compose.onNodeWithTag("folder-cover-icon:preview", useUnmergedTree = true).assertDoesNotExist()
            val pixels = compose.onNodeWithTag("folder-appearance-preview").captureToImage().toPixelMap()
            assertEquals(Color.Red, pixels[pixels.width / 2, pixels.height / 2])
            compose.runOnIdle { assertFalse(draft.value.showCoverIcon); assertFalse(draft.value.showCoverName) }
        } finally { document.delete() }
    }

    @Test fun deletedCoverRestoresVisibleIdentityOnResume() {
        val (document, uri) = cover("folder-cover-deleted.png")
        val folder = AppFolder("deleted", "Readable fallback", "sun", emptyList(), imageUri = uri,
            showCoverIcon = false, showCoverName = false)
        lateinit var owner: CoverLifecycle
        compose.runOnIdle { owner = CoverLifecycle().apply { registry.currentState = Lifecycle.State.RESUMED } }
        try {
            compose.setContent { MaterialTheme {
                CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                    EditableFolderGrid(LauncherData(folders = listOf(folder)), Modifier.width(320.dp).height(240.dp), {}, { _, _, _ -> })
                }
            } }
            awaitPictureOnly("Readable fallback")
            assertTrue(document.delete())
            compose.runOnIdle { owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE); owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME) }
            compose.waitUntil(5_000) { compose.onAllNodesWithText("Readable fallback", useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithText("Readable fallback", useUnmergedTree = true).assertIsDisplayed()
            compose.onNodeWithTag("folder-cover-icon:deleted", useUnmergedTree = true).assertExists()
            compose.onNodeWithContentDescription("Folder: Readable fallback").assertHasClickAction()
        } finally { document.delete() }
    }

    @Test fun folderEditorKeepsCoverDraftAcrossCollapsedSectionsAndRestorationThenPersistsIt() = runBlocking {
        val (document, uri) = cover("folder-cover-saved.png")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val repository = ConfigurationRepository(context)
        val original = repository.data.first()
        val folder = AppFolder("editor-save", "Saved cover", "sun", listOf("test/.B", "test/.A"),
            imageUri = uri, manualOrder = true, gridColumns = 5, gridRows = 4)
        val store = ViewModelStore()
        val show = mutableStateOf(true)
        val observed = MutableStateFlow<LauncherData?>(null)
        val observer = launch(Dispatchers.IO) { repository.data.collect { observed.value = it } }
        try {
            repository.restore(LauncherData(folders = listOf(folder)))
            withTimeout(5_000) { observed.filterNotNull().first { it.folders.any { f -> f.id == folder.id } } }
            lateinit var model: LauncherViewModel
            instrumentation.runOnMainSync {
                model = ViewModelProvider(store, ViewModelProvider.AndroidViewModelFactory(context.applicationContext as Application))[LauncherViewModel::class.java]
            }
            compose.waitUntil(5_000) { model.ready.value }
            val restoration = StateRestorationTester(compose)
            restoration.setContent { MaterialTheme {
                CompositionLocalProvider(LocalLauncherLanguage provides "en") {
                    if(show.value) FolderEditor(folder, emptyList(), model, { show.value = false }, { show.value = false })
                }
            } }
            compose.onNodeWithContentDescription("Show folder icon on cover").performScrollTo().performClick()
            compose.onNodeWithContentDescription("Show folder name on cover").performScrollTo().performClick()
            compose.onNodeWithText("Appearance").performScrollTo().performClick()
            compose.onNodeWithTag("folder-appearance-preview").assertDoesNotExist()
            compose.onNodeWithText("Layout & size").performScrollTo().performClick()
            compose.onNodeWithContentDescription("Keep custom app order").performScrollTo().assertIsOn()
            compose.onNodeWithText("Layout & size").performScrollTo().performClick()
            restoration.emulateSavedInstanceStateRestore()
            compose.onNodeWithText("Save").performClick()
            val saved = withTimeout(5_000) {
                observed.filterNotNull().first { state -> state.folders.any { it.id == folder.id && !it.showCoverName && !it.showCoverIcon } }
                    .folders.single { it.id == folder.id }
            }
            assertEquals(folder.copy(showCoverIcon = false, showCoverName = false), saved)
            compose.waitUntil(5_000) { !show.value }
        } finally {
            compose.runOnIdle { show.value = false; store.clear() }
            observer.cancelAndJoin()
            repository.restore(original)
            document.delete()
        }
    }

    private class CoverLifecycle : LifecycleOwner {
        val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
    }
}
