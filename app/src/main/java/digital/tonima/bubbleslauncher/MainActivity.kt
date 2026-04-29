package digital.tonima.bubbleslauncher

import androidx.compose.material3.Switch
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import kotlin.math.roundToInt
import dagger.hilt.android.AndroidEntryPoint
import digital.tonima.bubbleslauncher.data.Profile
import digital.tonima.bubbleslauncher.ui.MainScreen
import digital.tonima.bubbleslauncher.ui.MainViewModel
import digital.tonima.bubbleslauncher.model.AppInfo
import digital.tonima.bubbleslauncher.ui.OnboardingScreen
import digital.tonima.bubbleslauncher.ui.theme.BubblesLauncherTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import android.app.AppOpsManager
import android.app.role.RoleManager
import android.content.Intent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri

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

                // Show onboarding fullscreen, covering everything including the TopAppBar
                if (!state.hasSeenOnboarding) {
                    OnboardingScreen(
                        onComplete = { viewModel.submitIntent(MainViewModel.MainIntent.CompleteOnboarding) },
                        modifier = Modifier.fillMaxSize()
                    )
                    return@BubblesLauncherTheme
                }

                val (showDefaultLauncherPrompt, setShowDefaultLauncherPrompt) = remember { mutableStateOf(false) }
                val (dismissedDefaultLauncherPromptThisSession, setDismissedDefaultLauncherPromptThisSession) = remember { mutableStateOf(false) }

                val (showUsageRationale, setShowUsageRationale) = remember { mutableStateOf(false) }
                @Suppress("DEPRECATION")
                fun hasUsageAccess(ctx: Context): Boolean {
                    return try {
                        val appOps = ctx.getSystemService(AppOpsManager::class.java)
                        val mode = appOps.noteOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), ctx.packageName)
                        mode == AppOpsManager.MODE_ALLOWED
                    } catch (_: Throwable) {
                        false
                    }
                }

                val (hasUsage, setHasUsage) = remember { mutableStateOf(hasUsageAccess(this@MainActivity)) }
                val needsUsageAccess = !state.ignoreDynamicSize || state.showUsageBadges

                LaunchedEffect(state.apps, state.ignoreDynamicSize, state.showUsageBadges) {
                    val usageGranted = hasUsageAccess(this@MainActivity)
                    setHasUsage(usageGranted)
                    setShowUsageRationale(needsUsageAccess && !usageGranted)
                }

                LaunchedEffect(state.apps, dismissedDefaultLauncherPromptThisSession) {
                    if (dismissedDefaultLauncherPromptThisSession) {
                        setShowDefaultLauncherPrompt(false)
                    } else {
                        setShowDefaultLauncherPrompt(!isDefaultLauncher())
                    }
                }

                val snackbarHostState = remember { SnackbarHostState() }
                val prefSavedMsg = stringResource(id = R.string.msg_preference_saved)
                val highlightSavedMsg = stringResource(id = R.string.msg_highlight_saved)
                val pinSavedMsg = stringResource(id = R.string.msg_pin_saved)
                val showAppNames = state.showAppNames
                val showUsageBadges = state.showUsageBadges
                val (showSettings, setShowSettings) = remember { mutableStateOf(false) }
                val (showMetrics, setShowMetrics) = remember { mutableStateOf(false) }
                val selectedProfile = state.selectedProfile

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = if (state.useSystemWallpaper) androidx.compose.material3.MaterialTheme.colorScheme.background.copy(alpha = 0.65f) else androidx.compose.material3.MaterialTheme.colorScheme.background,
                    topBar = {
                        if (!showSettings && !showMetrics) {
                            Column(
                                modifier = Modifier
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                androidx.compose.material3.MaterialTheme.colorScheme.surface,
                                                androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                            )
                                        )
                                    )
                                    .shadow(4.dp)
                            ) {
                                TopAppBar(
                                    title = { Text(stringResource(id = R.string.app_name)) },
                                    actions = {
                                        TextButton(
                                            onClick = { viewModel.submitIntent(MainViewModel.MainIntent.ToggleFocusMode) }
                                        ) {
                                            Text(
                                                text = stringResource(id = R.string.toggle_focus_mode),
                                                color = if (state.isFocusModeEnabled) androidx.compose.material3.MaterialTheme.colorScheme.primary else androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        TextButton(onClick = { setShowSettings(true) }) {
                                            Text(stringResource(id = R.string.settings_title))
                                        }
                                    },
                                    colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                                        containerColor = androidx.compose.ui.graphics.Color.Transparent
                                    )
                                )
                                if (state.hasWorkProfile) {
                                    PrimaryTabRow(
                                        selectedTabIndex = if (selectedProfile == Profile.PERSONAL) 0 else 1,
                                        containerColor = androidx.compose.ui.graphics.Color.Transparent
                                    ) {
                                        Tab(selected = selectedProfile == Profile.PERSONAL, onClick = { viewModel.submitIntent(MainViewModel.MainIntent.SelectProfile(Profile.PERSONAL)) }) {
                                            Text(stringResource(id = R.string.tab_personal))
                                        }
                                        Tab(selected = selectedProfile == Profile.WORK, onClick = { viewModel.submitIntent(MainViewModel.MainIntent.SelectProfile(Profile.WORK)) }) {
                                            Text(stringResource(id = R.string.tab_work))
                                        }
                                    }
                                }
                            }
                        }
                    }, snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
                            if (showDefaultLauncherPrompt) {
                                AlertDialog(
                                    onDismissRequest = {
                                        setShowDefaultLauncherPrompt(false)
                                        setDismissedDefaultLauncherPromptThisSession(true)
                                    },
                                    title = { Text(stringResource(id = R.string.default_launcher_prompt_title)) },
                                    text = { Text(stringResource(id = R.string.default_launcher_prompt_message)) },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            setShowDefaultLauncherPrompt(false)
                                            requestSetAsDefaultLauncher()
                                        }) { Text(stringResource(id = R.string.default_launcher_prompt_action)) }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = {
                                            setShowDefaultLauncherPrompt(false)
                                            setDismissedDefaultLauncherPromptThisSession(true)
                                        }) { Text(stringResource(id = R.string.permission_rationale_cancel)) }
                                    }
                                )
                            }
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
                                    themeMode = state.themeMode,
                                    hiddenAppsList = state.hiddenApps.mapNotNull { pkg -> state.apps.find { it.packageName == pkg } },
                                    hiddenAppPackages = state.hiddenApps,
                                    onToggleShowNames = { viewModel.submitIntent(MainViewModel.MainIntent.ToggleShowAppNames) },
                                    onToggleShowUsageBadges = { viewModel.submitIntent(MainViewModel.MainIntent.ToggleShowUsageBadges) },
                                    onToggleIgnoreDynamicSize = { viewModel.submitIntent(MainViewModel.MainIntent.ToggleIgnoreDynamicSize) },
                                    onToggleUseSystemWallpaper = { viewModel.submitIntent(MainViewModel.MainIntent.ToggleUseSystemWallpaper) },
                                    onSetIconSize = { dp -> viewModel.submitIntent(MainViewModel.MainIntent.SetIconSize(dp)) },
                                    onSetThemeMode = { mode: String -> viewModel.submitIntent(MainViewModel.MainIntent.SetThemeMode(mode)) },
                                    onUnhideApp = { pkg ->
                                        viewModel.submitIntent(MainViewModel.MainIntent.ToggleHiddenApp(pkg)) 
                                    },
                                    onOpenMetrics = {
                                        setShowSettings(false)
                                        setShowMetrics(true)
                                    },
                                    onBack = { setShowSettings(false) },
                                    modifier = Modifier.padding(innerPadding)
                                )
                            } else if (showMetrics) {
                                digital.tonima.bubbleslauncher.ui.metrics.MetricsDashboardScreen(
                                    modifier = Modifier.padding(innerPadding),
                                    onBack = { setShowMetrics(false) }
                                )
                            } else {
                                Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                                    if (needsUsageAccess && !hasUsage) {
                                        androidx.compose.material3.ElevatedCard(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
                                                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer
                                            )
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Text(stringResource(id = R.string.usage_access_title), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer)
                                                Text(stringResource(id = R.string.usage_access_message), color = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                                                Button(
                                                    onClick = { 
                                                        try {
                                                            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                            startActivity(intent)
                                                        } catch (_: Exception) {}
                                                    },
                                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer,
                                                        contentColor = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer
                                                    )
                                                ) {
                                                    Text(stringResource(id = R.string.usage_access_button))
                                                }
                                            }
                                        }
                                    }
                                    MainScreen(
                                        state = state,
                                        onIntent = { intent -> viewModel.submitIntent(intent) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
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

    private fun isDefaultLauncher(): Boolean {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        val defaultHomePackage = resolveInfo?.activityInfo?.packageName
        return defaultHomePackage == packageName
    }

    private fun requestSetAsDefaultLauncher() {
        fun launchIfAvailable(intent: Intent): Boolean {
            return if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
                true
            } else {
                false
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val roleManager = getSystemService(RoleManager::class.java)
                if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    val roleIntent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                    if (launchIfAvailable(roleIntent)) {
                        return
                    }
                }
            } catch (_: Exception) {
                // Fallbacks below
            }
        }

        try {
            if (launchIfAvailable(Intent(Settings.ACTION_HOME_SETTINGS))) {
                return
            }
        } catch (_: Exception) {
            // Fallback below
        }

        try {
            if (launchIfAvailable(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))) {
                return
            }
        } catch (_: Exception) {
            // Fallback below
        }

        // Last fallback: open HOME intent to at least trigger chooser on some OEM ROMs.
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            if (launchIfAvailable(homeIntent)) {
                return
            }
        } catch (_: Exception) {
            // Nothing else we can do
        }

        Toast.makeText(this, getString(R.string.default_launcher_prompt_unavailable), Toast.LENGTH_SHORT).show()
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
    hiddenAppsList: List<AppInfo> = emptyList(),
    hiddenAppPackages: List<String> = emptyList(),
    onToggleShowNames: () -> Unit,
    onToggleShowUsageBadges: () -> Unit = {},
    onToggleIgnoreDynamicSize: () -> Unit = {},
    onToggleUseSystemWallpaper: () -> Unit = {},
    onSetIconSize: (Int) -> Unit = {},
    onSetThemeMode: (String) -> Unit = {},
    onUnhideApp: (String) -> Unit = {},
    onOpenMetrics: () -> Unit = {},
    onBack: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.settings_title),
                style = androidx.compose.material3.MaterialTheme.typography.headlineLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold
            )
        }

        // APARÊNCIA SECTION
        SettingsSectionCard(
            title = "Aparencia",
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Show Names Toggle
                    SettingsToggleItem(
                        label = stringResource(id = R.string.label_show_names),
                        description = "Exibir nomes dos aplicativos",
                        checked = showNames,
                        onCheckedChange = { onToggleShowNames() }
                    )

                    // Usage Badges Toggle
                    SettingsToggleItem(
                        label = stringResource(id = R.string.label_show_usage_badges),
                        description = "Mostrar tempo de uso",
                        checked = showUsageBadges,
                        onCheckedChange = { onToggleShowUsageBadges() }
                    )

                    // System Wallpaper Toggle
                    SettingsToggleItem(
                        label = stringResource(id = R.string.label_use_system_wallpaper),
                        description = "Usar papel de parede do sistema",
                        checked = useSystemWallpaper,
                        onCheckedChange = { onToggleUseSystemWallpaper() }
                    )

                    // Icon Size Slider
                    SettingsSliderItem(
                        label = stringResource(id = R.string.label_icon_size),
                        description = "Tamanho dos ícones: ${iconSizeDp} dp",
                        value = iconSizeDp.toFloat(),
                        onValueChange = { raw ->
                            val stepSize = 10f
                            val rounded = (raw / stepSize).roundToInt() * stepSize
                            val intRounded = rounded.toInt()
                            if (intRounded != iconSizeDp) {
                                onSetIconSize(intRounded)
                            }
                        },
                        min = 32f,
                        max = 128f,
                        enabled = ignoreDynamicSize
                    )
                }
            }
        )

        // COMPORTAMENTO SECTION
        SettingsSectionCard(
            title = "Comportamento",
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SettingsToggleItem(
                        label = stringResource(id = R.string.label_ignore_dynamic_size),
                        description = "Usar tamanho fixo para ícones",
                        checked = ignoreDynamicSize,
                        onCheckedChange = { onToggleIgnoreDynamicSize() }
                    )
                }
            }
        )

        // TEMA SECTION
        SettingsSectionCard(
            title = "Tema",
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(
                        "system" to stringResource(id = R.string.theme_system),
                        "light" to stringResource(id = R.string.theme_light),
                        "dark" to stringResource(id = R.string.theme_dark)
                    ).forEach { (mode, label) ->
                        SettingsRadioItem(
                            label = label,
                            selected = themeMode == mode,
                            onClick = { onSetThemeMode(mode) }
                        )
                    }
                }
            }
        )

        SettingsSectionCard(
            title = stringResource(id = R.string.metrics_title),
            content = {
                Button(
                    onClick = onOpenMetrics,
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Text(text = stringResource(id = R.string.metrics_title))
                }
            }
        )

        // HIDDEN APPS SECTION
        if (hiddenAppsList.isNotEmpty() || hiddenAppPackages.isNotEmpty()) {
            val hiddenAppsByPackage = hiddenAppsList.associateBy { it.packageName }
            SettingsSectionCard(
                title = "Aplicativos ocultos (${hiddenAppPackages.size})",
                content = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        hiddenAppPackages.forEach { pkg ->
                            val app = hiddenAppsByPackage[pkg]
                            val appLabel = app?.appName ?: pkg
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Text(
                                    text = appLabel,
                                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                )
                                TextButton(onClick = { onUnhideApp(pkg) }) {
                                    Text(stringResource(id = R.string.menu_unhide))
                                }
                            }
                        }
                    }
                }
            )
        }

        // Back Button
        Button(
            onClick = { onBack() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(top = 8.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.button_back),
                fontSize = 16.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    androidx.compose.material3.ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(5.dp, androidx.compose.foundation.shape.RoundedCornerShape(20.dp)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            androidx.compose.material3.MaterialTheme.colorScheme.surface,
                            androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = title,
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
            }
            content()
        }
    }
}

@Composable
fun SettingsToggleItem(
    label: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
            if (description != null) {
                Text(
                    text = description,
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        androidx.compose.material3.Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SettingsSliderItem(
    label: String,
    description: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    min: Float,
    max: Float,
    enabled: Boolean = true
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            )
            Text(
                text = description,
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        androidx.compose.material3.Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max,
            steps = ((max - min) / 10f).roundToInt() - 1,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun SettingsRadioItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
        )
    }
}
