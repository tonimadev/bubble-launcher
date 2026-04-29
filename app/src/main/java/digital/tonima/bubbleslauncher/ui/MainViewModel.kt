package digital.tonima.bubbleslauncher.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import digital.tonima.bubbleslauncher.data.AppRepository
import digital.tonima.bubbleslauncher.data.Profile
import digital.tonima.bubbleslauncher.data.SettingsRepository
import digital.tonima.bubbleslauncher.model.AppInfo
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.receiveAsFlow

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: AppRepository,
    private val settingsRepository: SettingsRepository
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
        val hasSeenOnboarding: Boolean = false
    )

    sealed class MainIntent {
        object LoadApps : MainIntent()
        data class ToggleHighlight(val packageName: String) : MainIntent()
        data class TogglePin(val packageName: String) : MainIntent()
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
    }

    private val _uiState = MutableStateFlow(MainUiState(isLoading = true))
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    sealed class UiEvent {
        object PreferenceSaved : UiEvent()
        object HighlightSaved : UiEvent()
        object PinSaved : UiEvent()
        data class Message(val text: String) : UiEvent()
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
    }

    fun submitIntent(intent: MainIntent) {
        intents.trySend(intent)
    }

    private suspend fun handleIntent(intent: MainIntent) {
        when (intent) {
            is MainIntent.LoadApps -> {
                _uiState.update { it.copy(isLoading = true) }
                try {
                    // Load apps for the currently selected profile so the selection persists across resumes
                    val profile = _uiState.value.selectedProfile
                    val apps = repository.getAppsForProfile(profile)
                    val sorted = apps.sortedByDescending { it.totalTimeInForeground }
                    val pinned = _uiState.value.pinnedApps
                    val pinnedAppsList = pinned.mapNotNull { p -> apps.find { it.packageName == p } }
                    val others = sorted.filterNot { pinned.contains(it.packageName) }
                    val ordered = pinnedAppsList + others
                    _uiState.update { it.copy(apps = ordered, isLoading = false) }
                } catch (t: Throwable) {
                    _uiState.update { it.copy(apps = emptyList(), isLoading = false) }
                    _events.trySend(UiEvent.Message("Falha ao carregar aplicativos: ${t.message}"))
                }
            }
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
                    val isCurrentlyPinned = state.pinnedApps.contains(pkg)
                    val newList = if (isCurrentlyPinned) {
                        state.pinnedApps - pkg
                    } else {
                        listOf(pkg) + state.pinnedApps
                    }

                    val pinnedAppsList = newList.mapNotNull { p -> state.apps.find { it.packageName == p } }
                    val others = state.apps.filterNot { newList.contains(it.packageName) }
                        .sortedByDescending { it.totalTimeInForeground }
                    val newApps = pinnedAppsList + others
                    state.copy(pinnedApps = newList, apps = newApps)
                }
                viewModelScope.launch {
                    val currently = _uiState.value.pinnedApps
                    settingsRepository.setPinnedApps(currently)
                    _events.trySend(UiEvent.PinSaved)
                }
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
                // persist selected profile and load apps for it
                _uiState.update { it.copy(selectedProfile = intent.profile, isLoading = true) }
                viewModelScope.launch {
                    try {
                        settingsRepository.setSelectedProfile(if (intent.profile == Profile.WORK) "work" else "personal")
                    } catch (_: Exception) { /* ignore persisting errors */ }

                    try {
                        val apps = repository.getAppsForProfile(intent.profile)
                        val sorted = apps.sortedByDescending { it.totalTimeInForeground }
                        val pinned = _uiState.value.pinnedApps
                        val pinnedAppsList = pinned.mapNotNull { p -> apps.find { it.packageName == p } }
                        val others = sorted.filterNot { pinned.contains(it.packageName) }
                        val ordered = pinnedAppsList + others
                        _uiState.update { it.copy(apps = ordered, isLoading = false) }
                        if (intent.profile == Profile.WORK && apps.isEmpty()) {
                            _events.trySend(UiEvent.Message("Nenhum aplicativo encontrado no perfil de trabalho (ou permissão faltando)."))
                        }
                    } catch (t: Throwable) {
                        _uiState.update { it.copy(apps = emptyList(), isLoading = false) }
                        _events.trySend(UiEvent.Message("Falha ao carregar apps do perfil: ${t.message}"))
                    }
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
        }
    }
}
