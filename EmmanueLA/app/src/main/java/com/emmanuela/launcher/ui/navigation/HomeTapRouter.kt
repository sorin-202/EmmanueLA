package com.emmanuela.launcher.ui.navigation

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.*
import kotlinx.coroutines.*

class HomeTapRouter(private val scope: CoroutineScope) {
    var doubleEnabled = false
    var tripleEnabled = false
    var timeout = 300L
    var radius = 100f
    var dispatch: (Int) -> Unit = {}
    private var pending: Job? = null
    private var count = 0
    private var origin = Offset.Zero
    private var single: (() -> Unit)? = null
    fun cancel() { pending?.cancel(); count = 0; single = null }
    fun tap(position: Offset, action: () -> Unit) {
        if (!doubleEnabled && !tripleEnabled) { action(); return }
        if (count > 0 && (position - origin).getDistance() > radius) {
            if (count == 1) single?.invoke() else if (count == 2 && doubleEnabled) dispatch(2)
            cancel()
        }
        if (count == 0) { origin = position; single = action }
        pending?.cancel(); count++
        if (count == 3) { cancel(); dispatch(3); return }
        if (count == 2 && !tripleEnabled) { cancel(); dispatch(2); return }
        pending = scope.launch {
            delay(timeout)
            val taps = count; val click = single
            count = 0; single = null
            if (taps == 1) click?.invoke() else if (doubleEnabled) dispatch(2)
        }
    }
}
val LocalHomeTapRouter = staticCompositionLocalOf<HomeTapRouter?> { null }
fun Modifier.homeClick(label: String, click: () -> Unit, longClick: (() -> Unit)? = null): Modifier = composed {
    val router = LocalHomeTapRouter.current
    val latestClick by rememberUpdatedState(click)
    val latestLong by rememberUpdatedState(longClick)
    var origin by remember { mutableStateOf(Offset.Zero) }
    onGloballyPositioned { origin = it.positionInRoot() }.semantics {
        role = Role.Button; contentDescription = label
        onClick { latestClick(); true }
        if (longClick != null) onLongClick("Manage app") { latestLong?.invoke(); true }
    }.pointerInput(router) {
        detectTapGestures(onTap = { position -> router?.tap(origin + position, latestClick) ?: latestClick() },
            onLongPress = if (longClick != null) { { _: Offset -> router?.cancel(); latestLong?.invoke(); Unit } } else null)
    }
}
