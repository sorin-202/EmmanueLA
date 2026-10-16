package com.emmanuela.launcher.ui.folders

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.emmanuela.launcher.data.AppFolder
import com.emmanuela.launcher.data.FolderAppearance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A small shared cache bounds the total retained image memory across all folder tiles. */
internal object FolderImages {
    private val cache = object : LruCache<String, Bitmap>(12 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }

    suspend fun load(context: Context, value: String): Bitmap? = withContext(Dispatchers.IO) {
        if (value.isEmpty() || !FolderAppearance.validImageUri(value)) return@withContext null
        // Check access even for a cached image so a revoked/deleted document gets a fallback.
        try {
            val resolver = context.applicationContext.contentResolver
            val uri = Uri.parse(value)
            resolver.openAssetFileDescriptor(uri, "r")?.use { require(it.length <= 40L * 1024 * 1024) }
                ?: return@withContext null
            cache.get(value)?.let { return@withContext it }
            val bitmap = if (Build.VERSION.SDK_INT >= 28) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
                    val (width, height) = FolderAppearance.imageSize(info.size.width, info.size.height)
                    decoder.setTargetSize(width, height)
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                val (width, height) = FolderAppearance.imageSize(bounds.outWidth, bounds.outHeight)
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                    inSampleSize = 1
                    while (bounds.outWidth / inSampleSize > width || bounds.outHeight / inSampleSize > height) inSampleSize *= 2
                }
                val source = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                    ?: return@withContext null
                val orientation = resolver.openInputStream(uri)?.use {
                    ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                } ?: ExifInterface.ORIENTATION_NORMAL
                val matrix = Matrix().apply {
                    when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                        ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                        ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
                        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                        ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
                        ExifInterface.ORIENTATION_TRANSPOSE -> { postRotate(90f); postScale(-1f, 1f) }
                        ExifInterface.ORIENTATION_TRANSVERSE -> { postRotate(270f); postScale(-1f, 1f) }
                    }
                }
                Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true).also {
                    if (it !== source) source.recycle()
                }
            }
            cache.put(value, bitmap)
            bitmap
        } catch (_: Exception) { cache.remove(value); null }
        catch (_: OutOfMemoryError) { cache.evictAll(); null }
    }
}

@Composable
internal fun folderImage(uri: String): ImageBitmap? {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var resumed by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) resumed++ }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return produceState<ImageBitmap?>(null, uri, resumed) {
        value = FolderImages.load(context, uri)?.asImageBitmap()
    }.value
}

@Composable
internal fun folderForeground(folder: AppFolder, fill: Boolean): Color {
    if (folder.imageUri.isNotEmpty()) return Color.White
    val accent = if (folder.color == 0L) MaterialTheme.colorScheme.onSurface else Color(folder.color)
    return if (fill || folder.solidBackground) {
        if (accent.luminance() > .45f) Color.Black else Color.White
    } else MaterialTheme.colorScheme.onSurface
}

/** Uses a center crop at render time; the source document is never rewritten. */
@Composable
internal fun FolderSurface(folder: AppFolder, fill: Boolean, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val image = folderImage(folder.imageUri)
    val accent = if (folder.color == 0L) MaterialTheme.colorScheme.onSurface else Color(folder.color)
    val hasImage = folder.imageUri.isNotEmpty()
    Box(modifier.clipToBounds().border(1.dp, accent).background(
        if (hasImage) Color(0xff303030) else if (fill || folder.solidBackground) accent else Color.Transparent
    )) {
        if (image != null) {
            Image(image, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = .5f)))
        }
        content()
    }
}
