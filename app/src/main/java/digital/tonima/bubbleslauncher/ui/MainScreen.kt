package digital.tonima.bubbleslauncher.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import android.app.WallpaperManager
import androidx.compose.ui.layout.ContentScale
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.res.stringResource
import digital.tonima.bubbleslauncher.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import digital.tonima.bubbleslauncher.model.AppInfo
import androidx.core.net.toUri

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@SuppressLint("MissingPermission")
@Composable
fun MainScreen(
    state: MainViewModel.MainUiState,
    onIntent: (MainViewModel.MainIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val packageManager = context.packageManager
    
    val wallpaperBitmap = remember {
        runCatching {
            val wallpaperManager = WallpaperManager.getInstance(context)
            wallpaperManager.drawable?.toBitmap()
        }.getOrNull()
    }

    var menuApp by remember { mutableStateOf<AppInfo?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (state.useSystemWallpaper && wallpaperBitmap != null) {
            Image(
                bitmap = wallpaperBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        val maxTime = state.apps.maxOfOrNull { it.totalTimeInForeground } ?: 1L
        // The ViewModel provides `state.apps` already ordered with pinned apps first.
        val sortedApps = state.apps

        androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val cellMinWidth = 96.dp
            val columns = maxOf(1, (maxWidth / cellMinWidth).toInt())

            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = {
                    items(sortedApps) { app ->
                    Box {
                        AppBubble(
                            app = app,
                            maxTime = maxTime,
                            highlighted = state.highlightedApps.contains(app.packageName),
                            showName = state.showAppNames,
                            ignoreDynamicSize = state.ignoreDynamicSize,
                            iconSizeDp = state.iconSizeDp,
                            onClick = {
                                val intent = packageManager.getLaunchIntentForPackage(app.packageName)
                                if (intent != null) {
                                    context.startActivity(intent)
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
                                            // Fallback: open Play Store page for the app
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
        }
    }
}

@Composable
fun AppBubble(
    app: AppInfo,
    maxTime: Long,
    highlighted: Boolean,
    showName: Boolean,
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

    // Convert dp size to pixels for bitmap creation and remember the scaled bitmap
    val density = LocalDensity.current
    val targetPx = with(density) { resolvedSize.roundToPx() }
    val imageBitmap = remember(app.packageName, targetPx) {
        // create a scaled bitmap to avoid large allocations during fast scroll
        try {
            app.icon.toBitmap(width = targetPx.coerceAtLeast(1), height = targetPx.coerceAtLeast(1)).asImageBitmap()
        } catch (t: Throwable) {
            // fallback to intrinsic size conversion if scaling fails
            app.icon.toBitmap().asImageBitmap()
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
        Image(
            bitmap = imageBitmap,
            contentDescription = app.appName,
            modifier = Modifier.size(resolvedSize),
            colorFilter = if (highlighted) null else ColorFilter.colorMatrix(grayscaleMatrix)
        )

        if (showName && resolvedSize >= 60.dp) {
            Text(
                text = app.appName,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp).size(width = resolvedSize, height = 20.dp)
            )
        }
    }
}