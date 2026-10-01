package com.emmanuela.launcher.data

/** Constructed on Default when catalog, aliases, tags or visibility change. No Context or UI objects. */
class AppSearchIndex(apps: List<LaunchableApp>, hiddenPackages:Set<String> = emptySet(), hideFromSearch:Boolean=true) {
    private val catalog=AlphabetIndex.build(apps.filter{!hideFromSearch||it.packageName !in hiddenPackages})
    val all = AlphabetIndex.build(apps.filter{it.packageName !in hiddenPackages})
    private val names = catalog.apps.map { listOf(it.label, it.originalLabel, it.packageName, it.id).map(AppNaming::folded) }
    private val tagged = catalog.apps.indices.filter { catalog.apps[it].tags.isNotEmpty() }.toSet()
    private val inverted: Map<String, Set<Int>> = run {
        val result = mutableMapOf<String, MutableSet<Int>>()
        catalog.apps.forEachIndexed { index, app -> app.tags.forEach { result.getOrPut(it) { mutableSetOf() }.add(index) } }
        result.mapValues { it.value.toSet() }
    }
    private val tags = inverted.keys.sorted()
    private fun prefixTags(prefix: String): List<String> {
        val search = tags.binarySearch(prefix)
        var index = if (search >= 0) search else -search - 1
        val matches = mutableListOf<String>()
        while (index < tags.size && tags[index].startsWith(prefix)) matches += tags[index++]
        return matches
    }
    fun containsTag(tag:String)=tag in inverted
    fun suggestions(query: String) = prefixTags(AppNaming.folded(query.trim().substringAfterLast(' ').removePrefix("#"))).take(8)
    fun search(query: String, tagSearch: Boolean = true, searchAliases:Boolean=true, searchPackages:Boolean=true): IndexedApps {
        if (query.isBlank()) return all
        val tokens = AppNaming.folded(query.trim()).split(Regex("\\s+"))
        val filtered = if (tagSearch && AppSearch.isTagMode(query)) {
            val words = tokens.map { it.removePrefix("#") }.filter(String::isNotBlank)
            var matches = tagged
            words.forEachIndexed { i, word ->
                val keys = if (i == words.lastIndex && !query.last().isWhitespace()) prefixTags(word) else listOf(word)
                val candidates = keys.flatMap { inverted[it].orEmpty() }.toSet()
                matches = matches.intersect(candidates)
            }
            catalog.apps.filterIndexed { i, _ -> i in matches }
        } else catalog.apps.filterIndexed { index, _ -> tokens.all { token -> (token in names[index][1]) || (searchAliases && token in names[index][0]) || (searchPackages && (token in names[index][2] || token in names[index][3])) } }
        return AlphabetIndex.build(filtered)
    }
}
