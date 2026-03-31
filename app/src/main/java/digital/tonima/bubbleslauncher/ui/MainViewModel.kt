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
        val pinnedApps: Set<String> = emptySet(),
        val showAppNames: Boolean = true,
        val isLoading: Boolean = false,
        val ignoreDynamicSize: Boolean = false
        ,
        val iconSizeDp: Int = 64,
        val useSystemWallpaper: Boolean = false
        ,
        val selectedProfile: Profile = Profile.PERSONAL
    )

    sealed class MainIntent {
        object LoadApps : MainIntent()
        data class ToggleHighlight(val packageName: String) : MainIntent()
        data class TogglePin(val packageName: String) : MainIntent()
        data class SetIconSize(val dp: Int) : MainIntent()
        object ToggleShowAppNames : MainIntent()
        object ToggleIgnoreDynamicSize : MainIntent()
        object ToggleUseSystemWallpaper : MainIntent()
        data class SelectProfile(val profile: Profile) : MainIntent()
        data class SetApps(val apps: List<AppInfo>) : MainIntent()
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
            settingsRepository.highlightedAppsFlow.collect { set ->
                _uiState.update { it.copy(highlightedApps = set) }
            }
        }
        viewModelScope.launch {
            settingsRepository.pinnedAppsFlow.collect { set ->
                _uiState.update { it.copy(pinnedApps = set) }
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
                    val apps = repository.getApps()
                    val sorted = apps.sortedByDescending { it.totalTimeInForeground }
                    val pinned = _uiState.value.pinnedApps
                    val (pinnedList, others) = sorted.partition { pinned.contains(it.packageName) }
                    val ordered = pinnedList + others
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
                    val newSet = if (state.pinnedApps.contains(pkg)) state.pinnedApps - pkg else state.pinnedApps + pkg
                    val pinnedList = state.apps.filter { newSet.contains(it.packageName) }
                    val others = state.apps.filterNot { newSet.contains(it.packageName) }
                        .sortedByDescending { it.totalTimeInForeground }
                    val newApps = pinnedList + others
                    state.copy(pinnedApps = newSet, apps = newApps)
                }
                viewModelScope.launch {
                    val currently = _uiState.value.pinnedApps
                    if (currently.contains(pkg)) {
                        settingsRepository.addPin(pkg)
                    } else {
                        settingsRepository.removePin(pkg)
                    }
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
            is MainIntent.ToggleUseSystemWallpaper -> {
                val newValue = !_uiState.value.useSystemWallpaper
                _uiState.update { it.copy(useSystemWallpaper = newValue) }
                viewModelScope.launch {
                    settingsRepository.setUseSystemWallpaper(newValue)
                    _events.trySend(UiEvent.PreferenceSaved)
                }
            }
            is MainIntent.SelectProfile -> {
                _uiState.update { it.copy(selectedProfile = intent.profile, isLoading = true) }
                viewModelScope.launch {
                    try {
                        val apps = repository.getAppsForProfile(intent.profile)
                        val sorted = apps.sortedByDescending { it.totalTimeInForeground }
                        val pinned = _uiState.value.pinnedApps
                        val (pinnedList, others) = sorted.partition { pinned.contains(it.packageName) }
                        val ordered = pinnedList + others
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
        }
    }
}
