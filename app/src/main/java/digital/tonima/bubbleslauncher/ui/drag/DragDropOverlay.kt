package digital.tonima.bubbleslauncher.ui.drag

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import digital.tonima.bubbleslauncher.R

@Composable
fun DragDropOverlay(
    dragDropState: DragDropState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = dragDropState.isDragging,
        enter = fadeIn(animationSpec = tween(200)) + slideInVertically(
            animationSpec = tween(300),
            initialOffsetY = { -it }
        ),
        exit = fadeOut(animationSpec = tween(150)) + slideOutVertically(
            animationSpec = tween(200),
            targetOffsetY = { -it }
        ),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Semi-transparent scrim over the entire screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )

            // Drop zones at the top
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.85f),
                                Color.Black.copy(alpha = 0.6f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(top = 8.dp, bottom = 32.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                @Composable
                fun Zone(zone: DropZone, labelRes: Int, icon: String, color: Color) {
                    DropZoneItem(
                        label = stringResource(id = labelRes),
                        icon = icon,
                        isActive = dragDropState.activeDropZone == zone,
                        activeColor = color,
                        modifier = Modifier
                            .weight(1f)
                            .onGloballyPositioned { dragDropState.zoneBounds[zone] = it.boundsInRoot() }
                    )
                }
                Zone(DropZone.PIN, R.string.drag_pin, "📌", Color(0xFF69F0AE))
                Zone(DropZone.DOCK, R.string.drag_dock, "⚓", Color(0xFFE040FB))
                Zone(DropZone.APP_INFO, R.string.drag_app_info, "ℹ️", Color(0xFF448AFF))
                Zone(DropZone.HIDE, R.string.drag_hide, "👁️", Color(0xFFFFD740))
                Zone(DropZone.UNINSTALL, R.string.drag_uninstall, "🗑️", Color(0xFFFF5252))
            }
        }
    }
}

@Composable
private fun DropZoneItem(
    label: String,
    icon: String,
    isActive: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.15f else 1.0f,
        animationSpec = tween(200),
        label = "dropZoneScale"
    )

    val backgroundColor = if (isActive) {
        activeColor.copy(alpha = 0.3f)
    } else {
        Color.White.copy(alpha = 0.1f)
    }

    val borderColor = if (isActive) {
        activeColor
    } else {
        Color.White.copy(alpha = 0.3f)
    }

    Column(
        modifier = modifier
            .scale(scale)
            .padding(horizontal = 4.dp)
            .background(
                backgroundColor,
                shape = RoundedCornerShape(16.dp)
            )
            .background(
                Brush.linearGradient(
                    listOf(
                        borderColor.copy(alpha = 0.2f),
                        Color.Transparent
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(vertical = 14.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = icon,
            fontSize = 28.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            color = if (isActive) activeColor else Color.White,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
        )
    }
}
