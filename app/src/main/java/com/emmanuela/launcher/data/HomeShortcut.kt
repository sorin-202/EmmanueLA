package com.emmanuela.launcher.data

import org.json.JSONArray
import org.json.JSONObject

/** A launcher activity, not a pin request or arbitrary intent supplied by another app. */
data class HomeShortcut(val appId: String, val position: WidgetPlacement)

object HomeShortcuts {
    const val MAX_COUNT = 64
    fun visible(appId: String, policies: Map<String, AppPolicy>) = policies[appId.substringBefore('/')]?.hidden != true

    fun candidates(apps: List<LaunchableApp>, saved: List<HomeShortcut>, policies: Map<String, AppPolicy>) =
        apps.filter { app -> visible(app.id, policies) && saved.none { it.appId == app.id } }

    fun availablePosition(occupied: List<WidgetPlacement>, width: Float, height: Float): WidgetPlacement {
        val columns=(width/84f).toInt().coerceAtLeast(1)
        val startY=280f.coerceAtMost((height-90f).coerceAtLeast(0f))
        val rows=((height-startY)/90f).toInt().coerceAtLeast(1)
        val candidates=(0 until columns*rows).asSequence().map{index->
            HomePlacement.constrain(WidgetPlacement(8f+(index%columns)*84f,startY+(index/columns)*90f),width-76f,height-90f)
        }.toList()
        return candidates.firstOrNull{candidate->occupied.none{kotlin.math.abs(it.x-candidate.x)<76f&&kotlin.math.abs(it.y-candidate.y)<84f}}?:candidates.first()
    }
}

object HomeShortcutCodec {
    fun encode(items: List<HomeShortcut>) = JSONArray().apply {
        items.forEach { item -> put(JSONObject().put("appId", item.appId)
            .put("x", item.position.x.toDouble()).put("y", item.position.y.toDouble())) }
    }

    fun decode(array: JSONArray): List<HomeShortcut> {
        require(array.length() <= HomeShortcuts.MAX_COUNT) { "Too many Home shortcuts" }
        val items = List(array.length()) { index ->
            val row = array.getJSONObject(index)
            val id = row.getString("appId")
            require(id.length in 3..512 && id.count { it == '/' } == 1 && !id.startsWith('/') && !id.endsWith('/') && id.none { it.isWhitespace() || it.isISOControl() }) { "Invalid launcher activity" }
            val x = row.getDouble("x").toFloat()
            val y = row.getDouble("y").toFloat()
            require(x in 0f..5000f && y in 0f..5000f) { "Invalid Home shortcut position" }
            HomeShortcut(id, WidgetPlacement(x, y))
        }
        require(items.map { it.appId }.distinct().size == items.size) { "Duplicate Home shortcut" }
        return items
    }
}
