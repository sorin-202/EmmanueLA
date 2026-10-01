package com.emmanuela.launcher.data

import java.text.Collator
import java.text.Normalizer
import java.util.Locale

/** Stable ID is a flattened launchable ComponentName, never a mutable label. */
data class AppMetadata(val alias: String = "", val tags: List<String> = emptyList())

data class LaunchableApp(
    val id: String,
    val label: String,
    val packageName: String,
    val originalLabel: String = label,
    val tags: List<String> = emptyList()
)

object AppNaming {
    const val MAX_ALIAS_LENGTH = 80
    const val MAX_TAGS = 24
    const val MAX_TAG_LENGTH = 32
    private val tagPattern = Regex("[\\p{L}\\p{N}_-]+")
    fun folded(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKC).lowercase(Locale.ROOT)
    fun alias(value: String): String = value.trim().also {
        require(it.length <= MAX_ALIAS_LENGTH) { "Use at most 80 characters for an alias." }
        require(it.none { character -> character.isISOControl() }) { "Use a single line for the alias." }
    }
    fun tags(input: String): List<String> {
        val values = input.split(Regex("[,\\s]+")).filter(String::isNotBlank).map {
            folded(it.removePrefix("#")).also { tag ->
                require(tag.length in 1..MAX_TAG_LENGTH && tagPattern.matches(tag)) {
                    "Tags use 1–32 letters, numbers, hyphens or underscores."
                }
            }
        }.distinct().sorted()
        require(values.size <= MAX_TAGS) { "Use at most 24 tags per app." }
        return values
    }
    fun metadata(aliasText: String, tagsText: String) = AppMetadata(alias(aliasText), tags(tagsText))
    fun decorate(apps: List<LaunchableApp>, metadata: Map<String, AppMetadata>): List<LaunchableApp> {
        val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }
        return apps.map { app ->
            val saved = metadata[app.id]
            app.copy(label = saved?.alias?.takeIf(String::isNotBlank) ?: app.originalLabel, tags = saved?.tags.orEmpty())
        }.sortedWith { a, b ->
            collator.compare(a.label, b.label).takeIf { it != 0 }
                ?: collator.compare(a.originalLabel, b.originalLabel).takeIf { it != 0 }
                ?: a.id.compareTo(b.id)
        }
    }
}

object AppSearch {
    fun isTagMode(query: String) = query.trimStart().startsWith("#")
    /** Leading # puts the whole query in tag mode. Tokens are ANDed, final token is a prefix. */
    fun filter(apps: List<LaunchableApp>, query: String): List<LaunchableApp> {
        val folded = AppNaming.folded(query.trim())
        if (folded.isEmpty()) return apps
        if (isTagMode(folded)) {
            val tokens = folded.split(Regex("\\s+")).map { it.removePrefix("#") }.filter(String::isNotBlank)
            if (tokens.isEmpty()) return apps.filter { it.tags.isNotEmpty() }
            return apps.filter { app -> tokens.withIndex().all { (i, tag) ->
                if (i == tokens.lastIndex && !query.last().isWhitespace()) app.tags.any { it.startsWith(tag) }
                else tag in app.tags
            } }
        }
        val tokens = folded.split(Regex("\\s+"))
        return apps.filter { app ->
            val names = listOf(app.label, app.originalLabel, app.packageName).map(AppNaming::folded)
            tokens.all { token -> names.any { token in it } }
        }
    }
}
