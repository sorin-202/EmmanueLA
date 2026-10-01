package com.emmanuela.launcher.platform

import com.emmanuela.launcher.data.LaunchableApp


import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow

/** Ephemeral package counts only. No notification text persists. */
object NotificationBadges {
    private val keys=linkedMapOf<String,String>()
    val counts=MutableStateFlow<Map<String,Int>>(emptyMap())
    val muted=MutableStateFlow<Set<String>>(emptySet())
    val enabled=MutableStateFlow(false)
    @Synchronized fun posted(key:String,pkg:String){keys[key]=pkg;if(keys.size>1000)keys.remove(keys.keys.first());publish()}
    @Synchronized fun removed(key:String){keys.remove(key);publish()}
    @Synchronized fun clear(){keys.clear();publish()}
    private fun publish(){counts.value=keys.values.groupingBy{it}.eachCount()}
}
@Composable
fun badgeLabel(app:LaunchableApp):String {
    val enabled by NotificationBadges.enabled.collectAsStateWithLifecycle()
    val muted by NotificationBadges.muted.collectAsStateWithLifecycle()
    val counts by NotificationBadges.counts.collectAsStateWithLifecycle()
    val count=if(enabled&&app.packageName !in muted)counts[app.packageName]?:0 else 0
    return if(count>0)"${app.label} · $count" else app.label
}
