package digital.tonima.bubbleslauncher.ui.drag

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import digital.tonima.bubbleslauncher.model.AppInfo

enum class DropZone {
    UNINSTALL,
    APP_INFO,
    HIDE,
    PIN,
    DOCK
}

/**
 * Drag state shared by the grid, the gesture detector and the overlay. All positions are in
 * root (window) coordinates. The bounds registries are plain maps on purpose: they are written
 * from layout callbacks and read by the gesture handler, and must not trigger recomposition.
 */
@Stable
class DragDropState {
    var draggedApp by mutableStateOf<AppInfo?>(null)
        private set
    var dragPosition by mutableStateOf(Offset.Zero)
        private set
    var activeDropZone by mutableStateOf<DropZone?>(null)
        private set

    val isDragging: Boolean get() = draggedApp != null

    /** Window position of the container the drag ghost is drawn in. */
    var containerOrigin: Offset = Offset.Zero

    val zoneBounds = HashMap<DropZone, Rect>()
    val itemBounds = HashMap<String, Rect>()

    fun startDrag(app: AppInfo, position: Offset) {
        draggedApp = app
        dragPosition = position
        activeDropZone = DragLogic.zoneAt(position, zoneBounds)
    }

    fun updateDrag(position: Offset) {
        dragPosition = position
        val zone = DragLogic.zoneAt(position, zoneBounds)
        if (zone != activeDropZone) activeDropZone = zone
    }

    /** Ends the drag and returns what was dropped where (zone is null when dropped elsewhere). */
    fun endDrag(): Pair<AppInfo?, DropZone?> {
        val result = draggedApp to activeDropZone
        cancelDrag()
        return result
    }

    fun cancelDrag() {
        draggedApp = null
        dragPosition = Offset.Zero
        activeDropZone = null
    }
}

@Composable
fun rememberDragDropState(): DragDropState = remember { DragDropState() }
