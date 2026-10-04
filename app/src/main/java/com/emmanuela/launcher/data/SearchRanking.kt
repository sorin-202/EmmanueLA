package com.emmanuela.launcher.data

/** Lower scores sort first. Input names and query are folded once by the caller. */
object SearchRanking {
    private val words = Regex("[^\\p{L}\\p{N}]+")
    private val marks = Regex("\\p{M}+")
    fun folded(value: String): String = java.text.Normalizer.normalize(AppNaming.folded(value), java.text.Normalizer.Form.NFD).replace(marks, "")
    fun score(query: String, names: List<String>): Int? {
        if (query.isBlank()) return null
        if (names.any { it == query }) return 0
        if (names.any { it.startsWith(query) }) return 1
        val tokens = query.split(Regex("\\s+")).filter(String::isNotEmpty)
        if (tokens.all { token -> names.any { name -> name.split(words).any { it.startsWith(token) } } }) return 2
        return if (tokens.all { token -> names.any { token in it } }) 3 else null
    }
}
