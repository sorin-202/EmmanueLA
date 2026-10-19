package com.emmanuela.launcher.data

/** Shared package/website targeting and accounting for the background service. */
object FocusTargets {
    fun groups(groups:List<FocusGroup>,pkg:String,url:String):List<FocusGroup> = groups.filter {
        pkg in it.packages || (pkg in it.browsers && FocusWindows.matches(it,url))
    }
    fun usagePackages(group:FocusGroup,pkg:String):Set<String> =
        if(group.perApp) setOf(pkg) else group.packages+pkg
    fun used(group:FocusGroup,pkg:String,usage:Map<String,Long>?):Long? =
        usage?.let { values -> usagePackages(group,pkg).sumOf { values[it]?:0L } }
}
