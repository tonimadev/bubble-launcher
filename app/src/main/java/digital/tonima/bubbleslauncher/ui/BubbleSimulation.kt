package digital.tonima.bubbleslauncher.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

data class Bubble(
    val id: String,
    val color: Color,
    var x: Float,
    var y: Float,
    val radius: Float,
    var speedX: Float,
    var speedY: Float,
    /** Seconds this bubble has been alive. */
    var age: Float = 0f
) {
    // Gradients are built once, centred on the origin, and drawn translated to (x, y),
    // so the draw loop allocates nothing per frame.
    val bodyBrush: Brush by lazy(LazyThreadSafetyMode.NONE) {
        Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.2f), color.copy(alpha = 0.6f), Color.Transparent),
            center = Offset.Zero,
            radius = radius
        )
    }
    val reflectionOffset: Offset = Offset(-radius * 0.35f, -radius * 0.35f)
    val reflectionBrush: Brush by lazy(LazyThreadSafetyMode.NONE) {
        Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.7f), Color.Transparent),
            center = reflectionOffset,
            radius = radius * 0.4f
        )
    }

    /** 1f while the bubble is alive, fading to 0f during the last [BubbleSimulation.FADE_SECONDS]. */
    fun alpha(): Float {
        val remaining = BubbleSimulation.LIFETIME_SECONDS - age
        return (remaining / BubbleSimulation.FADE_SECONDS).coerceIn(0f, 1f)
    }
}

/**
 * Pure-Kotlin physics for the aquarium background (no Android dependencies, unit-testable).
 *
 * Bubbles expire after [LIFETIME_SECONDS] and the total is capped at [MAX_BUBBLES], so the
 * simulation can never grow without bound and can be fully stopped once [isEmpty] is true.
 */
class BubbleSimulation {
    var width = 1000f
        private set
    var height = 2000f
        private set

    private val _bubbles = ArrayList<Bubble>()

    /** Snapshot-free view for the draw loop. Only mutated from the main thread. */
    val bubbles: List<Bubble> get() = _bubbles

    val isEmpty: Boolean get() = _bubbles.isEmpty()

    fun setBounds(width: Float, height: Float) {
        this.width = width
        this.height = height
    }

    fun add(bubble: Bubble) {
        if (_bubbles.size >= MAX_BUBBLES) _bubbles.removeAt(0)
        _bubbles.add(bubble)
    }

    /** Advances all bubbles by [deltaTime] seconds and drops the expired ones. */
    fun step(deltaTime: Float) {
        // Clamp so a long pause (e.g. returning from background) doesn't teleport bubbles.
        val dt = deltaTime.coerceIn(0f, MAX_STEP_SECONDS)
        var i = 0
        while (i < _bubbles.size) {
            val bubble = _bubbles[i]
            bubble.age += dt
            if (bubble.age >= LIFETIME_SECONDS) {
                _bubbles.removeAt(i)
                continue
            }
            bubble.x += bubble.speedX * dt
            bubble.y += bubble.speedY * dt

            if (bubble.x - bubble.radius < 0) {
                bubble.x = bubble.radius
                bubble.speedX *= -1
            } else if (bubble.x + bubble.radius > width) {
                bubble.x = width - bubble.radius
                bubble.speedX *= -1
            }

            if (bubble.y - bubble.radius < 0) {
                bubble.y = bubble.radius
                bubble.speedY *= -1
            } else if (bubble.y + bubble.radius > height && bubble.speedY > 0) {
                // speedY > 0 check: bubbles spawning below the screen must not bounce immediately
                bubble.y = height - bubble.radius
                bubble.speedY *= -1
            }
            i++
        }
    }

    companion object {
        const val LIFETIME_SECONDS = 30f
        const val FADE_SECONDS = 3f
        const val MAX_BUBBLES = 40
        const val MAX_STEP_SECONDS = 0.1f
    }
}
