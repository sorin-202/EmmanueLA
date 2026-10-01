package com.emmanuela.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private val Context.emaStore by preferencesDataStore(name = "ema_configuration")
data class Favorite(val id: String)
data class Preferences(
    val theme: String = "Dark", val bold: Boolean = false, val textScale: Float = 1f,
    val font: String = "Sans", val customFont: String = "", val notificationBar: Boolean = true,
    val showClock: Boolean = true, val showDate: Boolean = true, val showBattery: Boolean = true,
    val showScreenTime: Boolean = false, val alignment: String = "Right", val favoriteCount: Int = 6,
    val autoKeyboard: Boolean = false, val dailyWallpaper: Boolean = false,
    val wallpapers: List<String> = emptyList(), val dim: Float = .45f,
    val swipeUp: String = "apps", val swipeDown: String = "none",
    val swipeLeft: String = "camera", val swipeRight: String = "phone", val doubleTapAction: String = "none",
    val tileSize: Int = 150, val symbolSize: Int = 24, val symbolPlacement: String = "Top",
    val searchAccent: Boolean = false, val note: String = "", val ui: UiPreferences = UiPreferences()
)
data class LauncherData(val settings: Preferences = Preferences(), val favorites: List<Favorite> = emptyList(), val folders: List<AppFolder> = emptyList(), val appMetadata: Map<String, AppMetadata> = emptyMap(), val policies: Map<String, AppPolicy> = emptyMap(), val focusGroups: List<FocusGroup> = emptyList())

object ConfigurationCodec {
    fun encode(data: LauncherData): String = JSONObject().apply {
        put("version", 7)
        put("folders", JSONObject(FolderCodec.encode(data.folders)).getJSONArray("folders"))
        put("favorites", JSONArray().apply { data.favorites.forEach { put(JSONObject().put("id", it.id)) } })
        put("appMetadata", JSONObject().apply {
            data.appMetadata.toSortedMap().forEach { (id, metadata) ->
                put(id, JSONObject().put("alias", metadata.alias).put("tags", JSONArray(metadata.tags)))
            }
        })
        put("focusGroups",FocusCodec.encode(data.focusGroups))
        put("policies", PolicyCodec.encode(data.policies))
        val p = data.settings
        put("settings", JSONObject().apply {
            put("theme", p.theme); put("bold", p.bold); put("textScale", p.textScale.toDouble())
            put("font", p.font); put("customFont", p.customFont); put("notificationBar", p.notificationBar)
            put("showClock", p.showClock); put("showDate", p.showDate); put("showBattery", p.showBattery)
            put("showScreenTime", p.showScreenTime); put("alignment", p.alignment); put("favoriteCount", p.favoriteCount)
            put("autoKeyboard", p.autoKeyboard); put("dailyWallpaper", p.dailyWallpaper); put("wallpapers", JSONArray(p.wallpapers))
            put("dim", p.dim.toDouble()); put("swipeUp", p.swipeUp); put("swipeDown", p.swipeDown)
            put("swipeLeft", p.swipeLeft); put("swipeRight", p.swipeRight); put("doubleTapAction", p.doubleTapAction)
            put("tileSize", p.tileSize); put("symbolSize", p.symbolSize); put("symbolPlacement", p.symbolPlacement)
            put("searchAccent", p.searchAccent); put("note", p.note); put("ui", UiPreferencesCodec.encode(p.ui))
        })
    }.toString(2)
    fun decode(raw: String): LauncherData {
        require(raw.toByteArray(Charsets.UTF_8).size <= 2_000_000) { "Backup too large" }
        val root = JSONObject(raw)
        val version = root.getInt("version")
        require(version in 2..7) { "Unsupported backup version" }
        val s = root.getJSONObject("settings")
        fun choice(key: String, default: String, values: List<String>): String = s.optString(key, default).also { require(it in values) }
        fun action(key: String, default: String) = s.optString(key, default).also {
            require(it in listOf("none", "apps", "folders", "settings", "lock", "camera", "phone", "clock", "calendar", "battery", "usage", "wellbeing", "home", "search", "flashlight", "dnd", "airplane", "notifications") ||
                (it.startsWith("app:") && it.substringAfter("app:").contains('/')))
        }
        val walls = s.optJSONArray("wallpapers") ?: JSONArray()
        require(walls.length() <= 50)
        val prefs = Preferences(
            theme = choice("theme", "Dark", listOf("Dark", "Light", "System", "Custom")), bold = s.optBoolean("bold"),
            textScale = s.optDouble("textScale", 1.0).toFloat().also { require(it in .7f..1.5f) },
            font = choice("font", "Sans", listOf("Sans", "Serif", "Monospace", "Custom")), customFont = s.optString("customFont"),
            notificationBar = s.optBoolean("notificationBar", true), showClock = s.optBoolean("showClock", true),
            showDate = s.optBoolean("showDate", true), showBattery = s.optBoolean("showBattery", true),
            showScreenTime = s.optBoolean("showScreenTime"), alignment = choice("alignment", "Right", listOf("Left", "Center", "Right")),
            favoriteCount = s.optInt("favoriteCount", 6).also { require(it in 0..8) }, autoKeyboard = s.optBoolean("autoKeyboard"),
            dailyWallpaper = s.optBoolean("dailyWallpaper"), wallpapers = List(walls.length()) { walls.getString(it).also { uri -> require(uri.startsWith("content://")) } },
            dim = s.optDouble("dim", .45).toFloat().also { require(it in 0f..1f) },
            swipeUp = action("swipeUp", "apps"), swipeDown = action("swipeDown", "none"), swipeLeft = action("swipeLeft", "camera"),
            swipeRight = action("swipeRight", "phone"),
            doubleTapAction = if (version == 2) { if (s.optBoolean("doubleTap")) "lock" else "none" } else action("doubleTapAction", "none"),
            tileSize = s.optInt("tileSize", 150).also { require(it in 110..240) },
            symbolSize = s.optInt("symbolSize", 24).also { require(it in 16..48) },
            symbolPlacement = choice("symbolPlacement", "Top", listOf("Top", "Left", "Hidden")),
            searchAccent = s.optBoolean("searchAccent"), note = s.optString("note").also { require(it.length <= 10000) },
            ui = s.optJSONObject("ui")?.let(UiPreferencesCodec::decode) ?: UiPreferences()
        )
        val f = root.getJSONArray("favorites")
        require(f.length() <= 8)
        val favorites = List(f.length()) { i -> f.getJSONObject(i).let { Favorite(it.getString("id")) } }
        require(favorites.map { it.id }.distinct().size == favorites.size)
        require(favorites.all { it.id.contains('/') && it.id.length <= 512 })
        val folderArray = root.getJSONArray("folders")
        require(folderArray.length() <= 200)
        val folders = FolderCodec.decode(JSONObject().put("version", 1).put("folders", folderArray).toString())
        require(folders.all { it.name.isNotBlank() && it.name.length <= 40 && it.apps.size <= 2000 && it.apps.all { app -> app.contains('/') } })
        val metadata = linkedMapOf<String, AppMetadata>()
        if (version == 2) {
            // Earlier favorite-only labels become global aliases without losing the user's names.
            for (index in 0 until f.length()) {
                val favorite = f.getJSONObject(index)
                val alias = AppNaming.alias(favorite.optString("label"))
                if (alias.isNotEmpty()) metadata[favorite.getString("id")] = AppMetadata(alias)
            }
        } else {
            val saved = root.getJSONObject("appMetadata")
            require(saved.length() <= 5000) { "Too many app metadata entries" }
            saved.keys().forEach { id ->
                require(id.contains('/') && id.length <= 512)
                val item = saved.getJSONObject(id)
                val tags = item.getJSONArray("tags")
                require(tags.length() <= AppNaming.MAX_TAGS)
                val values = List(tags.length()) { tags.getString(it) }
                val normalized = AppNaming.metadata(item.getString("alias"), values.joinToString(" "))
                require(normalized.tags == values) { "Tags must be normalized and unique" }
                metadata[id] = normalized
            }
        }
        val policies=if(version>=4)PolicyCodec.decode(root.getJSONObject("policies"))else emptyMap()
        val migrated=if(version<7&&policies.values.any{it.notifications==NotificationMode.DIGEST})prefs.copy(ui=prefs.ui.copy(experience=prefs.ui.experience.copy(digestEnabled=true)))else prefs
        return LauncherData(migrated,favorites,folders,metadata,policies,FocusCodec.decode(root.optJSONArray("focusGroups")?:JSONArray()))
    }
    // Media grants and font files cannot travel between devices in a JSON backup.
    fun portable(data: LauncherData) = data.copy(settings = data.settings.copy(wallpapers = emptyList(), customFont = "", font = if (data.settings.font == "Custom") "Sans" else data.settings.font, dailyWallpaper = false, ui = data.settings.ui.copy(v2=data.settings.ui.v2.copy(wallpaperAlbum="",photoStyles=emptyMap()))))
}
class ConfigurationRepository(private val context: Context) {
    private val store = context.applicationContext.emaStore
    private val key = stringPreferencesKey("document")
    val data = store.data.map { it[key]?.let(ConfigurationCodec::decode) ?: LauncherData() }.flowOn(Dispatchers.IO)
    suspend fun migrate() {
        if (store.data.first()[key] == null) {
            val legacy = FolderRepository(context).folders.first()
            store.edit { if (it[key] == null) it[key] = ConfigurationCodec.encode(LauncherData(folders = legacy)) }
        }
    }
    suspend fun update(transform: (LauncherData) -> LauncherData) = withContext(Dispatchers.IO) {
        store.edit { prefs ->
            val current = prefs[key]?.let(ConfigurationCodec::decode) ?: LauncherData()
            val encoded = ConfigurationCodec.encode(transform(current))
            ConfigurationCodec.decode(encoded) // Reject invalid state before committing it.
            prefs[key] = encoded
        }
    }
    suspend fun restore(data: LauncherData) = withContext(Dispatchers.IO) {
        val encoded = ConfigurationCodec.encode(data)
        ConfigurationCodec.decode(encoded)
        store.edit { it[key] = encoded }
    }
}
