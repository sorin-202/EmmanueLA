package com.emmanuela.launcher.data

import org.json.JSONObject

/** Optional local backgrounds. Missing entries preserve each page's original theme. */
data class PageWallpaper(val uri: String, val style: PhotoStyle = PhotoStyle())

object PageWallpapers {
    val pages = setOf("apps", "folders")

    fun encode(values: Map<String, PageWallpaper>): JSONObject = JSONObject().apply {
        require(values.size <= pages.size)
        values.forEach { (page, wallpaper) ->
            require(page in pages)
            require(wallpaper.uri.isNotEmpty() && FolderAppearance.validImageUri(wallpaper.uri))
            val style = SurfaceStyleCodec.photos(mapOf(wallpaper.uri to wallpaper.style))
            // Apply the same bounded crop/dim/blur validation used for Home photos.
            SurfaceStyleCodec.readPhotos(style)
            put(page, JSONObject().put("uri", wallpaper.uri).put("style", style.getJSONObject(wallpaper.uri)))
        }
    }

    fun decode(value: JSONObject): Map<String, PageWallpaper> {
        require(value.length() <= pages.size)
        return value.keys().asSequence().associateWith { page ->
            require(page in pages)
            val item = value.getJSONObject(page)
            val uri = item.getString("uri")
            require(uri.isNotEmpty() && FolderAppearance.validImageUri(uri))
            val styles = JSONObject().put(uri, item.optJSONObject("style") ?: JSONObject())
            PageWallpaper(uri, SurfaceStyleCodec.readPhotos(styles).getValue(uri))
        }
    }

    fun changed(state: LauncherData, page: String, wallpaper: PageWallpaper?): LauncherData {
        require(page in pages)
        wallpaper?.let { encode(mapOf(page to it)) }
        val previous = state.settings.ui.pageWallpapers
        val next = if (wallpaper == null) previous - page else previous + (page to wallpaper)
        return state.copy(settings = state.settings.copy(ui = state.settings.ui.copy(pageWallpapers = next)))
    }
}
