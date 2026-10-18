package com.emmanuela.launcher

import com.emmanuela.launcher.data.*
import com.emmanuela.launcher.ui.home.homeFavoriteItems
import org.junit.Assert.*
import org.junit.Test

class HomeFavoritesTest {
    private val one = LaunchableApp("com.first/.Main", "First", "com.first")
    private val two = LaunchableApp("com.second/.Main", "Second", "com.second")
    private val three = LaunchableApp("com.third/.Main", "Third", "com.third")

    @Test fun favoritesKeepManualOrderRatherThanInstalledAppOrder() {
        val data = LauncherData(favorites = listOf(Favorite(three.id), Favorite(one.id), Favorite(two.id)))
        assertEquals(listOf(three, one, two), homeFavoriteItems(data, listOf(one, two, three)).map { it.app })
    }

    @Test fun hiddenFavoritesDoNotLeakOrConsumeVisibleSlots() {
        val data = LauncherData(settings = Preferences(favoriteCount = 2),
            favorites = listOf(Favorite(one.id), Favorite(two.id), Favorite(three.id)),
            policies = mapOf(one.packageName to AppPolicy(hidden = true)))
        assertEquals(listOf(two, three), homeFavoriteItems(data, listOf(one, two, three)).map { it.app })
        assertEquals(3, data.favorites.size) // Hiding is reversible and keeps the saved order.
    }

    @Test fun unavailableFavoriteKeepsItsEditableSlotAndBlockedPrivateAppsReachSecureLaunch() {
        val data = LauncherData(favorites = listOf(Favorite(three.id), Favorite(one.id), Favorite(two.id)),
            policies = mapOf(one.packageName to AppPolicy(blocked = true), two.packageName to AppPolicy(privateApp = true)))
        val items = homeFavoriteItems(data, listOf(one, two))
        assertEquals(three.id, items.first().id)
        assertNull(items.first().app)
        assertEquals(listOf(one, two), items.drop(1).map { it.app })
    }

    @Test fun zeroFavoritesShowsNoRowsInAnyLayout() {
        val data = LauncherData(settings = Preferences(favoriteCount = 0), favorites = listOf(Favorite(one.id)))
        assertTrue(homeFavoriteItems(data, listOf(one)).isEmpty())
    }
}
