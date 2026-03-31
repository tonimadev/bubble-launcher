package digital.tonima.bubbleslauncher.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("bubbles_settings")

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        private val SHOW_NAMES = booleanPreferencesKey("show_app_names")
        private val IGNORE_DYNAMIC_SIZE = booleanPreferencesKey("ignore_dynamic_size")
        private val ICON_SIZE = intPreferencesKey("icon_size_dp")
        private val USE_SYSTEM_WALLPAPER = booleanPreferencesKey("use_system_wallpaper")
        private val HIGHLIGHTED_APPS = stringSetPreferencesKey("highlighted_apps")
        private val PINNED_APPS = stringSetPreferencesKey("pinned_apps")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    private val dataStore = context.dataStore

    val showAppNamesFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[SHOW_NAMES] ?: true
    }

    val ignoreDynamicSizeFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[IGNORE_DYNAMIC_SIZE] ?: false
    }

    // fixed icon size in dp when ignoreDynamicSize is enabled
    val iconSizeFlow: Flow<Int> = dataStore.data.map { prefs ->
        prefs[ICON_SIZE] ?: 64
    }

    val useSystemWallpaperFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[USE_SYSTEM_WALLPAPER] ?: false
    }

    val highlightedAppsFlow: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[HIGHLIGHTED_APPS] ?: emptySet()
    }

    val pinnedAppsFlow: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[PINNED_APPS] ?: emptySet()
    }

    // theme mode stored as string: "system", "light", "dark"
    val themeModeFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[THEME_MODE] ?: "system"
    }

    suspend fun setShowAppNames(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[SHOW_NAMES] = value
        }
    }

    suspend fun setIgnoreDynamicSize(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[IGNORE_DYNAMIC_SIZE] = value
        }
    }

    suspend fun setUseSystemWallpaper(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[USE_SYSTEM_WALLPAPER] = value
        }
    }

    suspend fun setHighlightedApps(values: Set<String>) {
        dataStore.edit { prefs ->
            prefs[HIGHLIGHTED_APPS] = values
        }
    }

    suspend fun setPinnedApps(values: Set<String>) {
        dataStore.edit { prefs ->
            prefs[PINNED_APPS] = values
        }
    }

    suspend fun addHighlight(pkg: String) {
        dataStore.edit { prefs ->
            val current = prefs[HIGHLIGHTED_APPS] ?: emptySet()
            prefs[HIGHLIGHTED_APPS] = current + pkg
        }
    }

    suspend fun addPin(pkg: String) {
        dataStore.edit { prefs ->
            val current = prefs[PINNED_APPS] ?: emptySet()
            prefs[PINNED_APPS] = current + pkg
        }
    }

    suspend fun removeHighlight(pkg: String) {
        dataStore.edit { prefs ->
            val current = prefs[HIGHLIGHTED_APPS] ?: emptySet()
            prefs[HIGHLIGHTED_APPS] = current - pkg
        }
    }

    suspend fun removePin(pkg: String) {
        dataStore.edit { prefs ->
            val current = prefs[PINNED_APPS] ?: emptySet()
            prefs[PINNED_APPS] = current - pkg
        }
    }

    suspend fun setIconSize(dp: Int) {
        dataStore.edit { prefs ->
            prefs[ICON_SIZE] = dp
        }
    }

    suspend fun setThemeMode(value: String) {
        dataStore.edit { prefs ->
            prefs[THEME_MODE] = value
        }
    }
}

