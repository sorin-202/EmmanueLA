package com.emmanuela.launcher.ui.home

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.emmanuela.launcher.ui.navigation.LocalHomeTapRouter

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
    var itemCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val latestClick by rememberUpdatedState(click)
    val latestStart by rememberUpdatedState(start)
    val latestMove by rememberUpdatedState(move)
    val latestDrop by rememberUpdatedState(drop)
    val latestCancel by rememberUpdatedState(cancel)
    val latestOptions by rememberUpdatedState(options)
    onGloballyPositioned { itemCoordinates = it }.semantics {
        role = Role.Button
        contentDescription = label
        onClick { latestClick(); true }
        customActions = listOf(CustomAccessibilityAction("Options for $label") { latestOptions(); true })
    }.pointerInput(id, router) {
        var acquired = false
        try {
            awaitEachGesture {
                val down = awaitFirstDown()
                // Own the tap so the empty-Home hold action cannot fire behind this item.
                down.consume()
                // Enforce touch slop ourselves: Home can have all global gestures disabled.
                // Moving before the hold timeout must never turn a scroll into placement.
                val endedBeforeHold = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || event.changes.any { it.isConsumed } ||
                            event.changes.count { it.pressed } > 1 ||
                            (change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                            return@withTimeoutOrNull true
                        }
                        if (!change.pressed) {
                            change.consume()
                            router?.tap((itemCoordinates?.positionInRoot() ?: Offset.Zero) + change.position, latestClick) ?: latestClick()
                            return@withTimeoutOrNull true
                        }
                    }
                    @Suppress("UNREACHABLE_CODE")
                    false
                }
                if (endedBeforeHold != null) return@awaitEachGesture
                acquired = true
                var moved = false
                router?.cancel()
                latestStart()
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id }
                    // Compose marks the synthetic pointer-up from ACTION_CANCEL consumed.
                    if (change == null || event.changes.any { it.isConsumed } || event.changes.count { it.pressed } > 1) {
                        acquired = false
                        latestCancel()
                        return@awaitEachGesture
                    }
                    if (!change.pressed) {
                        val lastAmount = change.positionChange()
                        if (lastAmount != Offset.Zero) { moved = true; latestMove(lastAmount) }
                        change.consume()
                        acquired = false
                        latestDrop()
                        if (!moved) latestOptions()
                        return@awaitEachGesture
                    }
                    val amount = change.positionChange()
                    change.consume()
                    if (amount != Offset.Zero) moved = true
                    latestMove(amount)
                }
            }
        } finally {
            // Includes removal, a configuration change, or pointer-input disposal mid-drag.
            if (acquired) latestCancel()
        }
    }
}
