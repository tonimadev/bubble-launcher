package digital.tonima.bubbleslauncher

import android.Manifest
import android.app.AppOpsManager
import android.content.Intent
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.roundToInt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import digital.tonima.bubbleslauncher.data.Profile
import digital.tonima.bubbleslauncher.ui.MainScreen
import digital.tonima.bubbleslauncher.ui.MainViewModel
import digital.tonima.bubbleslauncher.ui.theme.BubblesLauncherTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val viewModel: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
                val state by viewModel.uiState.collectAsState()

                val themeMode = state.themeMode
                val useDark = when (themeMode) {
                    "light" -> false
                    "dark" -> true
                    else -> isSystemInDarkTheme()
                }

                BubblesLauncherTheme(darkTheme = useDark) {

                val (showUsageRationale, setShowUsageRationale) = remember { mutableStateOf(false) }
                fun hasUsageAccess(ctx: Context): Boolean {
                    return try {
                        val appOps = ctx.getSystemService(AppOpsManager::class.java)
                        val mode = appOps.noteOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), ctx.packageName)
                        mode == AppOpsManager.MODE_ALLOWED || mode == 0
                    } catch (t: Throwable) {
                        false
                    }
                }

                LaunchedEffect(state.ignoreDynamicSize, state.apps) {
                    if (!state.ignoreDynamicSize && !hasUsageAccess(this@MainActivity)) {
                        setShowUsageRationale(true)
                    } else {
                        setShowUsageRationale(false)
                    }
                }

                val snackbarHostState = remember { SnackbarHostState() }
                val prefSavedMsg = stringResource(id = R.string.msg_preference_saved)
                val highlightSavedMsg = stringResource(id = R.string.msg_highlight_saved)
                val pinSavedMsg = stringResource(id = R.string.msg_pin_saved)
                val showAppNames = state.showAppNames
                val showUsageBadges = state.showUsageBadges
                val (showSettings, setShowSettings) = remember { mutableStateOf(false) }
                val selectedProfile = state.selectedProfile

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = if (state.useSystemWallpaper) androidx.compose.material3.MaterialTheme.colorScheme.background.copy(alpha = 0.65f) else androidx.compose.material3.MaterialTheme.colorScheme.background,
                    topBar = {
                        Column {
                            TopAppBar(
                                title = { Text(stringResource(id = R.string.app_name)) },
                                actions = {
                                    IconButton(onClick = { setShowSettings(true) }) {
                                        Text(stringResource(id = R.string.settings_icon))
                                    }
                                },
                                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                                    containerColor = if (state.useSystemWallpaper) androidx.compose.ui.graphics.Color.Transparent else androidx.compose.material3.MaterialTheme.colorScheme.surface
                                )
                            )
                            if (state.hasWorkProfile) {
                                PrimaryTabRow(
                                    selectedTabIndex = if (selectedProfile == Profile.PERSONAL) 0 else 1,
                                    containerColor = if (state.useSystemWallpaper) androidx.compose.ui.graphics.Color.Transparent else androidx.compose.material3.MaterialTheme.colorScheme.surface
                                ) {
                                    Tab(selected = selectedProfile == Profile.PERSONAL, onClick = { viewModel.submitIntent(digital.tonima.bubbleslauncher.ui.MainViewModel.MainIntent.SelectProfile(Profile.PERSONAL)) }) {
                                        Text(stringResource(id = R.string.tab_personal))
                                    }
                                    Tab(selected = selectedProfile == Profile.WORK, onClick = { viewModel.submitIntent(digital.tonima.bubbleslauncher.ui.MainViewModel.MainIntent.SelectProfile(Profile.WORK)) }) {
                                        Text(stringResource(id = R.string.tab_work))
                                    }
                                }
                            }
                        }
                    }, snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
                            if (showUsageRationale) {
                                AlertDialog(
                                    onDismissRequest = { setShowUsageRationale(false) },
                                    title = { Text(stringResource(id = R.string.usage_access_title)) },
                                    text = { Text(stringResource(id = R.string.usage_access_message)) },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            setShowUsageRationale(false)
                                            try {
                                                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                startActivity(intent)
                                            } catch (_: Exception) { }
                                        }) { Text(stringResource(id = R.string.usage_access_button)) }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { setShowUsageRationale(false) }) { Text(stringResource(id = R.string.permission_rationale_cancel)) }
                                    }
                                )
                            }
                            if (showSettings) {
                                SettingsScreen(
                                    showNames = showAppNames,
                                    showUsageBadges = showUsageBadges,
                                    ignoreDynamicSize = state.ignoreDynamicSize,
                                    useSystemWallpaper = state.useSystemWallpaper,
                                    iconSizeDp = state.iconSizeDp,
                                    onToggleShowNames = { viewModel.submitIntent(MainViewModel.MainIntent.ToggleShowAppNames) },
                                    onToggleShowUsageBadges = { viewModel.submitIntent(MainViewModel.MainIntent.ToggleShowUsageBadges) },
                                    onToggleIgnoreDynamicSize = { viewModel.submitIntent(MainViewModel.MainIntent.ToggleIgnoreDynamicSize) },
                                    onToggleUseSystemWallpaper = { viewModel.submitIntent(MainViewModel.MainIntent.ToggleUseSystemWallpaper) },
                                    onSetIconSize = { dp -> viewModel.submitIntent(MainViewModel.MainIntent.SetIconSize(dp)) },
                                    onSetThemeMode = { mode: String -> viewModel.submitIntent(MainViewModel.MainIntent.SetThemeMode(mode)) },
                                    themeMode = state.themeMode,
                                    onBack = { setShowSettings(false) },
                                    modifier = Modifier.padding(innerPadding)
                                )
                            } else {
                        MainScreen(
                            state = state,
                            onIntent = { intent -> viewModel.submitIntent(intent) },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
                LaunchedEffect(Unit) {
                    viewModel.submitIntent(MainViewModel.MainIntent.LoadApps)
                }
                LaunchedEffect(Unit) {
                    viewModel.events.collect { event ->
                        when (event) {
                            is MainViewModel.UiEvent.PreferenceSaved -> {
                                snackbarHostState.showSnackbar(prefSavedMsg)
                            }
                            is MainViewModel.UiEvent.HighlightSaved -> {
                                snackbarHostState.showSnackbar(highlightSavedMsg)
                            }
                            is MainViewModel.UiEvent.PinSaved -> {
                                snackbarHostState.showSnackbar(pinSavedMsg)
                            }
                            is MainViewModel.UiEvent.Message -> {
                                snackbarHostState.showSnackbar(event.text)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.submitIntent(MainViewModel.MainIntent.LoadApps)
    }
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    showNames: Boolean,
    showUsageBadges: Boolean = true,
    ignoreDynamicSize: Boolean = false,
    useSystemWallpaper: Boolean = false,
    iconSizeDp: Int = 64,
    themeMode: String = "system",
    onToggleShowNames: () -> Unit,
    onToggleShowUsageBadges: () -> Unit = {},
    onToggleIgnoreDynamicSize: () -> Unit = {},
    onToggleUseSystemWallpaper: () -> Unit = {},
    onSetIconSize: (Int) -> Unit = {},
    onSetThemeMode: (String) -> Unit = {},
    onBack: () -> Unit
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(text = stringResource(id = R.string.settings_title), modifier = Modifier.padding(bottom = 8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = stringResource(id = R.string.label_show_names))
            Switch(checked = showNames, onCheckedChange = { onToggleShowNames() })
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = stringResource(id = R.string.label_show_usage_badges))
            Switch(checked = showUsageBadges, onCheckedChange = { onToggleShowUsageBadges() })
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = stringResource(id = R.string.label_ignore_dynamic_size))
            Switch(checked = ignoreDynamicSize, onCheckedChange = { onToggleIgnoreDynamicSize() })
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = stringResource(id = R.string.label_use_system_wallpaper))
            Switch(checked = useSystemWallpaper, onCheckedChange = { onToggleUseSystemWallpaper() })
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Column {
                val min = 32f
                val maxValue = 128f
                val stepSize = 10f
                val stepsCount = ((maxValue - min) / stepSize).roundToInt()
                val stepsParam = max(0, stepsCount - 1)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = stringResource(id = R.string.label_icon_size))
                    Text(text = "${iconSizeDp} dp")
                }

                androidx.compose.material3.Slider(
                    value = iconSizeDp.toFloat(),
                    onValueChange = { raw ->
                        val rounded = (raw / stepSize).roundToInt() * stepSize
                        val intRounded = rounded.toInt()
                        if (intRounded != iconSizeDp) {
                            onSetIconSize(intRounded)
                        }
                    },
                    valueRange = min..maxValue,
                    steps = stepsParam,
                    enabled = ignoreDynamicSize
                )
            }
        }
        Text(text = stringResource(id = R.string.label_theme), modifier = Modifier.padding(top = 12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row {
                RadioButton(selected = themeMode == "system", onClick = { onSetThemeMode("system") })
                Text(text = stringResource(id = R.string.theme_system), modifier = Modifier.padding(start = 8.dp))
            }
            Row {
                RadioButton(selected = themeMode == "light", onClick = { onSetThemeMode("light") })
                Text(text = stringResource(id = R.string.theme_light), modifier = Modifier.padding(start = 8.dp))
            }
            Row {
                RadioButton(selected = themeMode == "dark", onClick = { onSetThemeMode("dark") })
                Text(text = stringResource(id = R.string.theme_dark), modifier = Modifier.padding(start = 8.dp))
            }
        }
        Button(onClick = { onBack() }, modifier = Modifier.padding(top = 16.dp)) {
            Text(stringResource(id = R.string.button_back))
        }
    }
}
