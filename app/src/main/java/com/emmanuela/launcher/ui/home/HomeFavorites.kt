package com.emmanuela.launcher.ui.home

import com.emmanuela.launcher.data.LaunchableApp
import com.emmanuela.launcher.data.LauncherData

/** The same ordered favorites feed every Home layout; launch authorization stays with the caller. */
internal data class HomeFavoriteItem(val id: String, val app: LaunchableApp?)

internal fun homeFavoriteItems(data: LauncherData, apps: List<LaunchableApp>): List<HomeFavoriteItem> {
    val installed = apps.associateBy { it.id }
    return data.favorites.asSequence()
        .filter { data.policies[it.id.substringBefore('/')]?.hidden != true }
        .take(data.settings.favoriteCount.coerceIn(0, 8))
        .map { HomeFavoriteItem(it.id, installed[it.id]) }
        .toList()
}
