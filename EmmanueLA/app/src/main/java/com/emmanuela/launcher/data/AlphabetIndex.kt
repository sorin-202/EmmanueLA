package com.emmanuela.launcher.data

import java.text.BreakIterator
import java.text.Collator
import java.util.Locale

data class AlphabetSection(val label: String, val firstIndex: Int)
data class IndexedApps(val apps: List<LaunchableApp>, val sections: List<AlphabetSection>)
object AlphabetIndex {
    fun build(apps: List<LaunchableApp>, locale: Locale = Locale.getDefault()): IndexedApps {
        val collator = Collator.getInstance(locale)
        val groups = apps.groupBy { app ->
            val name = app.label.trim()
            val iterator = BreakIterator.getCharacterInstance(locale).apply { setText(name) }
            val end = iterator.next()
            val first = if (end == BreakIterator.DONE) "" else name.substring(0, end)
            if (first.isNotEmpty() && Character.isLetter(first.codePointAt(0))) first.uppercase(locale) else "#"
        }
        val letters = groups.keys.sortedWith { a, b -> when { a == b -> 0; a == "#" -> 1; b == "#" -> -1; else -> collator.compare(a, b) } }
        val ordered = mutableListOf<LaunchableApp>()
        val sections = letters.map { letter ->
            AlphabetSection(letter, ordered.size).also {
                ordered += groups.getValue(letter).sortedWith { a, b -> collator.compare(a.label, b.label).takeIf { it != 0 } ?: a.id.compareTo(b.id) }
            }
        }
        return IndexedApps(ordered, sections)
    }
}
