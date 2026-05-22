package digital.tonima.bubbleslauncher.data

import digital.tonima.bubbleslauncher.data.dao.WidgetDao
import digital.tonima.bubbleslauncher.data.model.WidgetEntry
import digital.tonima.bubbleslauncher.widget.WidgetHostManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source-of-truth for widget data.
 * Combines Room persistence ([WidgetDao]) with system-level widget management
 * ([WidgetHostManager]).
 */
@Singleton
class WidgetRepository @Inject constructor(
    private val widgetDao: WidgetDao,
    private val widgetHostManager: WidgetHostManager
) {
    /** Reactive stream of all persisted widgets, ordered by [WidgetEntry.widgetOrder]. */
    val widgets: Flow<List<WidgetEntry>> = widgetDao.getAllWidgets()

    /** Persist a new widget entry after the user has finished picking & binding it. */
    suspend fun addWidget(appWidgetId: Int, heightDp: Int = 200): Long {
        val entry = WidgetEntry(
            appWidgetId = appWidgetId,
            heightDp = heightDp
        )
        return widgetDao.insertWidget(entry)
    }

    /**
     * Remove a widget both from persistence and the system host so its ID can
     * be reclaimed.
     */
    suspend fun removeWidget(appWidgetId: Int) {
        widgetDao.deleteByWidgetId(appWidgetId)
        widgetHostManager.deleteWidgetId(appWidgetId)
    }
}
