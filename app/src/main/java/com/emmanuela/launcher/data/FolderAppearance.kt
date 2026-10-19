package com.emmanuela.launcher.data

import java.net.URI
import kotlin.math.min

/** Folder appearance accepts local document-provider images only, never a network URL. */
object FolderAppearance {
    fun validImageUri(value: String): Boolean = value.isEmpty() ||
        (value.length <= 4096 && runCatching {
            val uri = URI(value)
            uri.scheme == "content" && !uri.authority.isNullOrEmpty()
        }.getOrDefault(false))

    fun rgb(red: String, green: String, blue: String): Long? {
        val values = listOf(red, green, blue).map { part ->
            if (part.isBlank() || part.length > 3 || part.any { !it.isDigit() }) return null
            part.toIntOrNull()?.takeIf { it in 0..255 } ?: return null
        }
        return 0xff000000L or (values[0].toLong() shl 16) or (values[1].toLong() shl 8) or values[2].toLong()
    }

    /** Keep image decoding bounded even for a compressed image with huge source dimensions. */
    fun imageSize(width: Int, height: Int, maxEdge: Int = 1024): Pair<Int, Int> {
        require(width > 0 && height > 0 && width.toLong() * height <= 100_000_000L)
        require(maxEdge in 1..2048)
        val scale = min(1.0, maxEdge.toDouble() / maxOf(width, height))
        return (width * scale).toInt().coerceAtLeast(1) to (height * scale).toInt().coerceAtLeast(1)
    }
}

object FolderContentGrid {
    /** At least a 56dp touch target; large saved grids adapt when moved to a smaller screen. */
    fun columns(requested: Int, widthDp: Float): Int =
        requested.coerceIn(1, 8).coerceAtMost((widthDp / 56f).toInt().coerceAtLeast(1))

    fun rowHeight(rows: Int, viewportHeightDp: Float): Float =
        (viewportHeightDp / rows.coerceIn(1, 8)).coerceIn(88f, 144f)
}
