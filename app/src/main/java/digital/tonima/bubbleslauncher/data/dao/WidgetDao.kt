package digital.tonima.bubbleslauncher.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import digital.tonima.bubbleslauncher.data.model.WidgetEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface WidgetDao {
    @Query("SELECT * FROM widgets ORDER BY widgetOrder ASC")
    fun getAllWidgets(): Flow<List<WidgetEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWidget(widget: WidgetEntry): Long

    @Query("UPDATE widgets SET heightDp = :heightDp, halfWidth = :halfWidth WHERE appWidgetId = :appWidgetId")
    suspend fun updateLayout(appWidgetId: Int, heightDp: Int, halfWidth: Boolean)

    @Query("DELETE FROM widgets WHERE appWidgetId = :appWidgetId")
    suspend fun deleteByWidgetId(appWidgetId: Int)

    @Query("DELETE FROM widgets WHERE id = :id")
    suspend fun deleteById(id: Int)
}
