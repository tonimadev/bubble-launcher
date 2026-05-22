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
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import digital.tonima.bubbleslauncher.R
import digital.tonima.bubbleslauncher.data.WidgetRepository
import digital.tonima.bubbleslauncher.data.model.WidgetEntry
import digital.tonima.bubbleslauncher.ui.AppSectionHeader
import digital.tonima.bubbleslauncher.widget.WidgetHostManager
import kotlinx.coroutines.launch

/**
 * Adds the widget section items into a [LazyGridScope].
 * Must be called inside a `LazyVerticalGrid`'s `content` block.
 *
 * Shows:
 *  - Section header "Widgets"
 *  - Each persisted widget rendered as a full-span [AndroidView]
 *  - An "Add widget" button
 */
@OptIn(ExperimentalFoundationApi::class)
fun LazyGridScope.widgetSectionItems(
    widgets: List<WidgetEntry>,
    widgetHostManager: WidgetHostManager,
    onRemoveWidget: (Int) -> Unit,
    onAddWidgetClick: () -> Unit
) {
    // Section header
    item(span = { GridItemSpan(maxLineSpan) }, key = "widget_section_header") {
        AppSectionHeader(stringResource(id = R.string.section_widgets_title))
    }

    // Each widget as full-span item
    items(
        count = widgets.size,
        key = { index -> "widget_${widgets[index].appWidgetId}" },
        span = { GridItemSpan(maxLineSpan) }
    ) { index ->
        val entry = widgets[index]
        WidgetItem(
            entry = entry,
            widgetHostManager = widgetHostManager,
            onRemove = { onRemoveWidget(entry.appWidgetId) }
        )
    }

    item(span = { GridItemSpan(maxLineSpan) }, key = "widget_add_button") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            OutlinedButton(onClick = onAddWidgetClick) {
                Text(text = "+ " + stringResource(id = R.string.widget_add))
            }
        }
    }
}

/**
 * Renders a single widget inside an [AndroidView].
 * Long-press shows a dropdown to remove the widget.
 *
 * The [AppWidgetHostView] is cached via [remember] so that
 * LazyGrid recycling doesn't create duplicate views for the same
 * widget ID (which causes the new view to be blank).
 *
 * Because [AppWidgetHostView] is a native Android view that consumes
 * all touch events, we place an invisible overlay on top that only
 * intercepts long-press gestures — normal taps/scrolls pass through
 * to the widget underneath.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WidgetItem(
    entry: WidgetEntry,
    widgetHostManager: WidgetHostManager,
    onRemove: () -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    val providerInfo: AppWidgetProviderInfo? = remember(entry.appWidgetId) {
        try {
            widgetHostManager.getAppWidgetManager().getAppWidgetInfo(entry.appWidgetId)
        } catch (_: Exception) {
            null
        }
    }

    // Cache the host view so it survives recomposition/recycling
    val hostView = remember(entry.appWidgetId, providerInfo) {
        if (providerInfo != null) {
            try {
                widgetHostManager.createView(context, entry.appWidgetId, providerInfo)
            } catch (_: Exception) {
                null
            }
        } else null
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        if (hostView != null) {
            AndroidView(
                factory = {
                    // Detach from previous parent if recycled by LazyGrid
                    (hostView.parent as? android.view.ViewGroup)?.removeView(hostView)
                    hostView.apply {
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(entry.heightDp.dp)
            )

            // Invisible overlay that detects long-press on top of the native widget view.
            // We use pointerInput with detectTapGestures(onLongPress) which only fires
            // after the long-press threshold — short taps are ignored and fall through
            // to the AndroidView below.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { showMenu = true }
                        )
                    }
            )
        } else {
            // Widget provider was uninstalled or unavailable
            Text(
                text = stringResource(id = R.string.widget_no_widgets),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(entry.heightDp.dp)
                    .padding(16.dp)
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🗑️",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.widget_remove),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
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
                val height = info?.minHeight?.let { it / 2 }?.coerceIn(100, 400) ?: 200
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
            val height = info?.minHeight?.let { it / 2 }?.coerceIn(100, 400) ?: 200
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
