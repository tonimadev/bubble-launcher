package digital.tonima.bubbleslauncher.ui

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import digital.tonima.bubbleslauncher.R
import digital.tonima.bubbleslauncher.data.AppRepository
import digital.tonima.bubbleslauncher.data.Profile
import digital.tonima.bubbleslauncher.data.SettingsRepository
import digital.tonima.bubbleslauncher.notification.NotificationRepository
import digital.tonima.bubbleslauncher.model.AppFolder
import digital.tonima.bubbleslauncher.model.AppInfo
import digital.tonima.bubbleslauncher.model.FolderOps
import digital.tonima.bubbleslauncher.ui.drag.DragLogic
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.receiveAsFlow

@OptIn(kotlinx.coroutines.FlowPreview::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: AppRepository,
    private val settingsRepository: SettingsRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    data class MainUiState(
        val apps: List<AppInfo> = emptyList(),
        val highlightedApps: Set<String> = emptySet(),
        val pinnedApps: List<String> = emptyList(),
        val showAppNames: Boolean = true,
        val showUsageBadges: Boolean = true,
        val isLoading: Boolean = false,
        val ignoreDynamicSize: Boolean = false,
        val iconSizeDp: Int = 64,
        val useSystemWallpaper: Boolean = false,
        val selectedProfile: Profile = Profile.PERSONAL,
        val themeMode: String = "system", // "system", "light", "dark"
        val hasWorkProfile: Boolean = false,
        val delayApps: List<String> = emptyList(),
        val essentialApps: List<String> = emptyList(),
        val hiddenApps: List<String> = emptyList(),
        val isFocusModeEnabled: Boolean = false,
        val hasSeenOnboarding: Boolean = false,
        val notificationCounts: Map<String, Int> = emptyMap(),
        val showNotificationBadges: Boolean = true,
        val searchQuery: String = "",
        val dockApps: List<String> = emptyList(),
        val folders: List<AppFolder> = emptyList(),
        /** Bumped every time the Home button is pressed while the launcher is already on screen. */
        val homePressCount: Int = 0
    )

    sealed class MainIntent {
        object LoadApps : MainIntent()
        data class ToggleHighlight(val packageName: String) : MainIntent()
        data class TogglePin(val packageName: String) : MainIntent()
        /** Drag-reorder: move [packageName] to the slot currently held by [target] in Favorites. */
        data class MovePinned(val packageName: String, val target: String) : MainIntent()
        data class SetIconSize(val dp: Int) : MainIntent()
        data class SetThemeMode(val mode: String) : MainIntent()
        object ToggleShowAppNames : MainIntent()
        object ToggleShowUsageBadges : MainIntent()
        object ToggleIgnoreDynamicSize : MainIntent()
        object ToggleUseSystemWallpaper : MainIntent()
        data class SelectProfile(val profile: Profile) : MainIntent()
        data class SetApps(val apps: List<AppInfo>) : MainIntent()
        data class ToggleDelayApp(val packageName: String) : MainIntent()
        data class ToggleEssentialApp(val packageName: String) : MainIntent()
        data class ToggleHiddenApp(val packageName: String) : MainIntent()
        object ToggleFocusMode : MainIntent()
        object CompleteOnboarding : MainIntent()
        object ToggleShowNotificationBadges : MainIntent()
        data class SetSearchQuery(val query: String) : MainIntent()
        data class ToggleDock(val packageName: String) : MainIntent()
        data class CreateFolder(val name: String, val packageName: String) : MainIntent()
        data class AddToFolder(val folderId: String, val packageName: String) : MainIntent()
        data class RemoveFromFolder(val packageName: String) : MainIntent()
        data class RenameFolder(val folderId: String, val name: String) : MainIntent()
        data class DeleteFolder(val folderId: String) : MainIntent()
        data class MoveDock(val packageName: String, val target: String) : MainIntent()
        /** Home pressed: reset to a clean state (search cleared, scrolled to top). */
        object GoHome : MainIntent()
    }

    private val _uiState = MutableStateFlow(MainUiState(isLoading = true))
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    sealed class UiEvent {
        object PreferenceSaved : UiEvent()
        object HighlightSaved : UiEvent()
        object PinSaved : UiEvent()
        data class MessageRes(@StringRes val resId: Int, val args: List<Any> = emptyList()) : UiEvent()
    }

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val intents = Channel<MainIntent>(Channel.UNLIMITED)

    init {
        viewModelScope.launch {
            intents.consumeAsFlow().collect { intent ->
                handleIntent(intent)
            }
        }
        viewModelScope.launch {
            settingsRepository.showAppNamesFlow.collect { show ->
                _uiState.update { it.copy(showAppNames = show) }
            }
        }
        viewModelScope.launch {
            settingsRepository.ignoreDynamicSizeFlow.collect { ignore ->
                _uiState.update { it.copy(ignoreDynamicSize = ignore) }
            }
        }
        viewModelScope.launch {
            settingsRepository.showUsageBadgesFlow.collect { show ->
                _uiState.update { it.copy(showUsageBadges = show) }
            }
        }
        viewModelScope.launch {
            settingsRepository.iconSizeFlow.collect { size ->
                _uiState.update { it.copy(iconSizeDp = size) }
            }
        }
        viewModelScope.launch {
            settingsRepository.useSystemWallpaperFlow.collect { use ->
                _uiState.update { it.copy(useSystemWallpaper = use) }
            }
        }
        viewModelScope.launch {
            settingsRepository.themeModeFlow.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
        viewModelScope.launch {
            settingsRepository.selectedProfileFlow.collect { profileStr ->
                val profile = when (profileStr) {
                    "work" -> Profile.WORK
                    else -> Profile.PERSONAL
                }
                _uiState.update { it.copy(selectedProfile = profile) }
            }
        }
        // detect if device has a work profile
        viewModelScope.launch {
            val hasWork = try {
                repository.hasWorkProfile()
            } catch (_: Exception) { false }
            _uiState.update { it.copy(hasWorkProfile = hasWork) }
        }
        viewModelScope.launch {
            settingsRepository.highlightedAppsFlow.collect { set ->
                _uiState.update { it.copy(highlightedApps = set) }
            }
        }
        viewModelScope.launch {
            settingsRepository.pinnedAppsFlow.collect { set ->
                _uiState.update { it.copy(pinnedApps = set) }
            }
        }
        viewModelScope.launch {
            settingsRepository.delayAppsFlow.collect { list ->
                _uiState.update { it.copy(delayApps = list) }
            }
        }
        viewModelScope.launch {
            settingsRepository.essentialAppsFlow.collect { list ->
                _uiState.update { it.copy(essentialApps = list) }
            }
        }
        viewModelScope.launch {
            settingsRepository.foldersFlow.collect { list ->
                _uiState.update { it.copy(folders = list) }
            }
        }
        viewModelScope.launch {
            settingsRepository.dockAppsFlow.collect { list ->
                _uiState.update { it.copy(dockApps = list) }
            }
        }
        viewModelScope.launch {
            settingsRepository.hiddenAppsFlow.collect { list ->
                _uiState.update { it.copy(hiddenApps = list) }
            }
        }
        viewModelScope.launch {
            settingsRepository.isFocusModeEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(isFocusModeEnabled = enabled) }
            }
        }
        viewModelScope.launch {
            settingsRepository.hasSeenOnboardingFlow.collect { seen ->
                _uiState.update { it.copy(hasSeenOnboarding = seen) }
            }
        }
        viewModelScope.launch {
            notificationRepository.notificationCounts.collect { counts ->
                _uiState.update { it.copy(notificationCounts = counts) }
            }
        }
        // Reload when apps are installed/removed/updated (debounced: installs fire several callbacks).
        viewModelScope.launch {
            repository.packageChanges.debounce(300).collect { submitIntent(MainIntent.LoadApps) }
        }
        viewModelScope.launch {
            settingsRepository.showNotificationBadgesFlow.collect { show ->
                _uiState.update { it.copy(showNotificationBadges = show) }
            }
        }
    }

    /** Loads apps for the selected profile. Cheap when cached; ordering/sections are derived in the UI. */
    private suspend fun loadApps() {
        if (_uiState.value.apps.isEmpty()) _uiState.update { it.copy(isLoading = true) }
        try {
            val apps = repository.getAppsForProfile(_uiState.value.selectedProfile)
                .sortedByDescending { it.totalTimeInForeground }
            _uiState.update { it.copy(apps = apps, isLoading = false) }
        } catch (t: Throwable) {
            _uiState.update { it.copy(apps = emptyList(), isLoading = false) }
            _events.trySend(UiEvent.MessageRes(R.string.msg_load_apps_failed, listOf(t.message.orEmpty())))
        }
    }

    private fun setFolders(list: List<AppFolder>) {
        if (list == _uiState.value.folders) return
        _uiState.update { it.copy(folders = list) }
        viewModelScope.launch { settingsRepository.setFolders(list) }
    }

    private fun setDock(list: List<String>) {
        _uiState.update { it.copy(dockApps = list) }
        viewModelScope.launch { settingsRepository.setDockApps(list) }
    }

    fun submitIntent(intent: MainIntent) {
        intents.trySend(intent)
    }

    private suspend fun handleIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.LoadApps -> loadApps()
            is MainIntent.ToggleHighlight -> {
                val pkg = intent.packageName
                _uiState.update { state ->
                    val newSet = if (state.highlightedApps.contains(pkg)) state.highlightedApps - pkg else state.highlightedApps + pkg
                    state.copy(highlightedApps = newSet)
                }

                viewModelScope.launch {
                    val currently = _uiState.value.highlightedApps
                    if (currently.contains(pkg)) {
                        settingsRepository.addHighlight(pkg)
                    } else {
                        settingsRepository.removeHighlight(pkg)
                    }
                    _events.trySend(UiEvent.HighlightSaved)
                }
            }
            is MainIntent.TogglePin -> {
                val pkg = intent.packageName
                _uiState.update { state ->
                    val newList = if (pkg in state.pinnedApps) state.pinnedApps - pkg else listOf(pkg) + state.pinnedApps
                    state.copy(pinnedApps = newList)
                }
                viewModelScope.launch {
                    settingsRepository.setPinnedApps(_uiState.value.pinnedApps)
                    _events.trySend(UiEvent.PinSaved)
                }
            }
            is MainIntent.MovePinned -> {
                _uiState.update { it.copy(pinnedApps = DragLogic.moveTo(it.pinnedApps, intent.packageName, intent.target)) }
                viewModelScope.launch { settingsRepository.setPinnedApps(_uiState.value.pinnedApps) }
            }
            is MainIntent.ToggleShowAppNames -> {
                val newValue = !_uiState.value.showAppNames
                _uiState.update { it.copy(showAppNames = newValue) }
                viewModelScope.launch {
                    settingsRepository.setShowAppNames(newValue)
                    _events.trySend(UiEvent.PreferenceSaved)
                }
            }
            is MainIntent.ToggleShowUsageBadges -> {
                val newValue = !_uiState.value.showUsageBadges
                _uiState.update { it.copy(showUsageBadges = newValue) }
                viewModelScope.launch {
                    settingsRepository.setShowUsageBadges(newValue)
                    _events.trySend(UiEvent.PreferenceSaved)
                }
            }
            is MainIntent.ToggleIgnoreDynamicSize -> {
                val newValue = !_uiState.value.ignoreDynamicSize
                _uiState.update { it.copy(ignoreDynamicSize = newValue) }
                viewModelScope.launch {
                    settingsRepository.setIgnoreDynamicSize(newValue)
                    _events.trySend(UiEvent.PreferenceSaved)
                }
            }
            is MainIntent.SetIconSize -> {
                val dp = intent.dp
                _uiState.update { it.copy(iconSizeDp = dp) }
                viewModelScope.launch {
                    settingsRepository.setIconSize(dp)
                    _events.trySend(UiEvent.PreferenceSaved)
                }
            }
            is MainIntent.SetThemeMode -> {
                val mode = intent.mode
                _uiState.update { it.copy(themeMode = mode) }
                viewModelScope.launch {
                    settingsRepository.setThemeMode(mode)
                    _events.trySend(UiEvent.PreferenceSaved)
                }
            }
            is MainIntent.ToggleUseSystemWallpaper -> {
                val newValue = !_uiState.value.useSystemWallpaper
                _uiState.update { it.copy(useSystemWallpaper = newValue) }
                viewModelScope.launch {
                    settingsRepository.setUseSystemWallpaper(newValue)
                    _events.trySend(UiEvent.PreferenceSaved)
                }
            }
            is MainIntent.SelectProfile -> {
                _uiState.update { it.copy(selectedProfile = intent.profile) }
                viewModelScope.launch {
                    try {
                        settingsRepository.setSelectedProfile(if (intent.profile == Profile.WORK) "work" else "personal")
                    } catch (_: Exception) { /* ignore persisting errors */ }
                }
                loadApps()
                if (intent.profile == Profile.WORK && _uiState.value.apps.isEmpty()) {
                    _events.trySend(UiEvent.MessageRes(R.string.msg_work_profile_empty))
                }
            }
            is MainIntent.SetApps -> {
                _uiState.update { it.copy(apps = intent.apps) }
            }
            is MainIntent.ToggleDelayApp -> {
                val pkg = intent.packageName
                _uiState.update { state ->
                    val newList = if (state.delayApps.contains(pkg)) state.delayApps - pkg else state.delayApps + pkg
                    state.copy(delayApps = newList)
                }
                viewModelScope.launch {
                    settingsRepository.setDelayApps(_uiState.value.delayApps)
                    _events.trySend(UiEvent.PreferenceSaved)
                }
            }
            is MainIntent.ToggleEssentialApp -> {
                val pkg = intent.packageName
                _uiState.update { state ->
                    val newList = if (state.essentialApps.contains(pkg)) state.essentialApps - pkg else state.essentialApps + pkg
                    state.copy(essentialApps = newList)
                }
                viewModelScope.launch {
                    settingsRepository.setEssentialApps(_uiState.value.essentialApps)
                    _events.trySend(UiEvent.PreferenceSaved)
                }
            }
            is MainIntent.ToggleHiddenApp -> {
                val pkg = intent.packageName
                _uiState.update { state ->
                    val newList = if (state.hiddenApps.contains(pkg)) state.hiddenApps - pkg else state.hiddenApps + pkg
                    state.copy(hiddenApps = newList)
                }
                viewModelScope.launch {
                    settingsRepository.setHiddenApps(_uiState.value.hiddenApps)
                    _events.trySend(UiEvent.PreferenceSaved)
                }
            }
            is MainIntent.ToggleFocusMode -> {
                val newValue = !_uiState.value.isFocusModeEnabled
                _uiState.update { it.copy(isFocusModeEnabled = newValue) }
                viewModelScope.launch {
                    settingsRepository.setFocusModeEnabled(newValue)
                    _events.trySend(UiEvent.PreferenceSaved)
                }
            }
            is MainIntent.CompleteOnboarding -> {
                _uiState.update { it.copy(hasSeenOnboarding = true) }
                viewModelScope.launch {
                    settingsRepository.setHasSeenOnboarding(true)
                }
            }
            is MainIntent.ToggleShowNotificationBadges -> {
                val newValue = !_uiState.value.showNotificationBadges
                _uiState.update { it.copy(showNotificationBadges = newValue) }
                viewModelScope.launch {
                    settingsRepository.setShowNotificationBadges(newValue)
                    _events.trySend(UiEvent.PreferenceSaved)
                }
            }
            is MainIntent.CreateFolder -> setFolders(
                FolderOps.create(_uiState.value.folders, intent.name, intent.packageName, UUID.randomUUID().toString().take(8))
            )
            is MainIntent.AddToFolder -> setFolders(FolderOps.addApp(_uiState.value.folders, intent.folderId, intent.packageName))
            is MainIntent.RemoveFromFolder -> setFolders(FolderOps.removeApp(_uiState.value.folders, intent.packageName))
            is MainIntent.RenameFolder -> setFolders(FolderOps.rename(_uiState.value.folders, intent.folderId, intent.name))
            is MainIntent.DeleteFolder -> setFolders(FolderOps.delete(_uiState.value.folders, intent.folderId))
            is MainIntent.ToggleDock -> {
                val pkg = intent.packageName
                val current = _uiState.value.dockApps
                when {
                    pkg in current -> setDock(current - pkg)
                    current.size >= AppSections.MAX_DOCK -> _events.trySend(UiEvent.MessageRes(R.string.msg_dock_full))
                    else -> setDock(current + pkg)
                }
            }
            is MainIntent.MoveDock -> setDock(DragLogic.moveTo(_uiState.value.dockApps, intent.packageName, intent.target))
            is MainIntent.GoHome -> _uiState.update { it.copy(searchQuery = "", homePressCount = it.homePressCount + 1) }
            is MainIntent.SetSearchQuery -> {
                _uiState.update { it.copy(searchQuery = intent.query) }
            }
        }
    }
}
