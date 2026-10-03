package digital.tonima.bubbleslauncher.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import digital.tonima.bubbleslauncher.data.dao.ImpulseDao
import digital.tonima.bubbleslauncher.data.dao.WidgetDao
import digital.tonima.bubbleslauncher.data.model.ImpulseEvent
import digital.tonima.bubbleslauncher.data.model.WidgetEntry

@Database(entities = [ImpulseEvent::class, WidgetEntry::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun impulseDao(): ImpulseDao
    abstract fun widgetDao(): WidgetDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `widgets` ADD COLUMN `halfWidth` INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `widgets` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `appWidgetId` INTEGER NOT NULL,
                        `widgetOrder` INTEGER NOT NULL DEFAULT 0,
                        `heightDp` INTEGER NOT NULL DEFAULT 200
                    )"""
                )
            }
        }
    }
}
