package com.emmanuela.launcher.data

import org.json.JSONObject
import org.json.JSONArray

data class WidgetPlacement(val x: Float, val y: Float)
data class V2Preferences(
    val widgetStyles:Map<String,WidgetStyle> = emptyMap(), val photoStyles:Map<String,PhotoStyle> = emptyMap(),
    val gesturesEnabled: Boolean = true, val holdAction: String = "settings",
    val foldersEnabled: Boolean = true, val searchEnabled: Boolean = true,
    val shortcutMode: String = "Text", val leftVector: String = "apps", val rightVector: String = "settings",
    val widgetGrid: Int = 16, val widgetPositions: Map<String, WidgetPlacement> = emptyMap(),
    val locationWidgetEnabled: Boolean = false,
    val weatherEnabled: Boolean = false, val latitude: String = "", val longitude: String = "", val weatherAction: String = "none",
    val wallpaperAlbum: String = "", val wallpaperSwitchMinute: Int = 0,
    val cropX: Float = 0f, val cropY: Float = 0f, val cropZoom: Float = 1f,
    val customBackground: Long = 0xFF101018, val customForeground: Long = 0xFFF6F0E8,
    val notificationFilter: Boolean = true, val showBadges: Boolean = false,
    val recentTags: List<String> = emptyList(), val folderSnap: Int = 16
)
object V2Codec {
    fun encode(p: V2Preferences) = JSONObject().apply {
        put("widgetStyles",SurfaceStyleCodec.widgets(p.widgetStyles));put("photoStyles",SurfaceStyleCodec.photos(p.photoStyles))
        put("gesturesEnabled",p.gesturesEnabled);put("holdAction",p.holdAction);put("foldersEnabled",p.foldersEnabled);put("searchEnabled",p.searchEnabled)
        put("shortcutMode",p.shortcutMode);put("leftVector",p.leftVector);put("rightVector",p.rightVector);put("widgetGrid",p.widgetGrid)
        put("widgetPositions",JSONObject().apply { p.widgetPositions.forEach { (key,v) -> put(key,JSONObject().put("x",v.x.toDouble()).put("y",v.y.toDouble())) } })
        put("locationWidgetEnabled",p.locationWidgetEnabled); put("weatherEnabled",p.weatherEnabled);put("latitude",p.latitude);put("longitude",p.longitude);put("weatherAction",p.weatherAction)
        put("wallpaperAlbum",p.wallpaperAlbum);put("wallpaperSwitchMinute",p.wallpaperSwitchMinute)
        put("cropX",p.cropX.toDouble());put("cropY",p.cropY.toDouble());put("cropZoom",p.cropZoom.toDouble())
        put("customBackground",p.customBackground);put("customForeground",p.customForeground);put("notificationFilter",p.notificationFilter);put("showBadges",p.showBadges)
        put("recentTags",JSONArray(p.recentTags));put("folderSnap",p.folderSnap)
    }
    fun decode(o: JSONObject): V2Preferences {
        val d=V2Preferences()
        fun number(k:String, default:Float, min:Float,max:Float)=o.optDouble(k,default.toDouble()).toFloat().also { require(it in min..max) }
        fun action(k:String,default:String)=o.optString(k,default).also { require(it in listOf("weather-web","none","apps","folders","settings","lock","camera","phone","clock","calendar","battery","usage","wellbeing","home","search","flashlight","dnd","airplane","notifications") || (it.startsWith("app:") && it.contains('/'))) }
        val positions=o.optJSONObject("widgetPositions")?:JSONObject()
        require(positions.length()<=6)
        val map=positions.keys().asSequence().associateWith { key ->
            require(key in listOf("clock","date","battery","usage","weather","location"))
            val item=positions.getJSONObject(key)
            WidgetPlacement(item.getDouble("x").toFloat(),item.getDouble("y").toFloat()).also { require(it.x in 0f..5000f && it.y in 0f..5000f) }
        }
        val tags=o.optJSONArray("recentTags")?:JSONArray();require(tags.length()<=3)
        return V2Preferences(
            widgetStyles=SurfaceStyleCodec.readWidgets(o.optJSONObject("widgetStyles")?:JSONObject()),photoStyles=SurfaceStyleCodec.readPhotos(o.optJSONObject("photoStyles")?:JSONObject()),
            gesturesEnabled=o.optBoolean("gesturesEnabled",true),holdAction=action("holdAction","settings"),foldersEnabled=o.optBoolean("foldersEnabled",true),searchEnabled=o.optBoolean("searchEnabled",true),
            shortcutMode=o.optString("shortcutMode","Text").also{require(it in listOf("Text","Icon"))},leftVector=o.optString("leftVector","apps"),rightVector=o.optString("rightVector","settings"),
            widgetGrid=o.optInt("widgetGrid",16).also{require(it in 4..48)},widgetPositions=map,
            locationWidgetEnabled=o.optBoolean("locationWidgetEnabled"), weatherEnabled=o.optBoolean("weatherEnabled"),latitude=o.optString("latitude").also{require(it.isEmpty() || it.toDoubleOrNull()?.let{n->n in -90.0..90.0}==true)},
            longitude=o.optString("longitude").also{require(it.isEmpty() || it.toDoubleOrNull()?.let{n->n in -180.0..180.0}==true)},weatherAction=action("weatherAction","none"),
            wallpaperAlbum=o.optString("wallpaperAlbum").also{require(it.isEmpty()||it.startsWith("content://"))},wallpaperSwitchMinute=o.optInt("wallpaperSwitchMinute").also{require(it in 0..1439)},
            cropX=number("cropX",0f,-1f,1f),cropY=number("cropY",0f,-1f,1f),cropZoom=number("cropZoom",1f,1f,3f),
            customBackground=o.optLong("customBackground",d.customBackground),customForeground=o.optLong("customForeground",d.customForeground),
            notificationFilter=o.optBoolean("notificationFilter",true),showBadges=o.optBoolean("showBadges"),recentTags=List(tags.length()){tags.getString(it)},
            folderSnap=o.optInt("folderSnap",16).also{require(it in 4..48)}
        )
    }
}
