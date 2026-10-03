package digital.tonima.bubbleslauncher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.center
import androidx.compose.ui.test.down
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import digital.tonima.bubbleslauncher.ui.drag.LongPressDragCallbacks
import digital.tonima.bubbleslauncher.ui.drag.longPressDragGestures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LongPressDragGesturesTest {
    @get:Rule
    val rule = createComposeRule()

    private val events = mutableListOf<String>()
    private var taps = 0

    private fun setContent(hitKey: String? = "item") {
        events.clear()
        taps = 0
        val cb = LongPressDragCallbacks(
            hitTest = { hitKey },
            onLongPress = { events += "longPress:$it" },
            onLongPressRelease = { events += "release:$it" },
            onDragStart = { key, _ -> events += "dragStart:$key" },
            onDrag = { events += "drag" },
            onDragEnd = { events += "dragEnd" },
            onDragCancel = { events += "dragCancel" }
        )
        rule.setContent {
            // A clickable child, like the real grid items, to prove the click is suppressed.
            Box(
                Modifier
                    .size(200.dp)
                    .testTag("host")
                    .longPressDragGestures(cb)
                    .clickable { taps++ }
            )
        }
    }

    @Test
    fun quickTap_doesNotStartAnything_andReachesChild() {
        setContent()
        rule.onNodeWithTag("host").performTouchInput { down(center); advanceEventTime(50); up() }
        rule.waitForIdle()
        assertTrue(events.isEmpty())
        assertEquals(1, taps)
    }

    @Test
    fun longPressWithoutMoving_opensMenu_andSuppressesClick() {
        setContent()
        rule.onNodeWithTag("host").performTouchInput { down(center); advanceEventTime(800); up() }
        rule.waitForIdle()
        assertEquals(listOf("longPress:item", "release:item"), events)
        assertEquals("a long press must not also launch the app", 0, taps)
    }

    @Test
    fun longPressThenMove_dragsAndEnds_withoutMenuOrClick() {
        setContent()
        rule.onNodeWithTag("host").performTouchInput {
            down(center)
            advanceEventTime(800)
            moveBy(Offset(120f, 0f))
            moveBy(Offset(60f, 0f))
            up()
        }
        rule.waitForIdle()
        assertEquals("longPress:item", events.first())
        assertEquals("dragStart:item", events[1])
        assertTrue(events.count { it == "drag" } >= 1)
        assertEquals("dragEnd", events.last())
        assertTrue("no menu after a drag", events.none { it.startsWith("release") })
        assertEquals(0, taps)
    }

    @Test
    fun touchOutsideAnyItem_isIgnored() {
        setContent(hitKey = null)
        rule.onNodeWithTag("host").performTouchInput { down(center); advanceEventTime(800); up() }
        rule.waitForIdle()
        assertTrue(events.isEmpty())
    }
}
