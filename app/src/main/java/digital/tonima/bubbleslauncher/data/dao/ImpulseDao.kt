package digital.tonima.bubbleslauncher.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import digital.tonima.bubbleslauncher.data.model.ImpulseEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface ImpulseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: ImpulseEvent): Long

    @Query("SELECT * FROM impulse_events WHERE timestamp >= :startTimestamp AND timestamp <= :endTimestamp ORDER BY timestamp DESC")
    fun getEventsByDateRange(startTimestamp: Long, endTimestamp: Long): Flow<List<ImpulseEvent>>

    @Query("SELECT COUNT(*) FROM impulse_events WHERE wasResisted = 1 AND timestamp >= :startTimestamp AND timestamp <= :endTimestamp")
    fun getResistedCountByDateRange(startTimestamp: Long, endTimestamp: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM impulse_events WHERE timestamp >= :startTimestamp AND timestamp <= :endTimestamp")
    fun getTotalCountByDateRange(startTimestamp: Long, endTimestamp: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM impulse_events WHERE wasResisted = 1")
    fun getLifetimeResistedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM impulse_events")
    fun getLifetimeTotalCount(): Flow<Int>
}
