package com.emmanuela.launcher.ui.navigation

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import kotlin.math.abs

class GestureRegions {
    var dragX by mutableFloatStateOf(0f)
    var onClaim: () -> Unit = {}
    val controls = mutableMapOf<String, Rect>()
    val verticalScrolls=mutableMapOf<String,Rect>()
    var hostOrigin by mutableStateOf(Offset.Zero)
}
val LocalGestureRegions = staticCompositionLocalOf { GestureRegions() }
fun Modifier.ownsGesture(id: String): Modifier = composed {
    val regions = LocalGestureRegions.current
    DisposableEffect(id) { onDispose { regions.controls.remove(id) } }
    onGloballyPositioned { regions.controls[id] = it.boundsInRoot() }
}

fun Modifier.ownsVerticalScroll(id:String):Modifier=composed{
    val regions=LocalGestureRegions.current
    DisposableEffect(id){onDispose{regions.verticalScrolls.remove(id)}}
    onGloballyPositioned{regions.verticalScrolls[id]=it.boundsInRoot()}
}

/** Observe before clickable children; claim only after axis lock so taps/long presses still work. */
fun Modifier.launcherGestures(
    enabled: Boolean,
    home: Boolean,
    threshold: Float,
    regions: GestureRegions,
    dispatch: (String) -> Unit
): Modifier = composed {
    val latest by rememberUpdatedState(dispatch)
    onGloballyPositioned { regions.hostOrigin = it.positionInRoot() }.then(
        if (!enabled) Modifier else Modifier.pointerInput(home, threshold, regions) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if (regions.controls.values.any { it.contains(down.position + regions.hostOrigin) }) return@awaitEachGesture
                val nativeVertical=regions.verticalScrolls.values.any{it.contains(down.position+regions.hostOrigin)}
                var owned = false
                var horizontal = false
                var delta = Offset.Zero
                val began = down.uptimeMillis
                try { while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    // Android cancellation arrives as consumed pointer-up changes.
                    // Never navigate after cancellation or another recognizer's claim.
                    if(event.changes.any{it.isConsumed})return@awaitEachGesture
                    if (event.changes.count { it.pressed } > 1) return@awaitEachGesture
                    val change = event.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                    delta = change.position - down.position
                    if (!owned) {
                        // A completed long-press belongs to its context menu.
                        if (change.uptimeMillis - began >= viewConfiguration.longPressTimeoutMillis) return@awaitEachGesture
                        if (maxOf(abs(delta.x), abs(delta.y)) > viewConfiguration.touchSlop) {
                            horizontal = abs(delta.x) > abs(delta.y) * 1.15f
                            val vertical = abs(delta.y) > abs(delta.x) * 1.15f
                            if (vertical && (!home || nativeVertical)) return@awaitEachGesture
                            if (horizontal || (vertical && home)) { owned = true; regions.onClaim() }
                        }
                    }
                    if (owned) {
                        change.consume()
                        if(horizontal && !home) regions.dragX = delta.x * .18f
                    }
                    if (!change.pressed) {
                        if (owned && maxOf(abs(delta.x), abs(delta.y)) >= threshold) {
                            latest(if (horizontal) { if (delta.x < 0) "left" else "right" } else { if (delta.y < 0) "up" else "down" })
                        }
                        break
                    }
                } } finally { regions.dragX = 0f }
            }
        }
    )
}
