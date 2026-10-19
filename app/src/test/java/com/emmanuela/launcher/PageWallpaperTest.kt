package com.emmanuela.launcher

import com.emmanuela.launcher.data.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class PageWallpaperTest {
    private val home = "content://photos/home"
    private val appPhoto = "content://photos/apps"
    private val folderPhoto = "content://photos/folders"
    private val album = "content://photos/album"

    private fun fixture() = LauncherData(settings = Preferences(wallpapers = listOf(home), dailyWallpaper = true,
        note = "Keep my Home", ui = UiPreferences(v2 = V2Preferences(wallpaperAlbum = album,
            photoStyles = mapOf(home to PhotoStyle(x = .25f, zoom = 1.5f)),
            widgetPositions = mapOf("clock" to WidgetPlacement(12f, 40f))))),
        favorites = listOf(Favorite("com.example/.Main")),
        folders = listOf(AppFolder("one", "One", "folder", emptyList(), imageUri = folderPhoto)))

    @Test fun pageChangesAndRemovalPreserveOtherPageAndHome() {
        val original = fixture()
        val apps = PageWallpaper(appPhoto, PhotoStyle(zoom = 2f, dim = .6f))
        val folders = PageWallpaper(folderPhoto, PhotoStyle(x = -.5f, fit = "Fit"))
        val both = PageWallpapers.changed(PageWallpapers.changed(original, "apps", apps), "folders", folders)
        assertEquals(mapOf("apps" to apps, "folders" to folders), both.settings.ui.pageWallpapers)
        assertEquals(original, both.copy(settings = both.settings.copy(ui = both.settings.ui.copy(pageWallpapers = emptyMap()))))
        val removed = PageWallpapers.changed(both, "apps", null)
        assertEquals(mapOf("folders" to folders), removed.settings.ui.pageWallpapers)
        assertEquals(original.settings.wallpapers, removed.settings.wallpapers)
        assertEquals(original.settings.ui.v2.photoStyles, removed.settings.ui.v2.photoStyles)
        assertEquals(original.folders, removed.folders)
    }

    @Test fun fullConfigurationRoundTripKeepsIndependentPageCrops() {
        val original = fixture()
        val value = PageWallpapers.changed(PageWallpapers.changed(original, "apps",
            PageWallpaper(appPhoto, PhotoStyle(x = -.4f, y = .7f, zoom = 2.5f, dim = .2f, blur = 3f))), "folders",
            PageWallpaper(folderPhoto, PhotoStyle(x = .3f, y = -.8f, zoom = 1.1f, dim = .7f, blur = 12f, fit = "Fit")))
        val restored = ConfigurationCodec.decode(ConfigurationCodec.encode(value))
        assertEquals(value, restored)
        assertNotEquals(restored.settings.ui.pageWallpapers.getValue("apps").style,
            restored.settings.ui.pageWallpapers.getValue("folders").style)
        assertEquals(original.settings.ui.v2.photoStyles, restored.settings.ui.v2.photoStyles)
    }

    @Test fun legacyConfigurationsKeepOriginalPageBackgrounds() {
        val oldUi = UiPreferencesCodec.encode(UiPreferences()).apply { remove("pageWallpapers") }
        assertTrue(UiPreferencesCodec.decode(oldUi).pageWallpapers.isEmpty())
        val raw = JSONObject(ConfigurationCodec.encode(fixture()))
        raw.getJSONObject("settings").getJSONObject("ui").remove("pageWallpapers")
        val restored = ConfigurationCodec.decode(raw.toString())
        assertTrue(restored.settings.ui.pageWallpapers.isEmpty())
        assertEquals(fixture(), restored)
    }

    @Test fun unsupportedPagesCannotOverwriteHomeOrCreateExtraSurfaces() {
        listOf("home", "blank", "Apps", "").forEach { page ->
            assertThrows(IllegalArgumentException::class.java) {
                PageWallpapers.encode(mapOf(page to PageWallpaper(appPhoto)))
            }
            assertThrows(IllegalArgumentException::class.java) {
                PageWallpapers.decode(JSONObject().put(page, JSONObject().put("uri", appPhoto)))
            }
            assertThrows(IllegalArgumentException::class.java) { PageWallpapers.changed(fixture(), page, null) }
        }
    }

    @Test fun onlyNonemptyLocalDocumentUrisAreAcceptedForEitherPage() {
        listOf("", "https://example.org/photo.jpg", "http://example.org/photo.jpg", "file:///sdcard/photo.jpg",
            "javascript:alert(1)", "content:/missing-authority", "not a uri").forEach { uri ->
            assertThrows(IllegalArgumentException::class.java) {
                PageWallpapers.encode(mapOf("apps" to PageWallpaper(uri)))
            }
            assertThrows(IllegalArgumentException::class.java) {
                PageWallpapers.decode(JSONObject().put("folders", JSONObject().put("uri", uri)))
            }
        }
    }

    @Test fun invalidCropAndEffectsAreRejectedOnImportAndBeforeSaving() {
        val invalid = listOf(PhotoStyle(x = 1.01f), PhotoStyle(y = -1.01f), PhotoStyle(zoom = .99f),
            PhotoStyle(zoom = 3.01f), PhotoStyle(dim = -.01f), PhotoStyle(dim = 1.01f),
            PhotoStyle(blur = -.01f), PhotoStyle(blur = 30.01f), PhotoStyle(fit = "Stretch"))
        invalid.forEach { style ->
            assertThrows(IllegalArgumentException::class.java) {
                PageWallpapers.changed(fixture(), "apps", PageWallpaper(appPhoto, style))
            }
            val rawStyle = SurfaceStyleCodec.photos(mapOf(appPhoto to style)).getJSONObject(appPhoto)
            assertThrows(IllegalArgumentException::class.java) {
                PageWallpapers.decode(JSONObject().put("apps", JSONObject().put("uri", appPhoto).put("style", rawStyle)))
            }
        }
    }

    @Test fun portableExportRemovesPageImageGrantsAndPreservesLayoutAndFavorites() {
        val original = PageWallpapers.changed(PageWallpapers.changed(fixture(), "apps", PageWallpaper(appPhoto)),
            "folders", PageWallpaper(folderPhoto))
        val portable = ConfigurationCodec.decode(ConfigurationCodec.encode(ConfigurationCodec.portable(original)))
        assertTrue(portable.settings.ui.pageWallpapers.isEmpty())
        assertTrue(portable.settings.wallpapers.isEmpty())
        assertTrue(portable.settings.ui.v2.photoStyles.isEmpty())
        assertEquals("", portable.settings.ui.v2.wallpaperAlbum)
        assertEquals(original.settings.ui.v2.widgetPositions, portable.settings.ui.v2.widgetPositions)
        assertEquals(original.favorites, portable.favorites)
        assertEquals(original.folders.map { it.copy(imageUri = "") }, portable.folders)
        assertEquals(original.settings.note, portable.settings.note)
    }

    @Test fun grantReferencesIncludeBothPagesAndRetainSharedFolderAndHomeImages() {
        val original = PageWallpapers.changed(PageWallpapers.changed(fixture(), "apps", PageWallpaper(appPhoto)),
            "folders", PageWallpaper(folderPhoto))
        assertEquals(setOf(home, appPhoto, folderPhoto, album), FolderImageGrants.referenced(original))
        val withoutPage = PageWallpapers.changed(original, "folders", null)
        assertTrue("The folder still owns the shared cover grant", folderPhoto in FolderImageGrants.referenced(withoutPage))
        val homeShared = PageWallpapers.changed(withoutPage, "apps", PageWallpaper(home))
        assertEquals(setOf(home, folderPhoto, album), FolderImageGrants.referenced(homeShared))
        val noPages = PageWallpapers.changed(homeShared, "apps", null)
        assertEquals(setOf(home, folderPhoto, album), FolderImageGrants.referenced(noPages))
        assertFalse(appPhoto in FolderImageGrants.referenced(noPages))
    }
}
