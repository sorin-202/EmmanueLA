package com.emmanuela.launcher

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.emmanuela.launcher.data.*
import com.emmanuela.launcher.ui.folders.FolderImages
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FolderAppearancePersistenceTest {
    @Test fun colorImageAndCustomGridPersistThroughRepositoryRecreation() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = ConfigurationRepository(context)
        val original = repository.data.first()
        val folder = AppFolder("appearance-test", "Appearance", "folder", listOf("test/.B", "test/.A"),
            color = 0xff123456L, imageUri = "content://${context.packageName}.files/shared/folder-test.png",
            gridColumns = 5, gridRows = 4, widthUnits = 5, heightUnits = 5, manualOrder = true)
        try {
            repository.update { it.copy(folders = it.folders.filterNot { f -> f.id == folder.id } + folder) }
            assertEquals(folder, ConfigurationRepository(context).data.first().folders.first { it.id == folder.id })
        } finally { repository.restore(original) }
    }

    @Test fun actualAndroidImageDecodingIsBoundedAndDeletedDocumentFallsBack() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.cacheDir, "shared").apply { mkdirs() }
        val document = File(directory, "folder-image-test.png")
        try {
            val source = Bitmap.createBitmap(2048, 1024, Bitmap.Config.ARGB_8888)
            source.eraseColor(Color.RED)
            document.outputStream().use { assertTrue(source.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            source.recycle()
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", document).toString()
            val decoded = requireNotNull(FolderImages.load(context, uri))
            assertTrue(decoded.width <= 1024)
            assertTrue(decoded.height <= 1024)
            assertEquals(2f, decoded.width.toFloat() / decoded.height, .01f)
            assertTrue(document.delete())
            assertNull(FolderImages.load(context, uri))
        } finally { document.delete() }
    }

    @Test fun invalidImageAndRevokedProviderDoNotCrashOrKeepAStaleImage() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val document = File(File(context.cacheDir, "shared").apply { mkdirs() }, "not-an-image.png")
        try {
            document.writeText("This is not an image")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", document).toString()
            assertNull(FolderImages.load(context, uri))
            assertNull(FolderImages.load(context, "content://unavailable.provider/deleted"))
            assertNull(FolderImages.load(context, "https://example.org/no-network-image"))
        } finally { document.delete() }
    }
}
