package digital.tonima.bubbleslauncher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "impulse_events")
data class ImpulseEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,
    val packageName: String,
    val wasResisted: Boolean
)
