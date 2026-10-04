package com.emmanuela.launcher.ui.navigation

import com.emmanuela.launcher.data.ExperiencePreferences
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing

/** Shared policy: decorative effects must respect the global accessibility controls. */
fun ExperiencePreferences.motionDuration(): Int =
    if (motion == "Off" || reduceMotion) 0 else when (motionSpeed) {
        "Slow" -> 360
        "Normal" -> 280
        else -> 200
    }
fun ExperiencePreferences.motionEasing(): Easing =
    if (motion == "Fluid") FastOutSlowInEasing else androidx.compose.animation.core.CubicBezierEasing(.2f,0f,0f,1f)
fun ExperiencePreferences.alphabetEffect(requested: String): String =
    if (motionDuration() == 0) "None" else requested
