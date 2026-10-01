package com.emmanuela.launcher.ui.navigation

enum class LauncherSurface { HOME, APPS, FOLDERS, BLANK }
enum class DrawerSwipe { LEFT, RIGHT }

/** Physical swipe directions; independent of locale, app order, and previous visits. */
object LauncherNavigation {
    fun destination(current: LauncherSurface, swipe: DrawerSwipe): LauncherSurface = when (current) {
        LauncherSurface.HOME, LauncherSurface.BLANK -> LauncherSurface.HOME // Home has its own configurable actions.
        LauncherSurface.APPS -> if (swipe == DrawerSwipe.LEFT) LauncherSurface.FOLDERS else LauncherSurface.HOME
        LauncherSurface.FOLDERS -> if (swipe == DrawerSwipe.LEFT) LauncherSurface.APPS else LauncherSurface.HOME
    }
}
