package digital.tonima.bubbleslauncher.ui.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import android.view.ViewGroup
import kotlin.math.roundToInt
import digital.tonima.bubbleslauncher.widget.LongPressFrameLayout
import digital.tonima.bubbleslauncher.widget.WidgetLayout
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.material3.Button
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import digital.tonima.bubbleslauncher.R
import digital.tonima.bubbleslauncher.data.WidgetRepository
import digital.tonima.bubbleslauncher.data.model.WidgetEntry
import digital.tonima.bubbleslauncher.widget.WidgetSizing
import digital.tonima.bubbleslauncher.widget.WidgetHostManager
import kotlinx.coroutines.launch

/**
 * Adds the widgets into a [LazyGridScope] (call inside a `LazyVerticalGrid` content block).
 *
 * Each widget is either full width or half width; consecutive half-width widgets share a row.
 * Widgets sit directly at the top of the grid with rounded corners; adding one is a compact
 * tonal button under them. A long-press menu offers Resize (height handle + width toggle) and Remove.
 */
fun LazyGridScope.widgetSectionItems(
    widgets: List<WidgetEntry>,
    widgetHostManager: WidgetHostManager,
    resizingId: Int?,
    onResizingChange: (Int?) -> Unit,
    onResize: (appWidgetId: Int, heightDp: Int, halfWidth: Boolean) -> Unit,
    onRemoveWidget: (Int) -> Unit,
    onAddWidgetClick: () -> Unit
) {
    val rows = WidgetLayout.rows(widgets)
    items(
        count = rows.size,
        key = { index -> "widget_row_" + rows[index].first().appWidgetId },
        span = { GridItemSpan(maxLineSpan) }
    ) { index ->
        val row = rows[index]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            row.forEach { entry ->
                Box(modifier = Modifier.weight(1f)) {
                    WidgetItem(
                        entry = entry,
                        widgetHostManager = widgetHostManager,
                        resizing = resizingId == entry.appWidgetId,
                        onStartResize = { onResizingChange(entry.appWidgetId) },
                        onCommit = { height, half -> onResize(entry.appWidgetId, height, half) },
                        onDoneResizing = { onResizingChange(null) },
                        onRemove = { onRemoveWidget(entry.appWidgetId) }
                    )
                }
            }
            // A lone half-width widget keeps its half; the other half stays empty.
            if (row.size == 1 && row[0].halfWidth) Spacer(modifier = Modifier.weight(1f))
        }
    }

    item(span = { GridItemSpan(maxLineSpan) }, key = "widget_add_button") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            FilledTonalButton(
                onClick = onAddWidgetClick,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(text = "+  " + stringResource(id = R.string.widget_add))
            }
        }
    }
}

/**
 * Renders one widget in an [AndroidView].
 *
 * Two things matter for widgets to actually load and feel native:
 *  - The host view must be told its size ([AppWidgetHostView.updateAppWidgetSize]); providers
 *    such as clocks render by size and produce an empty layout for the default 0x0.
 *  - Long-press is attached to the host view itself. An overlay composable on top would swallow
 *    every touch and make the widget non-interactive (except deliberately, while resizing).
 *
 * While [resizing], a border marks the widget, touches no longer reach it, a handle on the bottom
 * edge changes the height within the provider's limits, and a button toggles half/full width.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WidgetItem(
    entry: WidgetEntry,
    widgetHostManager: WidgetHostManager,
    resizing: Boolean,
    onStartResize: () -> Unit,
    onCommit: (heightDp: Int, halfWidth: Boolean) -> Unit,
    onDoneResizing: () -> Unit,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    var showMenu by remember { mutableStateOf(false) }

    val providerInfo: AppWidgetProviderInfo? = remember(entry.appWidgetId) {
        try {
            widgetHostManager.getAppWidgetManager().getAppWidgetInfo(entry.appWidgetId)
        } catch (_: Exception) {
            null
        }
    }

    val bounds = remember(providerInfo, density) {
        val maxPx = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) providerInfo?.maxResizeHeight ?: 0 else 0
        WidgetSizing.heightBounds(providerInfo?.minResizeHeight ?: 0, maxPx, density)
    }
    val canResizeVertically = providerInfo?.let { WidgetSizing.canResizeVertically(it.resizeMode) } ?: true

    var liveHeight by remember(entry.heightDp, resizing) { mutableFloatStateOf(entry.heightDp.toFloat()) }
    val heightDp = if (resizing) liveHeight.roundToInt() else entry.heightDp

    val hostView = remember(entry.appWidgetId, providerInfo) {
        providerInfo?.let {
            try {
                widgetHostManager.createView(context, entry.appWidgetId, it)
            } catch (_: Exception) {
                null
            }
        }
    }

    val shape = RoundedCornerShape(28.dp)
    val primary = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(shape)
    ) {
        if (hostView != null) {
            AndroidView(
                factory = { ctx ->
                    // Detach from a previous parent if the grid recycled this item.
                    (hostView.parent as? ViewGroup)?.removeView(hostView)
                    LongPressFrameLayout(ctx).apply {
                        addView(
                            hostView,
                            FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                            )
                        )
                    }
                },
                update = { it.onLongPress = { showMenu = true } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heightDp.dp)
                    .onSizeChanged { size ->
                        val w = WidgetSizing.pxToDp(size.width, density)
                        val h = WidgetSizing.pxToDp(size.height, density)
                        if (w > 0 && h > 0) hostView.updateAppWidgetSize(Bundle(), w, h, w, h)
                    }
            )
        } else {
            // Provider uninstalled/unavailable: show a placeholder that can still be removed.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heightDp.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape)
                    .combinedClickable(onClick = {}, onLongClick = { showMenu = true }),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.widget_no_widgets),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (resizing) {
            // Edit-mode frame. It also swallows taps so the widget isn't triggered by accident;
            // drags still pass through, so the page keeps scrolling.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(2.dp, primary, shape)
                    .pointerInput(Unit) { detectTapGestures { } }
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = { onCommit(liveHeight.roundToInt(), !entry.halfWidth) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(
                            id = if (entry.halfWidth) R.string.widget_width_full else R.string.widget_width_half
                        ),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Button(
                    onClick = {
                        onCommit(liveHeight.roundToInt(), entry.halfWidth)
                        onDoneResizing()
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.widget_resize_done),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            if (canResizeVertically) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp)
                        .size(width = 72.dp, height = 28.dp)
                        .clip(CircleShape)
                        .background(primary)
                        .pointerInput(bounds, density) {
                            detectVerticalDragGestures { change, dragAmount ->
                                change.consume()
                                liveHeight = (liveHeight + dragAmount / density)
                                    .coerceIn(bounds.first.toFloat(), bounds.last.toFloat())
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "═", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }

        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(id = R.string.widget_resize)) },
                onClick = {
                    showMenu = false
                    onStartResize()
                }
            )
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(id = R.string.widget_remove),
                        color = MaterialTheme.colorScheme.error
                    )
                },
                onClick = {
                    showMenu = false
                    onRemove()
                }
            )
        }
    }
}

/**
 * Composable that manages the full widget-picker flow using
 * [rememberLauncherForActivityResult].
 *
 * Flow:
 * 1. allocateWidgetId() → launch system picker (ACTION_APPWIDGET_PICK)
 * 2. On pick result → try bindAppWidgetIdIfAllowed()
 *    - If allowed → proceed to configure or save
 *    - If denied → launch bind-permission dialog
 * 3. After bind → check for configure Activity
 *    - If configure exists → launch it
 *    - Otherwise → save to Room directly
 */
@Composable
fun rememberWidgetPickerLauncher(
    widgetHostManager: WidgetHostManager,
    widgetRepository: WidgetRepository
): () -> Unit {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density
    var pendingWidgetId by remember { mutableIntStateOf(0) }
    var pendingProvider by remember { mutableStateOf<android.content.ComponentName?>(null) }

    // Step 3: Handle optional configure Activity result
    val configureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        val widgetId = pendingWidgetId
        if (result.resultCode == Activity.RESULT_OK && widgetId != 0) {
            scope.launch {
                val info = widgetHostManager.getAppWidgetManager().getAppWidgetInfo(widgetId)
                val height = WidgetSizing.heightDp(info?.minHeight ?: 0, info?.minResizeHeight ?: 0, density)
                widgetRepository.addWidget(widgetId, height)
            }
        } else if (widgetId != 0) {
            widgetHostManager.deleteWidgetId(widgetId)
        }
        pendingWidgetId = 0
        pendingProvider = null
    }

    // Helper: after the widget is bound, either configure or save directly
    fun onWidgetBound(widgetId: Int) {
        val info = widgetHostManager.getAppWidgetManager().getAppWidgetInfo(widgetId)
        if (info?.configure != null) {
            val configIntent = Intent().apply {
                component = info.configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
            configureLauncher.launch(configIntent)
        } else {
            val height = WidgetSizing.heightDp(info?.minHeight ?: 0, info?.minResizeHeight ?: 0, density)
            scope.launch {
                widgetRepository.addWidget(widgetId, height)
            }
            pendingWidgetId = 0
            pendingProvider = null
        }
    }

    // Step 2b: Handle bind-permission dialog result
    val bindPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        val widgetId = pendingWidgetId
        if (result.resultCode == Activity.RESULT_OK && widgetId != 0) {
            onWidgetBound(widgetId)
        } else {
            if (widgetId != 0) {
                widgetHostManager.deleteWidgetId(widgetId)
            }
            pendingWidgetId = 0
            pendingProvider = null
        }
    }

    // Step 2a: After picking, try to bind
    fun tryBindAndConfigure(widgetId: Int, provider: android.content.ComponentName) {
        val bound = widgetHostManager.bindWidget(widgetId, provider)
        if (bound) {
            onWidgetBound(widgetId)
        } else {
            // Need explicit user permission to bind
            val bindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider)
            }
            bindPermissionLauncher.launch(bindIntent)
        }
    }

    // Step 1: Main picker result
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val extras = result.data?.extras
            val widgetId = extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: -1
            if (widgetId != -1) {
                pendingWidgetId = widgetId

                // Check if it's already bound (some OEMs auto-bind from the picker)
                val info = widgetHostManager.getAppWidgetManager().getAppWidgetInfo(widgetId)
                if (info != null) {
                    // Already bound — proceed directly
                    onWidgetBound(widgetId)
                } else {
                    // Not bound yet — extract the provider and bind manually
                    @Suppress("DEPRECATION")
                    val provider = extras?.getParcelable<android.content.ComponentName>(
                        AppWidgetManager.EXTRA_APPWIDGET_PROVIDER
                    )
                    if (provider != null) {
                        pendingProvider = provider
                        tryBindAndConfigure(widgetId, provider)
                    } else {
                        // Cannot determine provider — clean up
                        widgetHostManager.deleteWidgetId(widgetId)
                        pendingWidgetId = 0
                    }
                }
            }
        } else {
            // User cancelled the picker — release the pre-allocated ID
            if (pendingWidgetId != 0) {
                widgetHostManager.deleteWidgetId(pendingWidgetId)
                pendingWidgetId = 0
            }
        }
    }

    return remember(pickerLauncher, widgetHostManager) {
        {
            val widgetId = widgetHostManager.allocateWidgetId()
            pendingWidgetId = widgetId

            val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putParcelableArrayListExtra(
                    AppWidgetManager.EXTRA_CUSTOM_INFO,
                    ArrayList()
                )
                putParcelableArrayListExtra(
                    AppWidgetManager.EXTRA_CUSTOM_EXTRAS,
                    ArrayList<Bundle>()
                )
            }
            pickerLauncher.launch(pickIntent)
        }
    }
}
