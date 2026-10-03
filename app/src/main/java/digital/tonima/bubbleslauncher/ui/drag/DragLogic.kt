package digital.tonima.bubbleslauncher.ui.drag

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/** Pure helpers for drag & drop, kept free of Compose runtime state so they are unit-testable. */
object DragLogic {

    /**
     * Moves [pkg] to the slot currently held by [target], shifting the items in between
     * (the same behaviour as dragging a row over another in a reorderable list).
     * Returns [order] unchanged if either package is missing or they are equal.
     */
    fun moveTo(order: List<String>, pkg: String, target: String): List<String> {
        if (pkg == target) return order
        val from = order.indexOf(pkg)
        val to = order.indexOf(target)
        if (from < 0 || to < 0) return order
        return order.toMutableList().apply {
            removeAt(from)
            add(to, pkg)
        }
    }

    /** Which drop zone (if any) contains [position]. */
    fun zoneAt(position: Offset, bounds: Map<DropZone, Rect>): DropZone? =
        bounds.entries.firstOrNull { it.value.contains(position) }?.key

    /** Which keyed item (other than [exclude]) contains [position]. */
    fun itemAt(position: Offset, bounds: Map<String, Rect>, exclude: String? = null): String? =
        bounds.entries.firstOrNull { it.key != exclude && it.value.contains(position) }?.key

    /**
     * Scroll speed in px per frame for auto-scrolling near the bottom edge while dragging.
     * 0 when not near the edge; grows linearly up to [maxSpeed] at the very edge.
     */
    fun autoScrollSpeed(pointerY: Float, viewportBottom: Float, edge: Float, maxSpeed: Float): Float {
        val intoEdge = pointerY - (viewportBottom - edge)
        return if (intoEdge <= 0f) 0f else (intoEdge / edge).coerceAtMost(1f) * maxSpeed
    }
}
