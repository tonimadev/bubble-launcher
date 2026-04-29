package digital.tonima.bubbleslauncher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import java.util.UUID
import kotlin.random.Random

// 1. Data Class
data class Bubble(
    val id: String,
    val color: Color,
    var x: Float,
    var y: Float,
    val radius: Float,
    var speedX: Float,
    var speedY: Float
)

// 2. ViewModel for State Management
class BubblesViewModel : ViewModel() {
    private val _bubbles = MutableStateFlow<List<Bubble>>(emptyList())
    val bubbles: StateFlow<List<Bubble>> = _bubbles.asStateFlow()

    // Keep track of screen bounds for physics calculations
    private var screenWidth = 1000f
    private var screenHeight = 2000f

    fun onImpulseResisted(appColor: Color) {
        val radius = Random.nextFloat() * 40f + 40f // Random radius between 40 and 80

        // Start near the bottom, horizontally distributed
        val x = Random.nextFloat() * screenWidth
        // Start just below the screen so it elegantly floats upwards into view
        val y = screenHeight + radius 

        // Slow, calm, floating velocities (like a lava lamp or aquarium)
        // Horizontal drift (left or right)
        val speedX = (Random.nextFloat() - 0.5f) * 40f 
        // Vertical drift (always upwards initially)
        val speedY = -(Random.nextFloat() * 30f + 20f) 

        val newBubble = Bubble(
            id = UUID.randomUUID().toString(),
            color = appColor,
            x = x,
            y = y,
            radius = radius,
            speedX = speedX,
            speedY = speedY
        )

        // Add the new bubble to the flow
        _bubbles.update { it + newBubble }
    }

    // Called on every frame to step the physics engine forward
    fun updatePhysics(deltaTime: Float) {
        // We mutate the Bubble objects directly to avoid garbage collection overhead
        // which is crucial for a launcher background running constantly at 60/120fps.
        _bubbles.value.forEach { bubble ->
            // Linear Kinematics: new_position = current_position + (velocity * time)
            bubble.x += bubble.speedX * deltaTime
            bubble.y += bubble.speedY * deltaTime

            // Boundary collision logic: if a bubble hits an edge, we gently bounce it
            // back by reversing the velocity vector in that dimension (speed *= -1).
            
            // Left/Right boundaries
            if (bubble.x - bubble.radius < 0) {
                bubble.x = bubble.radius
                bubble.speedX *= -1
            } else if (bubble.x + bubble.radius > screenWidth) {
                bubble.x = screenWidth - bubble.radius
                bubble.speedX *= -1
            }

            // Top boundary
            if (bubble.y - bubble.radius < 0) {
                bubble.y = bubble.radius
                bubble.speedY *= -1
            } 
            // Bottom boundary (we check speedY > 0 so bubbles spawning below the 
            // screen aren't immediately bounced down before they enter the screen)
            else if (bubble.y + bubble.radius > screenHeight && bubble.speedY > 0) {
                bubble.y = screenHeight - bubble.radius
                bubble.speedY *= -1
            }
        }
    }

    fun updateScreenDimensions(width: Float, height: Float) {
        screenWidth = width
        screenHeight = height
    }
}

// 3. UI Component (Compose Canvas)
@Composable
fun MentalAquariumBackground(
    viewModel: BubblesViewModel,
    modifier: Modifier = Modifier
) {
    // PERFORMANCE OPTIMIZATION:
    // We use a `tick` state to force the Canvas to redraw.
    // By reading this state ONLY inside the Canvas (which operates in Compose's DrawScope), 
    // Compose optimizes the process by skipping the Composition and Layout phases entirely,
    // running only the Draw phase. This prevents any unnecessary recompositions of your app icons.
    var tick by remember { mutableFloatStateOf(0f) }

    // 4. Animation Engine
    LaunchedEffect(Unit) {
        var lastTime = withFrameNanos { it }
        while (isActive) {
            withFrameNanos { frameTime ->
                // Calculate delta time in seconds
                val deltaTime = (frameTime - lastTime) / 1_000_000_000f
                lastTime = frameTime
                
                // Step the physics simulation
                viewModel.updatePhysics(deltaTime)
                
                // Mutate the state to invalidate the DrawScope and trigger a redraw
                tick = frameTime.toFloat()
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        // Read the tick to observe the state and force this block to re-execute every frame
        val currentTick = tick
        
        // Update physics boundaries based on actual canvas dimensions
        viewModel.updateScreenDimensions(size.width, size.height)

        // Read the list directly from the flow's value. 
        // We do NOT use collectAsState() because that would trigger full recompositions!
        val currentBubbles = viewModel.bubbles.value

        currentBubbles.forEach { bubble ->
            val center = Offset(bubble.x, bubble.y)
            
            // Create a beautiful glass/soap bubble look using a radial gradient
            val bubbleBrush = Brush.radialGradient(
                colors = listOf(
                    bubble.color.copy(alpha = 0.2f), // Inner color (mostly transparent)
                    bubble.color.copy(alpha = 0.6f), // Outer edges (more opaque for 3D outline)
                    Color.Transparent
                ),
                center = center,
                radius = bubble.radius
            )
            
            // Draw the main bubble body
            drawCircle(
                brush = bubbleBrush,
                radius = bubble.radius,
                center = center
            )
            
            // Draw a small shiny reflection near the top-left of the bubble for a 3D effect
            val reflectionOffset = Offset(
                bubble.x - bubble.radius * 0.35f, 
                bubble.y - bubble.radius * 0.35f
            )
            val reflectionBrush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.7f),
                    Color.Transparent
                ),
                center = reflectionOffset,
                radius = bubble.radius * 0.4f
            )
            
            drawCircle(
                brush = reflectionBrush,
                radius = bubble.radius * 0.4f,
                center = reflectionOffset
            )
        }
    }
}
