package com.emmanuela.launcher

import android.app.Activity
import android.app.Application
import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.emmanuela.launcher.data.*
import com.emmanuela.launcher.ui.appearance.PageWallpaperSettings
import com.emmanuela.launcher.ui.appearance.PageWallpaperSurface
import com.emmanuela.launcher.ui.components.LocalLauncherLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
import java.util.concurrent.CopyOnWriteArrayList

/** Real picker callbacks, editor drafts, persistence and the same surface used by launcher pages. */
@RunWith(AndroidJUnit4::class)
class PageWallpaperFlowTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private fun photo(name: String, color: Int, width: Int = 80, height: Int = 80): Pair<File, String> {
        val context = instrumentation.targetContext
        val file = File(File(context.cacheDir, "shared").apply { mkdirs() }, name)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(color)
        file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
        return file to FileProvider.getUriForFile(context, "${context.packageName}.files", file).toString()
    }

    private inner class Fixture(
        val repository: ConfigurationRepository,
        val model: LauncherViewModel,
        val observed: MutableStateFlow<LauncherData?>,
        val page: MutableState<String>,
        val editor: MutableState<Boolean>,
        val visible: MutableState<Boolean>,
        val owner: WallpaperLifecycle
    ) {
        fun choosePage(value: String) = compose.runOnIdle { page.value = value }
        fun showEditor(value: Boolean) = compose.runOnIdle { editor.value = value }
        suspend fun stored(page: String, expected: PageWallpaper?) {
            withTimeout(5_000) { observed.filterNotNull().first { it.settings.ui.pageWallpapers[page] == expected } }
            withTimeout(5_000) { model.data.first { it.settings.ui.pageWallpapers[page] == expected } }
            compose.waitForIdle()
        }
        fun click(text: String) = compose.onNodeWithText(text).performScrollTo().performClick()
        fun resume() = compose.runOnIdle {
            owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            owner.registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
        fun awaitImage(tag: String) {
            compose.waitUntil(5_000) { compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithTag(tag, useUnmergedTree = true).assertExists()
        }
        fun assertImageColor(tag: String, expected: Color) {
            awaitImage(tag)
            if (tag == "Page wallpaper preview image") compose.onNodeWithTag(tag, useUnmergedTree = true).performScrollTo()
            // The preview can dim the photo and page content can cover its center.
            // A corner sample establishes that the actual decoded image changed.
            compose.waitUntil(5_000) {
                val image = compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().singleOrNull()
                if (image == null) false else {
                    val pixels = compose.onNodeWithTag(tag, useUnmergedTree = true).captureToImage().toPixelMap()
                    val sample = pixels[pixels.width / 4, pixels.height / 4]
                    when (expected) {
                        Color.Red -> sample.red > sample.green + .2f && sample.red > sample.blue + .2f
                        Color.Green -> sample.green > sample.red + .2f && sample.green > sample.blue + .2f
                        Color.Blue -> sample.blue > sample.red + .2f && sample.blue > sample.green + .2f
                        else -> error("Fixture color is not supported")
                    }
                }
            }
        }
    }

    private fun withFixture(test: suspend Fixture.() -> Unit) = runBlocking {
        val context = instrumentation.targetContext
        val repository = ConfigurationRepository(context)
        val original = repository.data.first()
        val store = ViewModelStore()
        val observed = MutableStateFlow<LauncherData?>(null)
        val page = mutableStateOf("apps")
        val editor = mutableStateOf(false)
        val visible = mutableStateOf(true)
        lateinit var owner: WallpaperLifecycle
        var model: LauncherViewModel? = null
        var observer: Job? = null
        try {
            repository.restore(LauncherData(settings = Preferences(ui = UiPreferences(
                experience = ExperiencePreferences(language = "en", autoLaunch = false)
            ))))
            observer = launch(Dispatchers.IO) { repository.data.collect { observed.value = it } }
            withTimeout(5_000) { observed.filterNotNull().first() }
            instrumentation.runOnMainSync {
                model = ViewModelProvider(store, ViewModelProvider.AndroidViewModelFactory(context.applicationContext as Application))[LauncherViewModel::class.java]
                owner = WallpaperLifecycle().apply { registry.currentState = Lifecycle.State.RESUMED }
            }
            val realModel = requireNotNull(model)
            compose.waitUntil(5_000) { realModel.ready.value }
            compose.setContent {
                val data by realModel.data.collectAsState()
                MaterialTheme(colorScheme = lightColorScheme()) {
                    CompositionLocalProvider(LocalLauncherLanguage provides "en", LocalLifecycleOwner provides owner) {
                        if (visible.value) {
                            if (editor.value) Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState())) {
                                PageWallpaperSettings(page.value, data, realModel)
                            } else PageWallpaperSurface(page.value, data.settings, Modifier.fillMaxSize()) {
                                // Plain remember deliberately detects a remounted content subtree.
                                // A saved-state holder could mask wallpaper-driven composition loss.
                                val counter = remember { mutableIntStateOf(0) }
                                Column {
                                    Text("${page.value} page content")
                                    TextButton(onClick = { counter.intValue++ }) { Text("Page counter: ${counter.intValue}") }
                                }
                            }
                        }
                    }
                }
            }
            Fixture(repository, realModel, observed, page, editor, visible, owner).test()
        } finally {
            try {
                if (model != null) {
                    compose.runOnIdle { visible.value = false }
                    instrumentation.runOnMainSync { store.clear(); owner.registry.currentState = Lifecycle.State.DESTROYED }
                }
            } finally {
                observer?.cancelAndJoin()
                repository.restore(original)
            }
        }
    }

    @Test fun independentPageWallpapersRenderAndSurviveRepositoryRecreation() = withFixture {
        val (appsFile, appsUri) = photo("page-wallpaper-apps.png", android.graphics.Color.RED)
        val (foldersFile, foldersUri) = photo("page-wallpaper-folders.png", android.graphics.Color.GREEN)
        val appsWallpaper = PageWallpaper(appsUri, PhotoStyle(dim = 0f))
        val foldersWallpaper = PageWallpaper(foldersUri, PhotoStyle(dim = 0f, zoom = 1.5f))
        try {
            model.settings { it.copy(wallpapers = listOf(appsUri)) }
            withTimeout(5_000) { observed.filterNotNull().first { it.settings.wallpapers == listOf(appsUri) } }
            withTimeout(5_000) { model.data.first { it.settings.wallpapers == listOf(appsUri) } }
            val homeBefore = repository.data.first().settings.wallpapers
            compose.onNodeWithText("Page counter: 0").performClick()
            assertTrue(model.savePageWallpaper("apps", appsWallpaper))
            stored("apps", appsWallpaper)
            assertImageColor("Page wallpaper: apps", Color.Red)
            compose.onNodeWithText("Page counter: 1").assertIsDisplayed()
            assertTrue(model.savePageWallpaper("folders", foldersWallpaper))
            stored("folders", foldersWallpaper)
            choosePage("folders")
            assertImageColor("Page wallpaper: folders", Color.Green)
            val reloaded = ConfigurationRepository(instrumentation.targetContext).data.first().settings
            assertEquals(mapOf("apps" to appsWallpaper, "folders" to foldersWallpaper), reloaded.ui.pageWallpapers)
            assertEquals("Page wallpaper changes must leave Home wallpaper selection intact", homeBefore, reloaded.wallpapers)
            assertTrue(model.savePageWallpaper("apps", null))
            stored("apps", null)
            assertEquals(foldersWallpaper, ConfigurationRepository(instrumentation.targetContext).data.first().settings.ui.pageWallpapers["folders"])
            choosePage("apps")
            compose.onNodeWithTag("Page wallpaper: apps", useUnmergedTree = true).assertDoesNotExist()
            compose.onNodeWithText("apps page content").assertIsDisplayed()
        } finally { appsFile.delete(); foldersFile.delete() }
    }

    @Test fun documentPickerPreviewsBeforeSaveCancelKeepsPriorImageAndRemoveOnlyChangesItsPage() = withFixture {
        val (savedFile, savedUri) = photo("page-wallpaper-saved.png", android.graphics.Color.RED)
        val (siblingFile, siblingUri) = photo("page-wallpaper-sibling.png", android.graphics.Color.GREEN)
        val (chosenFile, chosenUri) = photo("page-wallpaper-chosen.png", android.graphics.Color.BLUE)
        val saved = PageWallpaper(savedUri, PhotoStyle(dim = 0f))
        val sibling = PageWallpaper(siblingUri, PhotoStyle(dim = 0f))
        val requests = CopyOnWriteArrayList<Intent>()
        var monitor: Instrumentation.ActivityMonitor? = null
        try {
            assertTrue(model.savePageWallpaper("apps", saved)); stored("apps", saved)
            assertTrue(model.savePageWallpaper("folders", sibling)); stored("folders", sibling)
            monitor = object : Instrumentation.ActivityMonitor() {
                override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
                    if (intent.action != Intent.ACTION_OPEN_DOCUMENT) return null
                    requests.add(Intent(intent))
                    return Instrumentation.ActivityResult(Activity.RESULT_OK, Intent().setData(android.net.Uri.parse(chosenUri)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                }
            }.also(instrumentation::addMonitor)
            showEditor(true)
            assertImageColor("Page wallpaper preview image", Color.Red)
            click("Choose photo")
            compose.waitUntil(5_000) { requests.size == 1 }
            assertTrue("Picker must request image documents", requests.single().getStringArrayExtra(Intent.EXTRA_MIME_TYPES)?.contains("image/*") == true || requests.single().type == "image/*")
            assertImageColor("Page wallpaper preview image", Color.Blue)
            assertEquals("Selecting a photo is a preview, not a saved change", saved, repository.data.first().settings.ui.pageWallpapers["apps"])
            click("Cancel")
            assertImageColor("Page wallpaper preview image", Color.Red)
            assertEquals(saved, ConfigurationRepository(instrumentation.targetContext).data.first().settings.ui.pageWallpapers["apps"])
            click("Choose photo")
            compose.waitUntil(5_000) { requests.size == 2 }
            assertImageColor("Page wallpaper preview image", Color.Blue)
            // Leaving this editor disposes its draft, as navigating back does.
            showEditor(false)
            assertEquals(saved, ConfigurationRepository(instrumentation.targetContext).data.first().settings.ui.pageWallpapers["apps"])
            showEditor(true)
            assertImageColor("Page wallpaper preview image", Color.Red)
            click("Choose photo")
            compose.waitUntil(5_000) { requests.size == 3 }
            assertImageColor("Page wallpaper preview image", Color.Blue)
            click("Save wallpaper")
            withTimeout(5_000) { observed.filterNotNull().first { it.settings.ui.pageWallpapers["apps"]?.uri == chosenUri } }
            var applied = ConfigurationRepository(instrumentation.targetContext).data.first().settings.ui.pageWallpapers.getValue("apps")
            assertEquals(chosenUri, applied.uri)
            assertEquals(sibling, repository.data.first().settings.ui.pageWallpapers["folders"])
            // Saving replaces the draft state even when the URI stays the same. A second
            // gesture must still update the live preview and the next persisted crop.
            stored("apps", applied)
            val originalCrop = applied.style
            compose.onNodeWithTag("Page wallpaper preview").performScrollTo().performTouchInput {
                pinch(
                    start0 = center - Offset(width * .12f, 0f),
                    end0 = center - Offset(width * .28f, 0f),
                    start1 = center + Offset(width * .12f, 0f),
                    end1 = center + Offset(width * .28f, 0f),
                    durationMillis = 300
                )
            }
            compose.onNodeWithTag("Page wallpaper preview").performTouchInput {
                swipe(start = center, end = center + Offset(width * .15f, 0f), durationMillis = 300)
            }
            click("Save wallpaper")
            applied = withTimeout(5_000) {
                observed.filterNotNull().first { state ->
                    state.settings.ui.pageWallpapers["apps"]?.let { it.uri == chosenUri && it.style.zoom > originalCrop.zoom && it.style.x != originalCrop.x } == true
                }.settings.ui.pageWallpapers.getValue("apps")
            }
            stored("apps", applied)
            assertEquals("Cropping never changes the selected document", chosenUri, applied.uri)
            assertEquals(sibling, repository.data.first().settings.ui.pageWallpapers["folders"])
            showEditor(false)
            assertImageColor("Page wallpaper: apps", Color.Blue)
            choosePage("folders"); showEditor(true)
            assertImageColor("Page wallpaper preview image", Color.Green)
            click("Remove wallpaper")
            stored("folders", null)
            assertEquals(applied, ConfigurationRepository(instrumentation.targetContext).data.first().settings.ui.pageWallpapers["apps"])
            showEditor(false)
            compose.onNodeWithTag("Page wallpaper: folders", useUnmergedTree = true).assertDoesNotExist()
            compose.onNodeWithText("folders page content").assertIsDisplayed()
        } finally {
            monitor?.let(instrumentation::removeMonitor)
            savedFile.delete(); siblingFile.delete(); chosenFile.delete()
        }
    }

    @Test fun deletedImageFallsBackOnResumeWithoutErasingTheSavedChoice() = withFixture {
        val (document, uri) = photo("page-wallpaper-deleted.png", android.graphics.Color.RED)
        val saved = PageWallpaper(uri, PhotoStyle(dim = 0f))
        try {
            assertTrue(model.savePageWallpaper("apps", saved)); stored("apps", saved)
            assertImageColor("Page wallpaper: apps", Color.Red)
            compose.onNodeWithText("Page counter: 0").performClick()
            compose.onNodeWithText("Page counter: 1").assertIsDisplayed()
            assertTrue(document.delete())
            resume()
            compose.waitUntil(5_000) { compose.onAllNodesWithTag("Page wallpaper: apps", useUnmergedTree = true).fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithTag("Page wallpaper: apps", useUnmergedTree = true).assertDoesNotExist()
            compose.onNodeWithText("apps page content").assertIsDisplayed()
            compose.onNodeWithText("Page counter: 1").assertIsDisplayed()
            assertEquals("An unavailable image should not erase the user's saved choice", saved, ConfigurationRepository(instrumentation.targetContext).data.first().settings.ui.pageWallpapers["apps"])
        } finally { document.delete() }
    }

    @Test fun fitWallpaperKeepsDarkLetterboxesAndReadableTextInLightTheme() = withFixture {
        val (document, uri) = photo("page-wallpaper-wide-light.png", android.graphics.Color.WHITE, width = 800, height = 80)
        val saved = PageWallpaper(uri, PhotoStyle(dim = 0f, fit = "Fit"))
        try {
            model.settings { it.copy(theme = "Light") }
            withTimeout(5_000) { observed.filterNotNull().first { it.settings.theme == "Light" } }
            assertTrue(model.savePageWallpaper("apps", saved)); stored("apps", saved)
            awaitImage("Page wallpaper: apps")
            val pixels = compose.onNodeWithTag("Page wallpaper: apps", useUnmergedTree = true).captureToImage().toPixelMap()
            val topLetterbox = pixels[(pixels.width * .9f).toInt(), (pixels.height * .1f).toInt()]
            val bottomLetterbox = pixels[(pixels.width * .9f).toInt(), (pixels.height * .9f).toInt()]
            assertTrue("Light theme Fit letterboxes must contrast with white page text", topLetterbox.red < .1f && topLetterbox.green < .1f && topLetterbox.blue < .1f)
            assertTrue("The footer letterbox also needs a dark backing", bottomLetterbox.red < .1f && bottomLetterbox.green < .1f && bottomLetterbox.blue < .1f)
            assertEquals("Fit keeps the actual white photograph centered and undimmed", Color.White, pixels[pixels.width / 2, pixels.height / 2])
            val label = compose.onNodeWithText("apps page content").captureToImage().toPixelMap()
            assertTrue("The page label must remain visible in the dark letterbox", (0 until label.width).any { x ->
                (0 until label.height).any { y -> label[x, y].let { it.red > .9f && it.green > .9f && it.blue > .9f } }
            })
            captureUxScreenshot("page-wallpaper-fit-light")
            compose.onNodeWithText("Page counter: 0").performClick()
            assertTrue(model.savePageWallpaper("apps", null)); stored("apps", null)
            compose.onNodeWithTag("Page wallpaper: apps", useUnmergedTree = true).assertDoesNotExist()
            compose.onNodeWithText("Page counter: 1").assertIsDisplayed()
            val restoredLabel = compose.onNodeWithText("apps page content").captureToImage().toPixelMap()
            assertTrue("Removing the wallpaper must restore the light theme's dark page text", (0 until restoredLabel.width).any { x ->
                (0 until restoredLabel.height).any { y -> restoredLabel[x, y].let { it.red < .1f && it.green < .1f && it.blue < .1f } }
            })
            captureUxScreenshot("page-wallpaper-removed-light")
        } finally { document.delete() }
    }

    private class WallpaperLifecycle : LifecycleOwner {
        val registry = LifecycleRegistry(this)
        override val lifecycle: Lifecycle get() = registry
    }
}
