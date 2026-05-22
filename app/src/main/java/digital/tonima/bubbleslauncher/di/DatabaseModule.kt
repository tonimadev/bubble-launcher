package digital.tonima.bubbleslauncher.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import digital.tonima.bubbleslauncher.data.AppDatabase
import digital.tonima.bubbleslauncher.data.dao.ImpulseDao
import digital.tonima.bubbleslauncher.data.dao.WidgetDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "bubbles_database"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideImpulseDao(database: AppDatabase): ImpulseDao {
        return database.impulseDao()
    }

    @Provides
    fun provideWidgetDao(database: AppDatabase): WidgetDao {
        return database.widgetDao()
    }
}
