package com.emmanuela.launcher

import com.emmanuela.launcher.ui.navigation.DrawerSwipe
import com.emmanuela.launcher.ui.navigation.LauncherNavigation
import com.emmanuela.launcher.ui.navigation.LauncherSurface


import org.junit.Assert.assertEquals
import org.junit.Test

class NavigationTest {
    @Test fun repeatedLeftSwipesLoopWithoutEverReachingHome() {
        var page = LauncherSurface.APPS
        repeat(1000) { index ->
            page = LauncherNavigation.destination(page, DrawerSwipe.LEFT)
            assertEquals(if (index % 2 == 0) LauncherSurface.FOLDERS else LauncherSurface.APPS, page)
        }
    }
    @Test fun rightFromEitherDrawerSurfaceAlwaysGoesHome() {
        for (page in listOf(LauncherSurface.APPS, LauncherSurface.FOLDERS)) {
            assertEquals(LauncherSurface.HOME, LauncherNavigation.destination(page, DrawerSwipe.RIGHT))
        }
    }
    @Test fun drawerRulesDoNotOverrideHomeGestures() {
        for (swipe in DrawerSwipe.entries) assertEquals(LauncherSurface.HOME, LauncherNavigation.destination(LauncherSurface.HOME, swipe))
    }
}
