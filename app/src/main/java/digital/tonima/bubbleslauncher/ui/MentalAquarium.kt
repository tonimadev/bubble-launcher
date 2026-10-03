package digital.tonima.bubbleslauncher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.drawscope.translate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import digital.tonima.bubbleslauncher.data.dao.ImpulseDao
import digital.tonima.bubbleslauncher.data.model.ImpulseEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class BubblesViewModel @Inject constructor(
    private val impulseDao: ImpulseDao
) : ViewModel() {
    private val simulation = BubbleSimulation()
    val bubbles: List<Bubble> get() = simulation.bubbles

    // Drives whether the frame loop runs at all: no bubbles => no per-frame work.
    private val _hasBubbles = MutableStateFlow(false)
    val hasBubbles: StateFlow<Boolean> = _hasBubbles.asStateFlow()

    fun onImpulseResisted(appColor: Color, packageName: String = "") {
        // If packageName is provided (real usage, not onboarding demo), persist to DB
        if (packageName.isNotBlank()) {
            viewModelScope.launch {
                impulseDao.insertEvent(
                    ImpulseEvent(
                        timestamp = System.currentTimeMillis(),
                        packageName = packageName,
                        wasResisted = true
                    )
                )
            }
        }

        val radius = Random.nextFloat() * 40f + 40f
        simulation.add(
            Bubble(
                id = UUID.randomUUID().toString(),
                color = appColor,
                x = Random.nextFloat() * simulation.width,
                // Start just below the screen so it floats up into view
                y = simulation.height + radius,
                radius = radius,
                speedX = (Random.nextFloat() - 0.5f) * 40f,
                speedY = -(Random.nextFloat() * 30f + 20f)
            )
        )
        _hasBubbles.value = true
    }

    /** Called when the user did NOT resist the impulse (clicked Continue on MindfulDelay). */
    fun onImpulseGiven(packageName: String) {
        viewModelScope.launch {
            impulseDao.insertEvent(
                ImpulseEvent(
                    timestamp = System.currentTimeMillis(),
                    packageName = packageName,
                    wasResisted = false
                )
            )
        }
    }

    /** Called on every frame while bubbles exist. */
    fun updatePhysics(deltaTime: Float) {
        simulation.step(deltaTime)
        if (simulation.isEmpty) _hasBubbles.value = false
    }

    fun updateScreenDimensions(width: Float, height: Float) {
        simulation.setBounds(width, height)
    }
}

@Composable
fun MentalAquariumBackground(
    viewModel: BubblesViewModel,
    modifier: Modifier = Modifier
) {
    // `tick` is read only inside the Canvas draw block, so a frame invalidates draw only
    // (no recomposition or layout of the app grid).
    var tick by remember { mutableFloatStateOf(0f) }
    val hasBubbles by viewModel.hasBubbles.collectAsState()
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    // The loop exists only while there are bubbles AND the screen is visible (STARTED).
    LaunchedEffect(hasBubbles, lifecycle) {
        if (!hasBubbles) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var lastTime = withFrameNanos { it }
            while (isActive) {
                withFrameNanos { frameTime ->
                    val deltaTime = (frameTime - lastTime) / 1_000_000_000f
                    lastTime = frameTime
                    viewModel.updatePhysics(deltaTime)
                    tick = frameTime.toFloat()
                }
            }
        }
    }

    // Skip the Canvas entirely when idle.
    if (!hasBubbles) return

    Canvas(modifier = modifier.fillMaxSize()) {
        @Suppress("UNUSED_VARIABLE") val currentTick = tick
        viewModel.updateScreenDimensions(size.width, size.height)

        val currentBubbles = viewModel.bubbles
        for (i in currentBubbles.indices) {
            val bubble = currentBubbles[i]
            val alpha = bubble.alpha()
            translate(left = bubble.x, top = bubble.y) {
                drawCircle(brush = bubble.bodyBrush, radius = bubble.radius, center = Offset.Zero, alpha = alpha)
                drawCircle(
                    brush = bubble.reflectionBrush,
                    radius = bubble.radius * 0.4f,
                    center = bubble.reflectionOffset,
                    alpha = alpha
                )
            }
        }
    }
}
