package digital.tonima.bubbleslauncher.ui.drag

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/** Callbacks for [longPressDragGestures]. Positions are local to the modified element. */
class LongPressDragCallbacks(
    /** Key of the item under [position], or null if the touch is not on a draggable item. */
    val hitTest: (position: Offset) -> String?,
    /** The long-press timeout elapsed on [key]. */
    val onLongPress: (key: String) -> Unit,
    /** Finger lifted after a long press without moving: show the context menu. */
    val onLongPressRelease: (key: String) -> Unit,
    /** Finger moved past touch slop after a long press. */
    val onDragStart: (key: String, position: Offset) -> Unit,
    val onDrag: (position: Offset) -> Unit,
    val onDragEnd: () -> Unit,
    /** The gesture was interrupted (e.g. the system cancelled it). */
    val onDragCancel: () -> Unit
)

/**
 * Long-press, then drag, on any item of a container (e.g. a lazy grid) — handled at the
 * container level rather than per item, so the gesture survives items scrolling out of
 * composition during auto-scroll.
 *
 * Before the long press nothing is consumed, so taps and scrolling reach the children as usual.
 * Once the long press fires, subsequent events are consumed in the [PointerEventPass.Initial]
 * pass so the children neither scroll nor register a click when the finger is lifted.
 */
fun Modifier.longPressDragGestures(cb: LongPressDragCallbacks): Modifier =
    // Keyed on the callbacks object: callers keep it stable (remember) and have its lambdas read
    // the latest state, so the gesture coroutine isn't restarted mid-drag.
    pointerInput(cb) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val key = cb.hitTest(down.position) ?: return@awaitEachGesture
            val longPress = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture

            cb.onLongPress(key)
            var dragging = false
            var moved = Offset.Zero
            val slop = viewConfiguration.touchSlop
            longPress.consume()
            try {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    change.consume()
                    if (!change.pressed) {
                        if (dragging) cb.onDragEnd() else cb.onLongPressRelease(key)
                        dragging = false
                        return@awaitEachGesture
                    }
                    moved += change.position - change.previousPosition
                    if (!dragging && moved.getDistance() > slop) {
                        dragging = true
                        cb.onDragStart(key, change.position)
                    }
                    if (dragging) cb.onDrag(change.position)
                }
            } finally {
                if (dragging) cb.onDragCancel()
            }
        }
    }
