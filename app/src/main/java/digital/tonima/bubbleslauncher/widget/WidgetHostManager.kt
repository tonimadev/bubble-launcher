package digital.tonima.bubbleslauncher.widget

import android.app.Application
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps [AppWidgetHost] and [AppWidgetManager] to manage widget lifecycle
 * (allocate / bind / create views / deallocate).
 *
 * Injected as a Singleton so every consumer shares the same host instance.
 */
@Singleton
class WidgetHostManager @Inject constructor(
    private val application: Application
) {
    companion object {
        private const val HOST_ID = 1024
    }

    private val appWidgetHost: AppWidgetHost = AppWidgetHost(application, HOST_ID)
    private val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(application)

    /** Start listening for widget updates. Call from Activity.onStart(). */
    fun startListening() {
        appWidgetHost.startListening()
    }

    /** Stop listening for widget updates. Call from Activity.onStop(). */
    fun stopListening() {
        appWidgetHost.stopListening()
    }

    /** Allocate a new widget ID from the system. */
    fun allocateWidgetId(): Int {
        return appWidgetHost.allocateAppWidgetId()
    }

    /** Release a previously allocated widget ID. */
    fun deleteWidgetId(appWidgetId: Int) {
        appWidgetHost.deleteAppWidgetId(appWidgetId)
    }

    /** Create an [AppWidgetHostView] for the given widget. */
    fun createView(
        context: Context,
        appWidgetId: Int,
        providerInfo: AppWidgetProviderInfo
    ): AppWidgetHostView {
        return appWidgetHost.createView(context, appWidgetId, providerInfo)
    }

    /** Expose the underlying [AppWidgetManager]. */
    fun getAppWidgetManager(): AppWidgetManager = appWidgetManager

    /**
     * Try to bind a widget without user interaction.
     * Returns `true` if the bind succeeded (launcher has BIND_APPWIDGET permission),
     * `false` otherwise (caller must request permission via the system UI).
     */
    fun bindWidget(widgetId: Int, provider: ComponentName): Boolean {
        return AppWidgetManager.getInstance(application)
            .bindAppWidgetIdIfAllowed(widgetId, provider)
    }
}
