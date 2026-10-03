package digital.tonima.bubbleslauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "widgets")
data class WidgetEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val appWidgetId: Int,
    val widgetOrder: Int = 0,
    val heightDp: Int = 200,
    /** Shares a row with another half-width widget instead of spanning the full width. */
    val halfWidth: Boolean = false
)
