package com.emmanuela.launcher.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import org.json.JSONArray
import java.util.Locale

private val Context.webSearchStore by preferencesDataStore(name = "web_search_history")

enum class WebSearchEngine(val title: String, val host: String, val queryParameter: String = "q") {
    ANDROID_DEFAULT("Android default", "duckduckgo.com"),
    DUCKDUCKGO("DuckDuckGo", "duckduckgo.com"),
    GOOGLE("Google", "www.google.com"),
    BING("Bing", "www.bing.com");

    companion object {
        fun fromStored(value: String?): WebSearchEngine = entries.firstOrNull { it.name == value } ?: ANDROID_DEFAULT
    }
}

/** Queries stay on device. Only the explicit search action passes a query to another app. */
object WebSearchHistory {
    const val LIMIT = 5
    const val MAX_QUERY_LENGTH = 2000
    private val whitespace = Regex("\\s+")

    fun normalize(query: String): String = query.trim().replace(whitespace, " ")

    fun fromInput(input: String): String? = input.trimStart().takeIf { it.startsWith('/') }
        ?.drop(1)?.let(::normalize)?.takeIf { it.isNotBlank() && it.length <= MAX_QUERY_LENGTH }

    fun record(previous: List<String>, query: String): List<String> {
        val normalized = normalize(query)
        require(normalized.isNotEmpty() && normalized.length <= MAX_QUERY_LENGTH) { "Enter a web search of up to $MAX_QUERY_LENGTH characters." }
        val seen = mutableSetOf<String>()
        return (listOf(normalized) + previous.map(::normalize)).filter {
            it.isNotEmpty() && it.length <= MAX_QUERY_LENGTH && seen.add(it.lowercase(Locale.ROOT))
        }.take(LIMIT)
    }

    fun encode(queries: List<String>): String = JSONArray(queries.take(LIMIT)).toString()

    fun decode(raw: String?): List<String> = try {
        if (raw == null || raw.length > LIMIT * MAX_QUERY_LENGTH * 6 + 100) emptyList()
        else {
            val values = JSONArray(raw)
            val seen = mutableSetOf<String>()
            (0 until values.length().coerceAtMost(LIMIT)).mapNotNull { index ->
                (values.opt(index) as? String)?.let(::normalize)?.takeIf {
                    it.isNotBlank() && it.length <= MAX_QUERY_LENGTH && seen.add(it.lowercase(Locale.ROOT))
                }
            }
        }
    } catch (_: org.json.JSONException) { emptyList() }
}

class WebSearchHistoryRepository(context: Context) {
    private val store = context.applicationContext.webSearchStore
    private val key = stringPreferencesKey("queries")
    private val engineKey = stringPreferencesKey("engine")
    private val preferences = store.data.catch { error ->
        if (error is java.io.IOException) emit(emptyPreferences()) else throw error
    }
    val queries = preferences.map { WebSearchHistory.decode(it[key]) }
    val engine = preferences.map { WebSearchEngine.fromStored(it[engineKey]) }

    suspend fun record(query: String) {
        store.edit { it[key] = WebSearchHistory.encode(WebSearchHistory.record(WebSearchHistory.decode(it[key]), query)) }
    }

    suspend fun clear() { store.edit { it.remove(key) } }
    suspend fun setEngine(engine: WebSearchEngine) { store.edit { it[engineKey] = engine.name } }
    suspend fun reset() { store.edit { it.clear() } }
}
