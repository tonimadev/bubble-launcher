package digital.tonima.bubbleslauncher.data

import androidx.room.Database
import androidx.room.RoomDatabase
import digital.tonima.bubbleslauncher.data.dao.ImpulseDao
import digital.tonima.bubbleslauncher.data.model.ImpulseEvent

@Database(entities = [ImpulseEvent::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun impulseDao(): ImpulseDao
}
