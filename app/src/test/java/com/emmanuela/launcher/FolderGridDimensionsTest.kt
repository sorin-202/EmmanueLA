package com.emmanuela.launcher

import com.emmanuela.launcher.data.*
import org.junit.Assert.*
import org.junit.Test

class FolderGridDimensionsTest {
    @Test fun existingFoldersMigrateToFourByFourWithoutChangingOrder() {
        val raw = """{"version":1,"folders":[{"id":"a","name":"A","icon":"folder","apps":["b/.B","a/.A"],"gridCell":null,"manualOrder":true}]}"""
        val folder = FolderCodec.decode(raw).single()
        assertEquals(4, folder.gridColumns)
        assertEquals(4, folder.gridRows)
        assertEquals(listOf("b/.B", "a/.A"), folder.apps)
        assertTrue(folder.manualOrder)
    }

    @Test fun fiveByFourAndFiveByFiveRemainIndependentAndPersist() {
        val folders = listOf(
            AppFolder("a", "A", "folder", listOf("b/.B", "a/.A"), gridColumns = 5, gridRows = 4, widthUnits = 5, heightUnits = 4, manualOrder = true),
            AppFolder("b", "B", "folder", emptyList(), gridColumns = 5, gridRows = 5, widthUnits = 5, heightUnits = 5)
        )
        assertEquals(folders, FolderCodec.decode(FolderCodec.encode(folders)))
    }

    @Test fun canvasSpansAboveFourPackWithoutOverlapAndAdaptToSmallScreens() {
        val folders = listOf(AppFolder("a", "A", "folder", emptyList(), widthUnits = 5, heightUnits = 5),
            AppFolder("b", "B", "folder", emptyList(), widthUnits = 5, heightUnits = 4))
        val packed = FolderGridLayout.pack(folders, 6, 2, 2)
        assertEquals(5, packed.first().width)
        assertEquals(5, packed.first().height)
        val used = mutableSetOf<Pair<Int, Int>>()
        packed.forEach { cell ->
            for (x in cell.column until cell.column + cell.width) for (y in cell.row until cell.row + cell.height) assertTrue(used.add(x to y))
        }
        assertEquals(3, FolderGridLayout.pack(folders, 3, 2, 2).first().width)
    }

    @Test fun appGridAdaptsToScreenWithoutDroppingRequestedPreferences() {
        assertEquals(5, FolderContentGrid.columns(5, 360f))
        assertEquals(4, FolderContentGrid.columns(5, 260f))
        assertEquals(1, FolderContentGrid.columns(8, 40f))
        assertTrue(FolderContentGrid.rowHeight(8, 320f) >= 72f)
        assertTrue(FolderContentGrid.rowHeight(1, 1000f) <= 144f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun unsafeAppColumnsAreRejected() {
        FolderCodec.decode(FolderCodec.encode(listOf(AppFolder("a", "A", "folder", emptyList(), gridColumns = 9))))
    }

    @Test(expected = IllegalArgumentException::class)
    fun unsafeCanvasSpansAreRejected() {
        FolderCodec.decode(FolderCodec.encode(listOf(AppFolder("a", "A", "folder", emptyList(), heightUnits = 9))))
    }
}
