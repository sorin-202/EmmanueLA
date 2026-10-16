package com.emmanuela.launcher.ui.home

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import com.emmanuela.launcher.ui.navigation.LocalHomeTapRouter
import com.emmanuela.launcher.ui.navigation.homeClick

/** Taps keep Home's existing routing. Only a held pointer can acquire placement. */
fun Modifier.homeItemDrag(
    id: String,
    label: String,
    click: () -> Unit,
    options: () -> Unit,
    start: () -> Unit,
    move: (Offset) -> Unit,
    drop: () -> Unit,
    cancel: () -> Unit
): Modifier = composed {
    val haptics = LocalHapticFeedback.current
    val router = LocalHomeTapRouter.current
    val latestStart by rememberUpdatedState(start)
    val latestMove by rememberUpdatedState(move)
    val latestDrop by rememberUpdatedState(drop)
    val latestCancel by rememberUpdatedState(cancel)
    val latestOptions by rememberUpdatedState(options)
    homeClick(label, click).semantics {
        customActions = listOf(CustomAccessibilityAction("Options for $label") { latestOptions(); true })
    }.pointerInput(id, router) {
        var acquired = false
        var moved = false
        try {
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    acquired = true
                    moved = false
                    router?.cancel()
                    latestStart()
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onDrag = { change, amount ->
                    change.consume()
                    if (amount != Offset.Zero) moved = true
                    latestMove(amount)
                },
                onDragEnd = {
                    acquired = false
                    latestDrop()
                    if (!moved) latestOptions()
                },
                onDragCancel = {
                    acquired = false
                    latestCancel()
                }
            )
        } finally {
            // Includes removal, a configuration change, or pointer-input disposal mid-drag.
            if (acquired) latestCancel()
        }
    }
}
