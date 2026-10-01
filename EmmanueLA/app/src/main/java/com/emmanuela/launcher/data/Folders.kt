package com.emmanuela.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import org.json.JSONArray
import org.json.JSONObject

private val Context.folderDataStore by preferencesDataStore(name = "folders_v1")

data class AppFolder(val id: String, val name: String, val icon: String, val apps: List<String>, val color: Long = 0L, val passwordSalt: String = "", val passwordHash: String = "", val gridCell: Int? = null, val layout:String="Inherit", val widthDp:Float=0f,val heightDp:Float=150f,val manualOrder:Boolean=false)

object FolderCodec {
    fun encode(folders: List<AppFolder>): String = JSONObject().apply {
        put("version", 1)
        put("folders", JSONArray().apply {
            folders.forEach { folder -> put(JSONObject().apply {
                put("manualOrder",folder.manualOrder);put("widthDp",folder.widthDp.toDouble());put("heightDp",folder.heightDp.toDouble());put("layout",folder.layout);put("id", folder.id); put("name", folder.name); put("icon", folder.icon)
                put("color", folder.color); put("passwordSalt", folder.passwordSalt); put("passwordHash", folder.passwordHash); put("gridCell", folder.gridCell ?: JSONObject.NULL)
                put("apps", JSONArray(folder.apps.distinct()))
            }) }
        })
    }.toString()

    fun decode(raw: String?): List<AppFolder> {
        if (raw == null) return emptyList()
        val root = JSONObject(raw)
        require(root.getInt("version") == 1) { "Unsupported folder version" }
        val array = root.getJSONArray("folders")
        return List(array.length()) { i ->
            val obj = array.getJSONObject(i)
            val apps = obj.getJSONArray("apps")
            AppFolder(obj.getString("id"), obj.getString("name"), obj.getString("icon"),
                List(apps.length()) { apps.getString(it) }.distinct(), obj.optLong("color", 0L),
                obj.optString("passwordSalt").also { require(it.isEmpty() || it.length == 24) },
                obj.optString("passwordHash").also { require(it.isEmpty() || it.length == 44) },
                if (obj.isNull("gridCell")) null else obj.getInt("gridCell").also { require(it in 0..2000) },obj.optString("layout","Inherit").also{require(it in listOf("Inherit","Text","Icons","Grid"))},
                obj.optDouble("widthDp",0.0).toFloat().also{require(it==0f || it in 100f..360f)},obj.optDouble("heightDp",150.0).toFloat().also{require(it in 100f..360f)},obj.optBoolean("manualOrder"))
                .also { require(it.passwordSalt.isEmpty() == it.passwordHash.isEmpty()) }
        }.also { folders -> require(folders.map { it.id }.distinct().size == folders.size) }
    }
}

class FolderRepository(context: Context) {
    private val store = context.applicationContext.folderDataStore
    private val key = stringPreferencesKey("configuration")
    val folders = store.data.map { FolderCodec.decode(it[key]) }.flowOn(Dispatchers.IO)
    suspend fun save(folder: AppFolder) {
        require(folder.name.isNotBlank() && folder.name.length <= 40)
        store.edit { preferences ->
            val existing = FolderCodec.decode(preferences[key])
            val updated = if (existing.any { it.id == folder.id })
                existing.map { if (it.id == folder.id) folder else it } else existing + folder
            preferences[key] = FolderCodec.encode(updated)
        }
    }
    suspend fun delete(id: String) {
        store.edit { preferences ->
            preferences[key] = FolderCodec.encode(FolderCodec.decode(preferences[key]).filterNot { it.id == id })
        }
    }
}
