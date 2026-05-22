package digital.tonima.bubbleslauncher.ui.drag

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import digital.tonima.bubbleslauncher.model.AppInfo

enum class DropZone {
    UNINSTALL,
    APP_INFO,
    HIDE
}

class DragDropState {
    var isDragging by mutableStateOf(false)
    var draggedApp by mutableStateOf<AppInfo?>(null)
    var dragPosition by mutableStateOf(Offset.Zero)
    var activeDropZone by mutableStateOf<DropZone?>(null)

    fun startDrag(app: AppInfo, position: Offset) {
        isDragging = true
        draggedApp = app
        dragPosition = position
        activeDropZone = null
    }

    fun updateDrag(position: Offset) {
        dragPosition = position
    }

    fun endDrag(): Pair<AppInfo?, DropZone?> {
        val result = draggedApp to activeDropZone
        isDragging = false
        draggedApp = null
        dragPosition = Offset.Zero
        activeDropZone = null
        return result
    }

    fun cancelDrag() {
        isDragging = false
        draggedApp = null
        dragPosition = Offset.Zero
        activeDropZone = null
    }
}

@Composable
fun rememberDragDropState(): DragDropState {
    return remember { DragDropState() }
}
