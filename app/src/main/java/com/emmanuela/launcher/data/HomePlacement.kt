package com.emmanuela.launcher.data

import kotlin.math.roundToInt

/** Positions are dp, so saved layouts remain usable across density changes. */
object HomePlacement {
    fun constrain(point: WidgetPlacement, maxX: Float, maxY: Float) = WidgetPlacement(
        point.x.coerceIn(0f, maxX.coerceAtLeast(0f)),
        point.y.coerceIn(0f, maxY.coerceAtLeast(0f))
    )

    fun dropped(point: WidgetPlacement, maxX: Float, maxY: Float, grid: Int): WidgetPlacement {
        require(grid in 4..48)
        return constrain(WidgetPlacement(
            (point.x / grid).roundToInt() * grid.toFloat(),
            (point.y / grid).roundToInt() * grid.toFloat()
        ), maxX, maxY)
    }
}
