package com.emmanuela.launcher.data

/** An explicit mode replaces the old alphabet toggle without changing existing Home layouts. */
object HomeModes {
    const val NORMAL = "Normal"
    const val MINIMALIST = "Minimalist"
    const val ALPHABET = "Alphabet"
    val values = listOf(NORMAL, MINIMALIST, ALPHABET)

    fun effective(preferences: ExperiencePreferences): String = preferences.homeMode
        ?: if (preferences.homeAlphabet) ALPHABET else MINIMALIST

    fun select(preferences: ExperiencePreferences, mode: String): ExperiencePreferences {
        require(mode in values) { "Choose Normal, Minimalist or Alphabet." }
        return preferences.copy(homeMode = mode, homeAlphabet = mode == ALPHABET)
    }
}
