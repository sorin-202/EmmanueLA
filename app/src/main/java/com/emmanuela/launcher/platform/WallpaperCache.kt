package com.emmanuela.launcher.platform

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** One sampled immutable bitmap. No Activity or full-size source image is retained. */
object WallpaperCache {
    private val lock = Mutex()
    private var key: String? = null
    private var bitmap: ImageBitmap? = null
    suspend fun load(context: Context, uri: String?): ImageBitmap? = withContext(Dispatchers.IO) {
        lock.withLock {
            if (uri == key) return@withLock bitmap
            val image = uri?.let { runCatching {
                val resolver = context.applicationContext.contentResolver
                val source = Uri.parse(it)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(source)?.use { stream -> BitmapFactory.decodeStream(stream, null, bounds) }
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = android.graphics.Bitmap.Config.HARDWARE
                    inSampleSize = 1
                    while (bounds.outWidth / inSampleSize > 1280 || bounds.outHeight / inSampleSize > 1920) inSampleSize *= 2
                }
                resolver.openInputStream(source)?.use { stream -> BitmapFactory.decodeStream(stream, null, options)?.asImageBitmap() }
            }.getOrNull() }
            key = uri; bitmap = image; image
        }
    }
}
