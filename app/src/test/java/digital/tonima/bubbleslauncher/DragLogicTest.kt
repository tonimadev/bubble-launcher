package digital.tonima.bubbleslauncher

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import digital.tonima.bubbleslauncher.ui.drag.DragLogic
import digital.tonima.bubbleslauncher.ui.drag.DropZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DragLogicTest {
    private val abcd = listOf("a", "b", "c", "d")

    @Test fun moveForward_shiftsBetween() = assertEquals(listOf("b", "c", "a", "d"), DragLogic.moveTo(abcd, "a", "c"))

    @Test fun moveBackward_shiftsBetween() = assertEquals(listOf("d", "a", "b", "c"), DragLogic.moveTo(abcd, "d", "a"))

    @Test fun moveToSelf_noChange() = assertEquals(abcd, DragLogic.moveTo(abcd, "b", "b"))

    @Test fun unknownPackages_noChange() {
        assertEquals(abcd, DragLogic.moveTo(abcd, "x", "a"))
        assertEquals(abcd, DragLogic.moveTo(abcd, "a", "x"))
    }

    @Test fun moveAdjacent_swaps() = assertEquals(listOf("b", "a", "c", "d"), DragLogic.moveTo(abcd, "a", "b"))

    @Test fun moveKeepsAllItems() {
        val r = DragLogic.moveTo(abcd, "b", "d")
        assertEquals(abcd.sorted(), r.sorted())
    }

    private val zones = mapOf(
        DropZone.UNINSTALL to Rect(0f, 0f, 100f, 100f),
        DropZone.APP_INFO to Rect(100f, 0f, 200f, 100f),
        DropZone.HIDE to Rect(200f, 0f, 300f, 100f)
    )

    @Test fun zoneAt_hits() {
        assertEquals(DropZone.UNINSTALL, DragLogic.zoneAt(Offset(50f, 50f), zones))
        assertEquals(DropZone.HIDE, DragLogic.zoneAt(Offset(250f, 10f), zones))
    }

    @Test fun zoneAt_missOutside() = assertNull(DragLogic.zoneAt(Offset(50f, 150f), zones))

    @Test fun zoneAt_emptyBounds() = assertNull(DragLogic.zoneAt(Offset(1f, 1f), emptyMap()))

    @Test fun itemAt_excludesDragged() {
        val items = mapOf("a" to Rect(0f, 0f, 100f, 100f), "b" to Rect(100f, 0f, 200f, 100f))
        assertEquals("b", DragLogic.itemAt(Offset(150f, 50f), items, exclude = "a"))
        assertNull(DragLogic.itemAt(Offset(50f, 50f), items, exclude = "a"))
        assertEquals("a", DragLogic.itemAt(Offset(50f, 50f), items))
    }

    @Test fun autoScroll_zeroAwayFromEdge() = assertEquals(0f, DragLogic.autoScrollSpeed(500f, 2000f, 150f, 30f), 0.001f)

    @Test fun autoScroll_scalesAndCaps() {
        assertEquals(15f, DragLogic.autoScrollSpeed(1925f, 2000f, 150f, 30f), 0.001f)
        assertEquals(30f, DragLogic.autoScrollSpeed(2100f, 2000f, 150f, 30f), 0.001f)
    }
}
