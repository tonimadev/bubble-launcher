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
        private val PINNED_APPS = stringPreferencesKey("pinned_apps_list") // Migrated to String for ordering
        private val PINNED_APPS_LEGACY = stringSetPreferencesKey("pinned_apps") // To handle legacy data
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val SELECTED_PROFILE = stringPreferencesKey("selected_profile")
        private val SHOW_USAGE_BADGES = booleanPreferencesKey("show_usage_badges")
    }

    private val dataStore = context.dataStore

    val showAppNamesFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[SHOW_NAMES] ?: true
    }

    val ignoreDynamicSizeFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[IGNORE_DYNAMIC_SIZE] ?: false
    }

    val iconSizeFlow: Flow<Int> = dataStore.data.map { prefs ->
        prefs[ICON_SIZE] ?: 64
    }

    val useSystemWallpaperFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[USE_SYSTEM_WALLPAPER] ?: false
    }

    val highlightedAppsFlow: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[HIGHLIGHTED_APPS] ?: emptySet()
    }

    val pinnedAppsFlow: Flow<List<String>> = dataStore.data.map { prefs ->
        val pinnedStr = prefs[PINNED_APPS]
        if (pinnedStr != null) {
            if (pinnedStr.isEmpty()) emptyList() else pinnedStr.split(",")
        } else {
            // Check legacy
            val legacy = prefs[PINNED_APPS_LEGACY]
            legacy?.toList() ?: emptyList()
        }
    }

    val themeModeFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[THEME_MODE] ?: "system"
    }

    val selectedProfileFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[SELECTED_PROFILE] ?: "personal"
    }

    val showUsageBadgesFlow: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[SHOW_USAGE_BADGES] ?: true
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

    suspend fun setPinnedApps(values: List<String>) {
        dataStore.edit { prefs ->
            prefs[PINNED_APPS] = values.joinToString(",")
            prefs.remove(PINNED_APPS_LEGACY) // clear legacy once migrated
        }
    }

    suspend fun addHighlight(pkg: String) {
        dataStore.edit { prefs ->
            val current = prefs[HIGHLIGHTED_APPS] ?: emptySet()
            prefs[HIGHLIGHTED_APPS] = current + pkg
        }
    }

    suspend fun removeHighlight(pkg: String) {
        dataStore.edit { prefs ->
            val current = prefs[HIGHLIGHTED_APPS] ?: emptySet()
            prefs[HIGHLIGHTED_APPS] = current - pkg
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

    suspend fun setSelectedProfile(value: String) {
        dataStore.edit { prefs ->
            prefs[SELECTED_PROFILE] = value
        }
    }

    suspend fun setShowUsageBadges(value: Boolean) {
        dataStore.edit { prefs ->
            prefs[SHOW_USAGE_BADGES] = value
        }
    }
}

