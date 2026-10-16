package com.emmanuela.launcher

import com.emmanuela.launcher.data.HomePlacement
import com.emmanuela.launcher.data.WidgetPlacement
import org.junit.Assert.assertEquals
import org.junit.Test

class HomePlacementTest {
    @Test fun dropSnapsThenClampsToMeasuredItemBounds() {
        assertEquals(WidgetPlacement(32f,48f),HomePlacement.dropped(WidgetPlacement(38f,47f),300f,500f,16))
        assertEquals(WidgetPlacement(293f,0f),HomePlacement.dropped(WidgetPlacement(400f,-12f),293f,500f,16))
    }
    @Test fun itemLargerThanViewportRemainsReachable() {
        assertEquals(WidgetPlacement(0f,0f),HomePlacement.constrain(WidgetPlacement(42f,99f),-15f,-5f))
    }
    @Test(expected=IllegalArgumentException::class) fun rejectsInvalidGrid() {
        HomePlacement.dropped(WidgetPlacement(10f,10f),100f,100f,0)
    }
}
