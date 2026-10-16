package com.emmanuela.launcher

import com.emmanuela.launcher.data.*
import org.junit.Assert.*
import org.junit.Test

class FolderAppearanceTest {
    @Test fun rgbValidatesEachChannelAndCreatesOpaqueColor() {
        assertEquals(0xff00ff80L, FolderAppearance.rgb("0", "255", "128"))
        assertEquals(0xff000000L, FolderAppearance.rgb("000", "0", "0"))
        listOf("", "-1", "256", "1.5", " 2", "1e2", "9999").forEach { bad ->
            assertNull(FolderAppearance.rgb(bad, "0", "0"))
            assertNull(FolderAppearance.rgb("0", bad, "0"))
            assertNull(FolderAppearance.rgb("0", "0", bad))
        }
    }

    @Test fun appearanceIsIndependentPerFolderAndRoundTrips() {
        val folders = listOf(
            AppFolder("a", "Solid", "folder", emptyList(), color = 0xff123456L, solidBackground = true),
            AppFolder("b", "Photo", "work", emptyList(), color = 0xffabcdefL, imageUri = "content://photos/document/one")
        )
        assertEquals(folders, FolderCodec.decode(FolderCodec.encode(folders)))
    }

    @Test fun imageDocumentsAreLocalAndNoExecutableOrNetworkSourceIsAccepted() {
        assertTrue(FolderAppearance.validImageUri(""))
        assertTrue(FolderAppearance.validImageUri("content://media/external/images/1"))
        listOf("https://example.org/photo", "file:///etc/passwd", "javascript:alert(1)", "content:/missing-authority", "broken uri").forEach {
            assertFalse(FolderAppearance.validImageUri(it))
        }
    }

    @Test fun imageGrantCleanupPreservesSharedFoldersWallpapersAndAlbums() {
        val state = LauncherData(settings = Preferences(wallpapers = listOf("content://photos/wallpaper"),
            ui = UiPreferences(v2 = V2Preferences(wallpaperAlbum = "content://photos/album"))),
            folders = listOf(AppFolder("a", "A", "folder", emptyList(), imageUri = "content://photos/shared"),
                AppFolder("b", "B", "folder", emptyList(), imageUri = "content://photos/shared")))
        assertEquals(setOf("content://photos/shared", "content://photos/wallpaper", "content://photos/album"), FolderImageGrants.referenced(state))
    }

    @Test fun decoderBoundsDoNotUpscaleAndPreserveAspectRatio() {
        assertEquals(1024 to 512, FolderAppearance.imageSize(8000, 4000))
        assertEquals(512 to 1024, FolderAppearance.imageSize(4000, 8000))
        assertEquals(100 to 50, FolderAppearance.imageSize(100, 50))
    }

    @Test(expected = IllegalArgumentException::class)
    fun decompressionBombIsRejectedBeforeAllocatingBitmap() { FolderAppearance.imageSize(100_000, 100_000) }

    @Test(expected = IllegalArgumentException::class)
    fun missingDecoderDimensionsAreRejected() { FolderAppearance.imageSize(-1, -1) }

    @Test(expected = IllegalArgumentException::class)
    fun networkImageInPersistedConfigurationIsRejected() {
        FolderCodec.decode(FolderCodec.encode(listOf(AppFolder("a", "A", "folder", emptyList(), imageUri = "https://example.org/photo"))))
    }
}
