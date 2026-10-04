package com.emmanuela.launcher.ui.navigation

import com.emmanuela.launcher.data.Preferences

object PagePolicy {
    fun enabled(p:Preferences)=buildList {
        if(p.ui.experience.homeEnabled)add(LauncherSurface.HOME)
        if(p.ui.appListEnabled)add(LauncherSurface.APPS)
        if(p.ui.v2.foldersEnabled)add(LauncherSurface.FOLDERS)
    }
    /** Choose an enabled drawer before falling back to Home or the blank screen. */
    fun drawer(p:Preferences,preferred:LauncherSurface=LauncherSurface.APPS):LauncherSurface {
        val available=enabled(p)
        val alternate=if(preferred==LauncherSurface.FOLDERS)LauncherSurface.APPS else LauncherSurface.FOLDERS
        return when {
            preferred in available -> preferred
            alternate in available -> alternate
            LauncherSurface.HOME in available -> LauncherSurface.HOME
            else -> LauncherSurface.BLANK
        }
    }
    fun resolve(requested:LauncherSurface,p:Preferences):LauncherSurface {
        if(requested==LauncherSurface.APPS||requested==LauncherSurface.FOLDERS)return drawer(p,requested)
        val available=enabled(p)
        return if(requested in available)requested else available.firstOrNull()?:LauncherSurface.BLANK
    }
}
