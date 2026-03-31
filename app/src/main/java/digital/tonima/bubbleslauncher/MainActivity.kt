package digital.tonima.bubbleslauncher

import android.Manifest
import android.os.Build
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
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { _result ->
            viewModel.submitIntent(MainViewModel.MainIntent.LoadApps)
        }
        setContent {
                val state by viewModel.uiState.collectAsState()
                // Apply theme based on user's selection in settings
                val themeMode = state.themeMode
                val useDark = when (themeMode) {
                    "light" -> false
                    "dark" -> true
                    else -> isSystemInDarkTheme()
                }

                BubblesLauncherTheme(darkTheme = useDark) {
                    val (isRationaleVisible, setRationaleVisible) = remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        val needed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
                        } else {
                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                        val allGranted = needed.all { perm ->
                            ContextCompat.checkSelfPermission(this@MainActivity, perm) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        }
                        if (!allGranted) {
                            val shouldShow = needed.any { perm ->
                                ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, perm)
                            }
                            if (shouldShow) {
                                setRationaleVisible(true)
                            } else {
                                permissionLauncher.launch(needed)
                            }
                        }
                    }

                val snackbarHostState = remember { SnackbarHostState() }
                val prefSavedMsg = stringResource(id = R.string.msg_preference_saved)
                val highlightSavedMsg = stringResource(id = R.string.msg_highlight_saved)
                val pinSavedMsg = stringResource(id = R.string.msg_pin_saved)
                val showAppNames = state.showAppNames
                val (showSettings, setShowSettings) = remember { mutableStateOf(false) }
                val selectedProfile = state.selectedProfile

                Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
                    Column {
                        TopAppBar(
                            title = { Text(stringResource(id = R.string.app_name)) },
                            actions = {
                                IconButton(onClick = { setShowSettings(true) }) {
                                    Text(stringResource(id = R.string.settings_icon))
                                }
                            }
                        )
                        PrimaryTabRow(selectedTabIndex = if (selectedProfile == Profile.PERSONAL) 0 else 1) {
                            Tab(selected = selectedProfile == Profile.PERSONAL, onClick = { viewModel.submitIntent(digital.tonima.bubbleslauncher.ui.MainViewModel.MainIntent.SelectProfile(Profile.PERSONAL)) }) {
                                Text(stringResource(id = R.string.tab_personal))
                            }
                            Tab(selected = selectedProfile == Profile.WORK, onClick = { viewModel.submitIntent(digital.tonima.bubbleslauncher.ui.MainViewModel.MainIntent.SelectProfile(Profile.WORK)) }) {
                                Text(stringResource(id = R.string.tab_work))
                            }
                        }
                    }
                }, snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
                            if (isRationaleVisible) {
                                AlertDialog(
                                    onDismissRequest = { setRationaleVisible(false) },
                                    title = { Text(stringResource(id = R.string.permission_rationale_title)) },
                                    text = { Text(stringResource(id = R.string.permission_rationale_message)) },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            setRationaleVisible(false)
                                            val needed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
                                            } else {
                                                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                                            }
                                            permissionLauncher.launch(needed)
                                        }) {
                                            Text(stringResource(id = R.string.permission_rationale_ok))
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { setRationaleVisible(false) }) {
                                            Text(stringResource(id = R.string.permission_rationale_cancel))
                                        }
                                    }
                                )
                            }
                            if (showSettings) {
                                SettingsScreen(
                                    showNames = showAppNames,
                                    ignoreDynamicSize = state.ignoreDynamicSize,
                                    useSystemWallpaper = state.useSystemWallpaper,
                                    iconSizeDp = state.iconSizeDp,
                                    onToggleShowNames = { viewModel.submitIntent(MainViewModel.MainIntent.ToggleShowAppNames) },
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
    ignoreDynamicSize: Boolean = false,
    useSystemWallpaper: Boolean = false,
    iconSizeDp: Int = 64,
    themeMode: String = "system",
    onToggleShowNames: () -> Unit,
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
        // Theme selection
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
