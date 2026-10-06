package com.emmanuela.launcher.data

/** Constructed on Default when catalog, aliases, tags or visibility change. No Context or UI objects. */
class AppSearchIndex(apps: List<LaunchableApp>, hiddenPackages:Set<String> = emptySet(), hideFromSearch:Boolean=true) {
    private val catalog=AlphabetIndex.build(apps.filter{!hideFromSearch||it.packageName !in hiddenPackages})
    val all = AlphabetIndex.build(apps.filter{it.packageName !in hiddenPackages})
    private val names = catalog.apps.map { listOf(it.label, it.originalLabel, it.packageName, it.id).map{value->SearchRanking.Name(SearchRanking.folded(value))} }
    private val labels=names.map{it.take(2)}
    private val originals=names.map{listOf(it[1])}
    private val originalAndPackages=names.map{it.drop(1)}
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
    fun search(query: String, tagSearch: Boolean = true, searchAliases:Boolean=true, searchPackages:Boolean=false): IndexedApps {
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
        } else {
            val queryText = SearchRanking.Query(SearchRanking.folded(tokens.joinToString(" ")))
            val ranked = catalog.apps.indices.mapNotNull { index ->
                val rank = SearchRanking.score(queryText,if(searchAliases)labels[index]else originals[index])
                    ?: if (searchPackages) SearchRanking.score(queryText,if(searchAliases)names[index]else originalAndPackages[index])?.plus(4) else null
                rank?.let { index to it }
            }.sortedBy { it.second }.map { catalog.apps[it.first] }
            // Keep relevance order; alphabetic sections are only navigation metadata.
            val alphabet = AlphabetIndex.build(ranked)
            val positions = ranked.mapIndexed { i, app -> app.id to i }.toMap()
            return IndexedApps(ranked, alphabet.sections.map { section ->
                val app = alphabet.apps[section.firstIndex]
                AlphabetSection(section.label, positions.getValue(app.id))
            }.sortedBy { it.firstIndex })
        }
        return AlphabetIndex.build(filtered)
    }
}
