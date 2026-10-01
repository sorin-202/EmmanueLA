package com.emmanuela.launcher.data

import org.json.JSONObject

/** Value-only configuration; no Bitmap, Context or mutable UI state is retained here. */
data class UiPreferences(
    val appListEnabled: Boolean = true,
    val searchPosition: String = "Top", val searchStyle: String = "Outline", val searchColor: Long = 0L,
    val tagSearch: Boolean = true, val tagSuggestions: Boolean = true,
    val alphabet: Boolean = true, val alphabetAnimation: String = "Wave",
    val tripleTapAction: String = "none", val italic: Boolean = false, val palette: String = "Neutral",
    val wallpaperBlur: Float = 0f,
    val widgetFont: String = "Inherit", val widgetColor: Long = 0L,
    val widgetBackground: Long = 0L, val widgetPosition: String = "Top", val widgetScale: Float = 1f,
    val clockAction: String = "clock", val dateAction: String = "calendar", val batteryAction: String = "battery",
    val screenTimeAction: String = "wellbeing", val screenTimeLimit: Int = 180,
    val leftShortcut: String = "apps", val rightShortcut: String = "settings",
    val leftSymbol: String = "↑", val rightSymbol: String = "⚙",
    val bottomShortcuts: Boolean = true, val bottomBarWidth: Float = 1f, val bottomBarTextScale: Float = 1f, val bottomBarOffset: Float = 0f,
    val bottomBarHeight: Float = 56f, val bottomBarPadding: Float = 12f, val bottomBarWeight: String = "Normal",
    val searchCursorColor: Long = 0L, val searchCursorWidth: Float = 2f, val searchCursorBlinkMs: Int = 500,
    val folderFill: Boolean = false, val folderLayout: String = "Adaptive", val folderAppIcons: Boolean = false,
    val v2: V2Preferences = V2Preferences(),
    val experience: ExperiencePreferences = ExperiencePreferences()
)
object UiPreferencesCodec {
    fun encode(p: UiPreferences) = JSONObject().apply {
        put("experience",ExperienceCodec.encode(p.experience))
        put("v2", V2Codec.encode(p.v2))
        put("appListEnabled", p.appListEnabled); put("searchPosition", p.searchPosition); put("searchStyle", p.searchStyle)
        put("searchColor", p.searchColor); put("tagSearch", p.tagSearch); put("tagSuggestions", p.tagSuggestions)
        put("alphabet", p.alphabet); put("alphabetAnimation", p.alphabetAnimation); put("tripleTapAction", p.tripleTapAction)
        put("italic", p.italic); put("palette", p.palette); put("wallpaperBlur", p.wallpaperBlur.toDouble())
        put("widgetFont", p.widgetFont); put("widgetColor", p.widgetColor); put("widgetBackground", p.widgetBackground)
        put("widgetPosition", p.widgetPosition); put("widgetScale", p.widgetScale.toDouble()); put("clockAction", p.clockAction)
        put("dateAction", p.dateAction); put("batteryAction", p.batteryAction); put("screenTimeAction", p.screenTimeAction)
        put("screenTimeLimit", p.screenTimeLimit); put("leftShortcut", p.leftShortcut); put("rightShortcut", p.rightShortcut)
        put("leftSymbol", p.leftSymbol); put("rightSymbol", p.rightSymbol); put("bottomShortcuts", p.bottomShortcuts)
        put("bottomBarWidth", p.bottomBarWidth.toDouble()); put("bottomBarTextScale", p.bottomBarTextScale.toDouble()); put("bottomBarOffset", p.bottomBarOffset.toDouble())
        put("bottomBarHeight", p.bottomBarHeight.toDouble()); put("bottomBarPadding", p.bottomBarPadding.toDouble()); put("bottomBarWeight", p.bottomBarWeight)
        put("searchCursorColor", p.searchCursorColor); put("searchCursorWidth", p.searchCursorWidth.toDouble()); put("searchCursorBlinkMs", p.searchCursorBlinkMs)
        put("folderFill", p.folderFill); put("folderLayout", p.folderLayout); put("folderAppIcons", p.folderAppIcons)
    }
    fun decode(o: JSONObject): UiPreferences {
        val d = UiPreferences()
        fun choice(k: String, default: String, options: List<String>) = o.optString(k, default).also { require(it in options) }
        fun action(k: String, default: String) = o.optString(k, default).also {
            require(it in listOf("none", "apps", "folders", "settings", "lock", "camera", "phone", "clock", "calendar", "battery", "usage", "wellbeing", "home", "search", "flashlight", "dnd", "airplane", "notifications") ||
                it.startsWith("app:") && it.contains('/'))
        }
        return UiPreferences(
            o.optBoolean("appListEnabled", true), choice("searchPosition", d.searchPosition, listOf("Top", "Bottom")),
            choice("searchStyle", d.searchStyle, listOf("Outline", "Filled", "Underline", "Pill", "Box", "Floating Outline", "Minimal")), o.optLong("searchColor"),
            o.optBoolean("tagSearch", true), o.optBoolean("tagSuggestions", true), o.optBoolean("alphabet", true),
            choice("alphabetAnimation", d.alphabetAnimation, listOf("Bubble", "Fade", "None", "Wave")), action("tripleTapAction", "none"),
            o.optBoolean("italic"), choice("palette", "Neutral", listOf("Neutral", "Sage", "Ocean", "Rose")),
            o.optDouble("wallpaperBlur", 0.0).toFloat().also { require(it in 0f..30f) },
            choice("widgetFont", "Inherit", listOf("Inherit", "Sans", "Serif", "Monospace")), o.optLong("widgetColor"), o.optLong("widgetBackground"),
            choice("widgetPosition", "Top", listOf("Top", "Center", "Bottom")),
            o.optDouble("widgetScale", 1.0).toFloat().also { require(it in .6f..1.5f) },
            action("clockAction", "clock"), action("dateAction", "calendar"), action("batteryAction", "battery"), action("screenTimeAction", "wellbeing"),
            o.optInt("screenTimeLimit", 180).also { require(it in 1..1440) }, action("leftShortcut", "apps"), action("rightShortcut", "settings"),
            o.optString("leftSymbol", "↑").also { require(it.length <= 8) }, o.optString("rightSymbol", "⚙").also { require(it.length <= 8) },
            o.optBoolean("bottomShortcuts", true),
            o.optDouble("bottomBarWidth", 1.0).toFloat().also { require(it in .5f..1f) },
            o.optDouble("bottomBarTextScale", 1.0).toFloat().also { require(it in .7f..1.8f) },
            o.optDouble("bottomBarOffset", 0.0).toFloat().also { require(it in 0f..120f) },
            o.optDouble("bottomBarHeight", 56.0).toFloat().also { require(it in 40f..120f) },
            o.optDouble("bottomBarPadding", 12.0).toFloat().also { require(it in 0f..40f) },
            choice("bottomBarWeight", "Normal", listOf("Light", "Normal", "Medium", "Bold")),
            o.optLong("searchCursorColor"),
            o.optDouble("searchCursorWidth", 2.0).toFloat().also { require(it in 1f..6f) },
            o.optInt("searchCursorBlinkMs", 500).also { require(it==0 || it in 250..1200) },
            o.optBoolean("folderFill"),
            choice("folderLayout", "Adaptive", listOf("Adaptive", "List", "Freeform", "Canvas")), o.optBoolean("folderAppIcons"),
            o.optJSONObject("v2")?.let(V2Codec::decode) ?: V2Preferences(),
            o.optJSONObject("experience")?.let(ExperienceCodec::decode) ?: ExperiencePreferences()
        )
    }
}
