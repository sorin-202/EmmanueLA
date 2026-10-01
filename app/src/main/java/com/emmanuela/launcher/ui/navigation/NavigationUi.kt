package com.emmanuela.launcher.ui.navigation

import com.emmanuela.launcher.data.ExperiencePreferences


import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * A directional state machine instead of an infinite pager: RIGHT must always reach Home,
 * while LEFT toggles Apps/Folders. This avoids duplicated pages and unbounded back stacks.
 */
@Composable
fun LauncherSurfaceHost(
    surface: LauncherSurface,
    direction: Int,
    gesturesEnabled: Boolean,
    homeGesture: (String) -> Unit,
    navigate: (LauncherSurface, Int) -> Unit,
    experience: ExperiencePreferences = ExperiencePreferences(),
    content: @Composable (LauncherSurface, Boolean) -> Unit
) {
    val transition = updateTransition(surface, label = "Launcher navigation")
    val stateHolder = rememberSaveableStateHolder()
    val threshold = with(LocalDensity.current) { when(experience.gestureSensitivity){"High"->40.dp;"Low"->96.dp;else->64.dp}.toPx() }
    val duration=experience.motionDuration()
    val haptic=com.emmanuela.launcher.platform.rememberLauncherHapticTick()
    val regions = remember { GestureRegions() }
    val dragModifier = Modifier.launcherGestures(gesturesEnabled && !transition.isRunning,
        surface == LauncherSurface.HOME || surface==LauncherSurface.BLANK, threshold, regions) { gesture ->
        if(experience.gestureHaptics)haptic()
        if (surface == LauncherSurface.HOME || surface==LauncherSurface.BLANK) homeGesture(gesture)
        else {
            val swipe = if (gesture == "left") DrawerSwipe.LEFT else DrawerSwipe.RIGHT
            navigate(LauncherNavigation.destination(surface, swipe), if (swipe == DrawerSwipe.LEFT) 1 else -1)
        }
    }
    CompositionLocalProvider(LocalGestureRegions provides regions) {
    transition.AnimatedContent(
        modifier = Modifier.fillMaxSize().then(dragModifier),
        transitionSpec = {
            (slideInHorizontally(tween(durationMillis = duration, easing = experience.motionEasing())) { it * direction }+fadeIn(tween(duration),initialAlpha=.85f)) togetherWith
                (slideOutHorizontally(tween(durationMillis = duration, easing = experience.motionEasing())) { -it * direction }+fadeOut(tween(duration),targetAlpha=.9f))
        },
        contentKey = { it }
    ) { page ->
        stateHolder.SaveableStateProvider(page.name) {
            content(page, transition.currentState == page && transition.targetState == page && !transition.isRunning)
        }
    }
}
}
