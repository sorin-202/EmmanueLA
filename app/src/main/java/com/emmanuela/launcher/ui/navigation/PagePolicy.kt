package com.emmanuela.launcher.ui.navigation

import com.emmanuela.launcher.data.Preferences

object PagePolicy {
    fun enabled(p:Preferences)=buildList {
        if(p.ui.experience.homeEnabled)add(LauncherSurface.HOME)
        if(p.ui.appListEnabled && !p.ui.experience.homeAlphabet)add(LauncherSurface.APPS)
        if(p.ui.v2.foldersEnabled)add(LauncherSurface.FOLDERS)
    }
    fun resolve(requested:LauncherSurface,p:Preferences):LauncherSurface {
        val available=enabled(p)
        return if(requested in available)requested else available.firstOrNull()?:LauncherSurface.BLANK
    }
}
