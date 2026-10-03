package digital.tonima.bubbleslauncher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import digital.tonima.bubbleslauncher.R
import android.content.pm.ShortcutInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import digital.tonima.bubbleslauncher.data.WidgetRepository
import digital.tonima.bubbleslauncher.model.AppInfo
import digital.tonima.bubbleslauncher.ui.drag.DragDropOverlay
import digital.tonima.bubbleslauncher.ui.drag.DragDropState
import digital.tonima.bubbleslauncher.ui.drag.DragLogic
import digital.tonima.bubbleslauncher.ui.drag.DropZone
import digital.tonima.bubbleslauncher.ui.drag.LongPressDragCallbacks
import digital.tonima.bubbleslauncher.ui.drag.longPressDragGestures
import digital.tonima.bubbleslauncher.ui.drag.rememberDragDropState
import kotlin.math.roundToInt
import digital.tonima.bubbleslauncher.ui.search.AppSearchBar
import digital.tonima.bubbleslauncher.ui.widget.rememberWidgetPickerLauncher
import digital.tonima.bubbleslauncher.ui.widget.widgetSectionItems
import digital.tonima.bubbleslauncher.widget.WidgetHostManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/** Visual preferences shared by every bubble; one stable object instead of 6 loose params. */
@Immutable
data class BubbleStyle(
    val showName: Boolean,
    val showUsageBadges: Boolean,
    val ignoreDynamicSize: Boolean,
    val iconSizeDp: Int,
    val showNotificationBadges: Boolean
)

private val ImpulseColors = listOf(
    Color(0xFFFF5252), Color(0xFF448AFF), Color(0xFF69F0AE), Color(0xFFFFD740),
    Color(0xFFE040FB), Color(0xFF18FFFF), Color(0xFFFF4081)
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen(
    state: MainViewModel.MainUiState,
    onIntent: (MainViewModel.MainIntent) -> Unit,
    modifier: Modifier = Modifier,
    bubblesViewModel: BubblesViewModel = hiltViewModel(),
    widgetHostManager: WidgetHostManager? = null,
    widgetRepository: WidgetRepository? = null
) {
    val context = LocalContext.current
    var delayAppToLaunch by remember { mutableStateOf<AppInfo?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        MentalAquariumBackground(viewModel = bubblesViewModel)

        // Sets give O(1) membership; recomputed only when the underlying lists change.
        val hiddenSet = remember(state.hiddenApps) { state.hiddenApps.toHashSet() }
        val essentialSet = remember(state.essentialApps) { state.essentialApps.toHashSet() }
        val pinnedSet = remember(state.pinnedApps) { state.pinnedApps.toHashSet() }
        val delaySet = remember(state.delayApps) { state.delayApps.toHashSet() }
        val dockSet = remember(state.dockApps) { state.dockApps.toHashSet() }

        val sections = remember(
            state.apps, state.pinnedApps, state.dockApps, state.folders, hiddenSet, essentialSet, state.isFocusModeEnabled, state.searchQuery
        ) {
            AppSections.compute(
                apps = state.apps,
                pinnedOrder = state.pinnedApps,
                hidden = hiddenSet,
                essential = essentialSet,
                focusMode = state.isFocusModeEnabled,
                query = state.searchQuery,
                dockOrder = state.dockApps,
                folders = state.folders
            )
        }

        val style = remember(
            state.showAppNames, state.showUsageBadges, state.ignoreDynamicSize,
            state.iconSizeDp, state.showNotificationBadges
        ) {
            BubbleStyle(
                showName = state.showAppNames,
                showUsageBadges = state.showUsageBadges,
                ignoreDynamicSize = state.ignoreDynamicSize,
                iconSizeDp = state.iconSizeDp,
                showNotificationBadges = state.showNotificationBadges
            )
        }

        // Dock icons are fixed-size and nameless, so the dock stays compact and uniform.
        val dockStyle = remember(state.showNotificationBadges) {
            BubbleStyle(
                showName = false,
                showUsageBadges = false,
                ignoreDynamicSize = true,
                iconSizeDp = 52,
                showNotificationBadges = state.showNotificationBadges
            )
        }

        val scope = rememberCoroutineScope()
        val widgetPickerLauncher = if (widgetHostManager != null && widgetRepository != null) {
            rememberWidgetPickerLauncher(widgetHostManager, widgetRepository)
        } else null

        val widgets by widgetRepository?.widgets?.collectAsState(initial = emptyList())
            ?: remember { mutableStateOf(emptyList()) }

        // ---- Drag & drop -------------------------------------------------------------------
        val dragState = rememberDragDropState()
        val gridState = rememberLazyGridState()
        val haptic = LocalHapticFeedback.current
        val focusManager = LocalFocusManager.current
        val density = LocalDensity.current
        var menuPkg by remember { mutableStateOf<String?>(null) }
        var openFolderId by remember { mutableStateOf<String?>(null) }
        var folderPickerFor by remember { mutableStateOf<AppInfo?>(null) }
        var gridBottomRoot by remember { mutableFloatStateOf(0f) }

        // The gesture callbacks live as long as the grid, so they read the latest state through these.
        val currentApps by rememberUpdatedState(state.apps)
        val currentPinned by rememberUpdatedState(pinnedSet)
        val currentDock by rememberUpdatedState(dockSet)
        val currentOnIntent by rememberUpdatedState(onIntent)

        val dragCallbacks = remember(dragState) {
            var lastTarget: String? = null
            LongPressDragCallbacks(
                hitTest = { local -> DragLogic.itemAt(local + dragState.containerOrigin, dragState.itemBounds) },
                onLongPress = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                onLongPressRelease = { pkg -> menuPkg = pkg },
                onDragStart = { pkg, local ->
                    currentApps.firstOrNull { it.packageName == pkg }?.let { app ->
                        lastTarget = null
                        dragState.startDrag(app, local + dragState.containerOrigin)
                    }
                },
                onDrag = { local ->
                    val root = local + dragState.containerOrigin
                    dragState.updateDrag(root)
                    val dragged = dragState.draggedApp?.packageName
                    // Reorder live as the finger crosses another item of the same group.
                    val group = when {
                        dragged == null -> null
                        dragged in currentDock -> currentDock
                        dragged in currentPinned -> currentPinned
                        else -> null
                    }
                    if (dragged != null && group != null) {
                        val target = DragLogic.itemAt(root, dragState.itemBounds.filterKeys { it in group }, exclude = dragged)
                        if (target != null && target != lastTarget) {
                            currentOnIntent(
                                if (group === currentDock) MainViewModel.MainIntent.MoveDock(dragged, target)
                                else MainViewModel.MainIntent.MovePinned(dragged, target)
                            )
                        }
                        lastTarget = target
                    }
                },
                onDragEnd = {
                    val (app, zone) = dragState.endDrag()
                    if (app != null && zone != null) {
                        when (zone) {
                            DropZone.UNINSTALL -> AppActions.uninstall(context, app)
                            DropZone.APP_INFO -> AppActions.openAppInfo(context, app)
                            DropZone.HIDE -> currentOnIntent(MainViewModel.MainIntent.ToggleHiddenApp(app.packageName))
                            DropZone.PIN -> currentOnIntent(MainViewModel.MainIntent.TogglePin(app.packageName))
                            DropZone.DOCK -> currentOnIntent(MainViewModel.MainIntent.ToggleDock(app.packageName))
                        }
                    }
                },
                onDragCancel = { dragState.cancelDrag() }
            )
        }

        // Home pressed while already here: back to a clean top-of-grid state.
        LaunchedEffect(state.homePressCount) {
            if (state.homePressCount > 0) {
                menuPkg = null
                dragState.cancelDrag()
                focusManager.clearFocus()
                gridState.animateScrollToItem(0)
            }
        }

        // Back never leaves a launcher; it only dismisses transient UI.
        BackHandler {
            when {
                menuPkg != null -> menuPkg = null
                state.searchQuery.isNotEmpty() -> onIntent(MainViewModel.MainIntent.SetSearchQuery(""))
                else -> Unit
            }
        }

        // Scroll the grid while the finger is held near its bottom edge.
        LaunchedEffect(dragState.isDragging) {
            if (!dragState.isDragging) return@LaunchedEffect
            val edge = with(density) { 96.dp.toPx() }
            val maxSpeed = with(density) { 14.dp.toPx() }
            while (true) {
                withFrameNanos { }
                val speed = DragLogic.autoScrollSpeed(dragState.dragPosition.y, gridBottomRoot, edge, maxSpeed)
                if (speed > 0f) gridState.scrollBy(speed)
            }
        }

        // Builds one grid cell for [app]; shared by all three sections.
        @Composable
        fun Cell(app: AppInfo, modifier: Modifier = Modifier, cellStyle: BubbleStyle = style, fill: Boolean = true) {
            // Grid cells fill their slot; dock cells must wrap their content or the dock grows to full height.
            Box(modifier = if (fill) modifier.fillMaxSize() else modifier, contentAlignment = Alignment.Center) {
                AppGridItem(
                    app = app,
                    maxTime = sections.maxTime,
                    style = cellStyle,
                    highlighted = app.packageName in state.highlightedApps,
                    isPinned = app.packageName in pinnedSet && cellStyle === style,
                    isDocked = app.packageName in dockSet,
                    isInFolder = state.folders.any { app.packageName in it.packages },
                    onAddToFolder = { folderPickerFor = app },
                    isDelay = app.packageName in delaySet,
                    isEssential = app.packageName in essentialSet,
                    notificationCount = state.notificationCounts[app.packageName] ?: 0,
                    dimmed = dragState.draggedApp?.packageName == app.packageName,
                    menuOpen = menuPkg == app.packageName,
                    dragState = dragState,
                    onDismissMenu = { menuPkg = null },
                    onOpenMenu = { menuPkg = app.packageName },
                    onIntent = onIntent,
                    onRequestDelay = { delayAppToLaunch = it }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { dragState.containerOrigin = it.positionInRoot() }
                .longPressDragGestures(dragCallbacks)
        ) {
            AppSearchBar(
                query = state.searchQuery,
                onQueryChange = { onIntent(MainViewModel.MainIntent.SetSearchQuery(it)) },
                modifier = Modifier.padding(bottom = 8.dp),
                onSearch = {
                    sections.firstMatch()?.let { app ->
                        focusManager.clearFocus()
                        if (app.packageName in delaySet) delayAppToLaunch = app else AppActions.launch(context, app)
                    }
                }
            )

            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(96.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onGloballyPositioned { gridBottomRoot = it.positionInRoot().y + it.size.height },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
            ) {
                // Stable first item: a lazy grid keeps the first *visible* item anchored when items
                // are inserted above it. Widgets load asynchronously from Room, so without this
                // anchor they'd appear above the viewport and look like they never loaded.
                item(key = "grid_top_anchor", span = { GridItemSpan(maxLineSpan) }) {
                    Spacer(modifier = Modifier.height(1.dp))
                }

                if (widgetHostManager != null && widgetRepository != null) {
                    widgetSectionItems(
                        widgets = widgets,
                        widgetHostManager = widgetHostManager,
                        onRemoveWidget = { appWidgetId ->
                            scope.launch { widgetRepository.removeWidget(appWidgetId) }
                        },
                        onAddWidgetClick = { widgetPickerLauncher?.invoke() }
                    )
                }

                if (sections.pinned.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "header_pinned") {
                        AppSectionHeader(stringResource(id = R.string.section_favorites_title))
                    }
                    items(sections.pinned, key = { "${it.packageName}_${it.userHandle}_pinned" }) { Cell(it, Modifier.animateItem()) }
                }

                if (sections.folders.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "header_folders") {
                        AppSectionHeader(stringResource(id = R.string.section_folders_title))
                    }
                    items(sections.folders, key = { "folder_" + it.folder.id }) { section ->
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            FolderTile(section, onClick = { openFolderId = section.folder.id })
                        }
                    }
                }

                if (sections.inUse.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "header_inuse") {
                        AppSectionHeader(stringResource(id = R.string.section_used_today_title))
                    }
                    items(sections.inUse, key = { "${it.packageName}_${it.userHandle}_inuse" }) { Cell(it) }
                }

                if (sections.other.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "header_other") {
                        AppSectionHeader(stringResource(id = R.string.section_all_apps_title))
                    }
                    items(sections.other, key = { "${it.packageName}_${it.userHandle}_other" }) { Cell(it) }
                }
            }

            Dock(sections.dock) { Cell(it, cellStyle = dockStyle, fill = false) }
        }

        sections.folders.firstOrNull { it.folder.id == openFolderId }?.let { section ->
            FolderDialog(
                section = section,
                onDismiss = { openFolderId = null },
                onLaunch = { app ->
                    openFolderId = null
                    if (app.packageName in delaySet) delayAppToLaunch = app else AppActions.launch(context, app)
                },
                onRename = { onIntent(MainViewModel.MainIntent.RenameFolder(section.folder.id, it)) },
                onDelete = {
                    openFolderId = null
                    onIntent(MainViewModel.MainIntent.DeleteFolder(section.folder.id))
                },
                onRemoveApp = { onIntent(MainViewModel.MainIntent.RemoveFromFolder(it.packageName)) }
            )
        }

        folderPickerFor?.let { app ->
            FolderPickerDialog(
                folders = state.folders,
                onPick = {
                    onIntent(MainViewModel.MainIntent.AddToFolder(it.id, app.packageName))
                    folderPickerFor = null
                },
                onCreate = {
                    onIntent(MainViewModel.MainIntent.CreateFolder(it, app.packageName))
                    folderPickerFor = null
                },
                onDismiss = { folderPickerFor = null }
            )
        }

        DragDropOverlay(dragDropState = dragState, modifier = Modifier.fillMaxSize())
        DragGhost(dragState)

        delayAppToLaunch?.let { app ->
            MindfulDelayOverlay(
                app = app,
                onCancel = {
                    bubblesViewModel.onImpulseResisted(ImpulseColors.random(), app.packageName)
                    delayAppToLaunch = null
                },
                onContinue = {
                    delayAppToLaunch = null
                    bubblesViewModel.onImpulseGiven(app.packageName)
                    AppActions.launch(context, app)
                }
            )
        }
    }
}

@Composable
fun AppSectionHeader(title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** A bubble plus its own context menu. Takes only primitives so it can skip recomposition. */
@Composable
fun AppGridItem(
    app: AppInfo,
    maxTime: Long,
    style: BubbleStyle,
    highlighted: Boolean,
    isPinned: Boolean,
    isDelay: Boolean,
    isEssential: Boolean,
    isDocked: Boolean,
    isInFolder: Boolean,
    onAddToFolder: () -> Unit,
    notificationCount: Int,
    dimmed: Boolean,
    menuOpen: Boolean,
    dragState: DragDropState,
    onDismissMenu: () -> Unit,
    onOpenMenu: () -> Unit,
    onIntent: (MainViewModel.MainIntent) -> Unit,
    onRequestDelay: (AppInfo) -> Unit
) {
    val context = LocalContext.current
    val key = app.packageName

    // Published so the container-level drag gesture can find which item is under the finger.
    DisposableEffect(key) { onDispose { dragState.itemBounds.remove(key) } }

    AppBubble(
        modifier = Modifier.onGloballyPositioned { c ->
            dragState.itemBounds[key] = Rect(c.positionInRoot(), Size(c.size.width.toFloat(), c.size.height.toFloat()))
        },
        dimmed = dimmed,
        onOpenMenu = onOpenMenu,
        app = app,
        maxTime = maxTime,
        style = style,
        highlighted = highlighted,
        isPinned = isPinned,
        notificationCount = notificationCount,
        onClick = {
            if (isDelay) onRequestDelay(app) else AppActions.launch(context, app)
        }
    )

    if (menuOpen) {
        AppContextMenu(
            app = app,
            highlighted = highlighted,
            isPinned = isPinned,
            isDelay = isDelay,
            isEssential = isEssential,
            isDocked = isDocked,
            isInFolder = isInFolder,
            onAddToFolder = onAddToFolder,
            onIntent = onIntent,
            onDismiss = onDismissMenu
        )
    }
}

@Composable
fun AppContextMenu(
    app: AppInfo,
    highlighted: Boolean,
    isPinned: Boolean,
    isDelay: Boolean,
    isEssential: Boolean,
    isDocked: Boolean,
    isInFolder: Boolean,
    onAddToFolder: () -> Unit,
    onIntent: (MainViewModel.MainIntent) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val pkg = app.packageName
    val shortcuts by produceState(emptyList<AppShortcut>(), pkg) {
        value = withContext(Dispatchers.IO) { AppActions.shortcuts(context, app) }
    }

    @Composable
    fun Item(textRes: Int, onClick: () -> Unit) {
        DropdownMenuItem(
            text = { Text(stringResource(id = textRes)) },
            onClick = {
                onDismiss()
                onClick()
            }
        )
    }

    DropdownMenu(expanded = true, onDismissRequest = onDismiss) {
        if (shortcuts.isNotEmpty()) {
            shortcuts.forEach { sc ->
                DropdownMenuItem(
                    text = { Text(sc.label) },
                    onClick = {
                        onDismiss()
                        AppActions.startShortcut(context, sc)
                    }
                )
            }
            HorizontalDivider()
        }
        Item(if (highlighted) R.string.menu_unhighlight else R.string.menu_highlight) {
            onIntent(MainViewModel.MainIntent.ToggleHighlight(pkg))
        }
        Item(if (isPinned) R.string.menu_unpin else R.string.menu_pin) {
            onIntent(MainViewModel.MainIntent.TogglePin(pkg))
        }
        Item(if (isDocked) R.string.menu_remove_dock else R.string.menu_add_dock) {
            onIntent(MainViewModel.MainIntent.ToggleDock(pkg))
        }
        if (isInFolder) {
            Item(R.string.menu_remove_from_folder) { onIntent(MainViewModel.MainIntent.RemoveFromFolder(pkg)) }
        } else {
            Item(R.string.menu_add_to_folder) { onAddToFolder() }
        }
        Item(if (isDelay) R.string.menu_remove_delay else R.string.menu_add_delay) {
            onIntent(MainViewModel.MainIntent.ToggleDelayApp(pkg))
        }
        Item(if (isEssential) R.string.menu_remove_essential else R.string.menu_add_essential) {
            onIntent(MainViewModel.MainIntent.ToggleEssentialApp(pkg))
        }
        Item(R.string.menu_hide) { onIntent(MainViewModel.MainIntent.ToggleHiddenApp(pkg)) }
        Item(R.string.menu_app_info) { AppActions.openAppInfo(context, app) }
        Item(R.string.menu_uninstall) { AppActions.uninstall(context, app) }
    }
}

@Composable
fun AppBubble(
    app: AppInfo,
    maxTime: Long,
    style: BubbleStyle,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenMenu: () -> Unit = {},
    dimmed: Boolean = false,
    isPinned: Boolean = false,
    notificationCount: Int = 0
) {
    val resolvedSize = if (style.ignoreDynamicSize) {
        style.iconSizeDp.dp
    } else {
        bubbleSizeDp(app.totalTimeInForeground, maxTime).dp
    }

    val grayscaleMatrix = remember { ColorMatrix().apply { setToSaturation(0f) } }

    val context = LocalContext.current
    val moreOptionsLabel = stringResource(id = R.string.a11y_more_options)
    val imageBitmap by produceState<ImageBitmap?>(
        initialValue = AppIconCache.peek(app),
        app.packageName,
        app.userHandle
    ) {
        if (value == null) {
            value = withContext(Dispatchers.IO) { AppIconCache.load(context, app) }
        }
    }

    Column(
        // Long-press is handled by the container's drag gesture (menu or drag), not here.
        modifier = modifier
            .alpha(if (dimmed) 0.25f else 1f)
            .padding(8.dp)
            .clickable(onClick = onClick)
            // Touch long-press is the container's drag gesture; screen readers get the menu here.
            .semantics {
                onLongClick(label = moreOptionsLabel) {
                    onOpenMenu()
                    true
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(resolvedSize)
                .then(
                    if (isPinned) {
                        Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(4.dp)
                    } else Modifier
                )
        ) {
            imageBitmap?.let { bmp ->
                Image(
                    bitmap = bmp,
                    contentDescription = app.appName,
                    modifier = Modifier.fillMaxSize(),
                    colorFilter = if (highlighted) null else ColorFilter.colorMatrix(grayscaleMatrix)
                )
            }
            if (style.showUsageBadges && app.totalTimeInForeground > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.7f), shape = RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = formatUsage(app.totalTimeInForeground),
                        fontSize = 10.sp,
                        color = Color.White
                    )
                }
            }
            if (style.showNotificationBadges && notificationCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(10.dp)
                        .background(MaterialTheme.colorScheme.error, shape = CircleShape)
                )
            }
        }

        if (style.showName && resolvedSize >= 60.dp) {
            Text(
                text = app.appName,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground,
                style = TextStyle(
                    shadow = Shadow(
                        color = MaterialTheme.colorScheme.background,
                        offset = Offset(0f, 2f),
                        blurRadius = 8f
                    )
                ),
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(width = resolvedSize + 8.dp, height = 30.dp)
            )
        }
    }
}

/** Bubble size in dp, scaling from 48 to 120 with usage relative to the most used app. */
fun bubbleSizeDp(timeInForeground: Long, maxTime: Long): Float =
    if (maxTime > 0) 48f + (timeInForeground.toFloat() / maxTime) * (120f - 48f) else 48f

fun formatUsage(timeInForegroundMs: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInForegroundMs)
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        minutes == 0L -> "<1m"
        hours > 0 -> "${hours}h${mins}m"
        else -> "${mins}m"
    }
}

@Composable
fun MindfulDelayOverlay(
    app: AppInfo,
    onCancel: () -> Unit,
    onContinue: () -> Unit
) {
    var timeLeft by remember { mutableIntStateOf(5) }

    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000L)
            timeLeft--
        }
    }

    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(id = R.string.dialog_delay_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = stringResource(id = R.string.dialog_delay_message),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TextButton(onClick = onCancel) {
                        Text(stringResource(id = R.string.dialog_delay_cancel))
                    }
                    Button(onClick = onContinue, enabled = timeLeft == 0) {
                        Text(
                            text = if (timeLeft > 0) {
                                stringResource(id = R.string.dialog_delay_continue, timeLeft)
                            } else {
                                stringResource(id = R.string.dialog_delay_continue_ready)
                            }
                        )
                    }
                }
            }
        }
    }
}

/** The icon that follows the finger while dragging. Position is read in the layout phase only. */
@Composable
private fun DragGhost(dragState: DragDropState, sizeDp: Int = 72) {
    val app = dragState.draggedApp ?: return
    val bitmap = AppIconCache.peek(app) ?: return
    val sizePx = with(LocalDensity.current) { sizeDp.dp.toPx() }
    Image(
        bitmap = bitmap,
        contentDescription = null,
        modifier = Modifier
            .offset {
                IntOffset(
                    (dragState.dragPosition.x - dragState.containerOrigin.x - sizePx / 2).roundToInt(),
                    (dragState.dragPosition.y - dragState.containerOrigin.y - sizePx / 2).roundToInt()
                )
            }
            .size(sizeDp.dp)
            .alpha(0.92f)
    )
}

/** Always-visible row of up to [AppSections.MAX_DOCK] apps pinned to the bottom of the screen. */
@Composable
private fun Dock(apps: List<AppInfo>, item: @Composable (AppInfo) -> Unit) {
    if (apps.isEmpty()) return
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
        tonalElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            apps.forEach { app ->
                key(app.packageName) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) { item(app) }
                }
            }
        }
    }
}
