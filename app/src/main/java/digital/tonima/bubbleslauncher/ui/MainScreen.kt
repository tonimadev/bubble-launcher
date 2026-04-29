package digital.tonima.bubbleslauncher.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import android.annotation.SuppressLint
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.derivedStateOf
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import digital.tonima.bubbleslauncher.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import digital.tonima.bubbleslauncher.model.AppInfo
import androidx.core.net.toUri
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@SuppressLint("MissingPermission")
@Composable
fun MainScreen(
    state: MainViewModel.MainUiState,
    onIntent: (MainViewModel.MainIntent) -> Unit,
    modifier: Modifier = Modifier,
    bubblesViewModel: BubblesViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    
    var menuApp by remember { mutableStateOf<AppInfo?>(null) }
    var delayAppToLaunch by remember { mutableStateOf<AppInfo?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        MentalAquariumBackground(viewModel = bubblesViewModel)

        // Use Sets for O(1) contains checks — avoids O(n²) filtering on each recomposition
        val hiddenSet by remember(state.hiddenApps) { derivedStateOf { state.hiddenApps.toHashSet() } }
        val essentialSet by remember(state.essentialApps) { derivedStateOf { state.essentialApps.toHashSet() } }

        // Hoist expensive filtering with remember so it only re-runs when inputs actually change
        val visibleApps by remember(state.apps, hiddenSet, state.isFocusModeEnabled, essentialSet) {
            derivedStateOf {
                var filtered = state.apps.filter { !hiddenSet.contains(it.packageName) }
                if (state.isFocusModeEnabled) {
                    filtered = filtered.filter { essentialSet.contains(it.packageName) }
                }
                filtered
            }
        }
        val maxTime by remember(visibleApps) {
            derivedStateOf { visibleApps.maxOfOrNull { it.totalTimeInForeground } ?: 1L }
        }

        // GridCells.Adaptive replaces BoxWithConstraints + Fixed(columns) — same result, less overhead
        LazyVerticalGrid(
            columns = GridCells.Adaptive(96.dp),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            content = {
                // key = packageName + userHandle ensures uniqueness even when the same package
                // exists in both personal and work profiles (same packageName, different userHandle)
                items(visibleApps, key = { "${it.packageName}_${it.userHandle}" }) { app ->
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        AppBubble(
                            app = app,
                            maxTime = maxTime,
                            highlighted = state.highlightedApps.contains(app.packageName),
                            showName = state.showAppNames,
                            showUsageBadges = state.showUsageBadges,
                            ignoreDynamicSize = state.ignoreDynamicSize,
                            iconSizeDp = state.iconSizeDp,
                            onClick = {
                                if (state.delayApps.contains(app.packageName)) {
                                    delayAppToLaunch = app
                                } else {
                                    try {
                                        if (app.componentName != null && app.userHandle != null) {
                                            val launcherApps = context.getSystemService(android.content.pm.LauncherApps::class.java)
                                            launcherApps?.startMainActivity(app.componentName, app.userHandle, null, null)
                                        } else {
                                            val intent = packageManager.getLaunchIntentForPackage(app.packageName)
                                            if (intent != null) {
                                                context.startActivity(intent)
                                            }
                                        }
                                    } catch (_: Throwable) {
                                        try {
                                            val infoIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                "package:${app.packageName}".toUri()).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                            context.startActivity(infoIntent)
                                        } catch (_: Exception) { /* ignore */ }
                                    }
                                }
                            },
                            onLongClick = { menuApp = app }
                        )

                        if (menuApp?.packageName == app.packageName) {
                            DropdownMenu(
                                expanded = true,
                                onDismissRequest = { menuApp = null }
                            ) {
                                val isHighlighted = state.highlightedApps.contains(app.packageName)
                                DropdownMenuItem(
                                    text = { Text(if (isHighlighted) stringResource(id = R.string.menu_unhighlight) else stringResource(id = R.string.menu_highlight)) },
                                    onClick = {
                                        onIntent(MainViewModel.MainIntent.ToggleHighlight(app.packageName))
                                        menuApp = null
                                    }
                                )
                                    val isPinned = state.pinnedApps.contains(app.packageName)
                                    DropdownMenuItem(
                                        text = { Text(if (isPinned) stringResource(id = R.string.menu_unpin) else stringResource(id = R.string.menu_pin)) },
                                        onClick = {
                                            onIntent(MainViewModel.MainIntent.TogglePin(app.packageName))
                                            menuApp = null
                                        }
                                    )
                                val isDelay = state.delayApps.contains(app.packageName)
                                DropdownMenuItem(
                                    text = { Text(if (isDelay) stringResource(id = R.string.menu_remove_delay) else stringResource(id = R.string.menu_add_delay)) },
                                    onClick = {
                                        onIntent(MainViewModel.MainIntent.ToggleDelayApp(app.packageName))
                                        menuApp = null
                                    }
                                )
                                val isEssential = state.essentialApps.contains(app.packageName)
                                DropdownMenuItem(
                                    text = { Text(if (isEssential) stringResource(id = R.string.menu_remove_essential) else stringResource(id = R.string.menu_add_essential)) },
                                    onClick = {
                                        onIntent(MainViewModel.MainIntent.ToggleEssentialApp(app.packageName))
                                        menuApp = null
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.menu_hide)) },
                                    onClick = {
                                        onIntent(MainViewModel.MainIntent.ToggleHiddenApp(app.packageName))
                                        menuApp = null
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(id = R.string.menu_app_info)) },
                                    onClick = {
                                        try {
                                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                "package:${app.packageName}".toUri()).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            val intent = Intent(Intent.ACTION_VIEW,
                                                "market://details?id=${app.packageName}".toUri()).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        }
                                        menuApp = null
                                    }
                                )
                            }
                        }
                    }
                    }
            }
        )
    if (delayAppToLaunch != null) {
        MindfulDelayOverlay(
            app = delayAppToLaunch!!,
            onCancel = { 
                val colors = listOf(
                    Color(0xFFFF5252), // Red
                    Color(0xFF448AFF), // Blue
                    Color(0xFF69F0AE), // Green
                    Color(0xFFFFD740), // Yellow
                    Color(0xFFE040FB), // Purple
                    Color(0xFF18FFFF), // Cyan
                    Color(0xFFFF4081)  // Pink
                )
                // Pass packageName so the event is persisted in the DB for metrics
                bubblesViewModel.onImpulseResisted(colors.random(), delayAppToLaunch?.packageName ?: "")
                delayAppToLaunch = null
            },
            onContinue = {
                val app = delayAppToLaunch!!
                delayAppToLaunch = null
                // Record that the impulse was NOT resisted
                bubblesViewModel.onImpulseGiven(app.packageName)
                try {
                    if (app.componentName != null && app.userHandle != null) {
                        val launcherApps = context.getSystemService(android.content.pm.LauncherApps::class.java)
                        launcherApps?.startMainActivity(app.componentName, app.userHandle, null, null)
                    } else {
                        val intent = packageManager.getLaunchIntentForPackage(app.packageName)
                        if (intent != null) {
                            context.startActivity(intent)
                        }
                    }
                } catch (_: Throwable) {
                    try {
                        val infoIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            "package:${app.packageName}".toUri()).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                        context.startActivity(infoIntent)
                    } catch (_: Exception) { /* ignore */ }
                }
            }
        )
    }
}
}


@Composable
fun AppBubble(
    app: AppInfo,
    maxTime: Long,
    highlighted: Boolean,
    showName: Boolean,
    showUsageBadges: Boolean,
    ignoreDynamicSize: Boolean,
    iconSizeDp: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val normalizedSize = if (maxTime > 0) {
        48 + (app.totalTimeInForeground.toFloat() / maxTime * (120 - 48))
    } else {
        48f
    }.dp

    val resolvedSize = if (ignoreDynamicSize) {
        iconSizeDp.dp
    } else {
        normalizedSize
    }

    val grayscaleMatrix = remember {
        ColorMatrix().apply { setToSaturation(0f) }
    }

    // Load icon at a fixed resolution (128px) regardless of the displayed bubble size.
    // This is the key fix: previously targetPx was a key, causing ALL icons to reload from
    // disk every time usage stats changed (maxTime changed → targetPx changed for every app).
    // Now the icon only reloads when the package actually changes.
    val context = LocalContext.current
    val imageBitmap by produceState<ImageBitmap?>(initialValue = null, app.packageName, app.userHandle) {
        value = withContext(Dispatchers.IO) {
            try {
                val pm = context.packageManager
                val drawable = if (app.userHandle != null) {
                    val launcherApps = context.getSystemService(android.content.pm.LauncherApps::class.java)
                    val activities = launcherApps?.getActivityList(app.packageName, app.userHandle)
                    activities?.firstOrNull()?.getBadgedIcon(0) ?: pm.getApplicationIcon(app.packageName)
                } else {
                    pm.getApplicationIcon(app.packageName)
                }
                try {
                    drawable.toBitmap(width = 128, height = 128).asImageBitmap()
                } catch (_: Throwable) {
                    drawable.toBitmap().asImageBitmap()
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    Column(
        modifier = Modifier
            .padding(8.dp)
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = { onLongClick() }
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(resolvedSize)) {
            val currentImage = imageBitmap
            if (currentImage != null) {
                Image(
                    bitmap = currentImage,
                    contentDescription = app.appName,
                    modifier = Modifier.fillMaxSize(),
                    colorFilter = if (highlighted) null else ColorFilter.colorMatrix(grayscaleMatrix)
                )
            }
            if (showUsageBadges && app.totalTimeInForeground > 0) {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(app.totalTimeInForeground)
                if (minutes > 0) {
                    val hours = minutes / 60
                    val mins = minutes % 60
                    val timeStr = if (hours > 0) "${hours}h${mins}m" else "${mins}m"
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .background(Color.Black.copy(alpha = 0.7f), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(text = timeStr, fontSize = 10.sp, color = Color.White)
                    }
                }
            }
        }

        if (showName && resolvedSize >= 60.dp) {
            Text(
                text = app.appName,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                style = androidx.compose.ui.text.TextStyle(
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = androidx.compose.material3.MaterialTheme.colorScheme.background,
                        offset = androidx.compose.ui.geometry.Offset(0f, 2f),
                        blurRadius = 8f
                    )
                ),
                modifier = Modifier.padding(top = 4.dp).size(width = resolvedSize, height = 20.dp)
            )
        }
    }
}

@Composable
fun MindfulDelayOverlay(
    app: AppInfo,
    onCancel: () -> Unit,
    onContinue: () -> Unit
) {
    var timeLeft by remember { mutableStateOf(5) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            kotlinx.coroutines.delay(1000L)
            timeLeft--
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = { onCancel() }) {
        androidx.compose.material3.Surface(
            shape = RoundedCornerShape(16.dp),
            color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.dialog_delay_title),
                    style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = stringResource(id = R.string.dialog_delay_message).replace("este aplicativo", app.appName),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    androidx.compose.material3.TextButton(onClick = onCancel) {
                        Text(stringResource(id = R.string.dialog_delay_cancel))
                    }
                    androidx.compose.material3.Button(
                        onClick = onContinue,
                        enabled = timeLeft == 0
                    ) {
                        Text(
                            text = if (timeLeft > 0) String.format(stringResource(id = R.string.dialog_delay_continue), timeLeft) 
                                   else stringResource(id = R.string.dialog_delay_continue_ready)
                        )
                    }
                }
            }
        }
    }
}