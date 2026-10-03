package digital.tonima.bubbleslauncher.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BubbleSimulationTest {

    private fun bubble(
        id: String = "b",
        x: Float = 500f,
        y: Float = 1000f,
        radius: Float = 50f,
        speedX: Float = 0f,
        speedY: Float = 0f
    ) = Bubble(id, Color.Red, x, y, radius, speedX, speedY)

    private fun sim() = BubbleSimulation().apply { setBounds(1000f, 2000f) }

    @Test
    fun newSimulation_isEmpty() {
        assertTrue(sim().isEmpty)
    }

    @Test
    fun step_movesBubbleByVelocityTimesDelta() {
        val s = sim()
        s.add(bubble(speedX = 10f, speedY = -20f))
        s.step(0.1f)
        val b = s.bubbles.single()
        assertEquals(501f, b.x, 0.001f)
        assertEquals(998f, b.y, 0.001f)
    }

    @Test
    fun step_bouncesOffLeftAndRightWalls() {
        val s = sim()
        s.add(bubble(x = 55f, speedX = -100f))
        s.step(0.1f)
        assertEquals(50f, s.bubbles.single().x, 0.001f)
        assertEquals(100f, s.bubbles.single().speedX, 0.001f)

        val r = sim()
        r.add(bubble(x = 945f, speedX = 100f))
        r.step(0.1f)
        assertEquals(950f, r.bubbles.single().x, 0.001f)
        assertEquals(-100f, r.bubbles.single().speedX, 0.001f)
    }

    @Test
    fun step_bouncesOffTop() {
        val s = sim()
        s.add(bubble(y = 55f, speedY = -100f))
        s.step(0.1f)
        assertEquals(50f, s.bubbles.single().y, 0.001f)
        assertEquals(100f, s.bubbles.single().speedY, 0.001f)
    }

    @Test
    fun bubbleSpawnedBelowScreen_risingIsNotBouncedBack() {
        val s = sim()
        s.add(bubble(y = 2050f, speedY = -30f))
        s.step(0.05f)
        assertEquals(-30f, s.bubbles.single().speedY, 0.001f)
    }

    @Test
    fun bubble_expiresAfterLifetime() {
        val s = sim()
        s.add(bubble())
        repeat((BubbleSimulation.LIFETIME_SECONDS / BubbleSimulation.MAX_STEP_SECONDS).toInt() + 2) {
            s.step(BubbleSimulation.MAX_STEP_SECONDS)
        }
        assertTrue(s.isEmpty)
    }

    @Test
    fun bubble_aliveBeforeLifetime() {
        val s = sim()
        s.add(bubble())
        s.step(BubbleSimulation.MAX_STEP_SECONDS)
        assertFalse(s.isEmpty)
    }

    @Test
    fun step_clampsHugeDeltaSoNothingTeleports() {
        val s = sim()
        s.add(bubble(speedX = 100f))
        s.step(60f) // e.g. returning from background
        assertEquals(500f + 100f * BubbleSimulation.MAX_STEP_SECONDS, s.bubbles.single().x, 0.001f)
        assertFalse(s.isEmpty)
    }

    @Test
    fun add_isCappedAndDropsOldest() {
        val s = sim()
        repeat(BubbleSimulation.MAX_BUBBLES + 10) { s.add(bubble(id = "b$it")) }
        assertEquals(BubbleSimulation.MAX_BUBBLES, s.bubbles.size)
        assertEquals("b10", s.bubbles.first().id)
    }

    @Test
    fun alpha_fadesDuringLastSeconds() {
        val b = bubble()
        assertEquals(1f, b.alpha(), 0.001f)
        b.age = BubbleSimulation.LIFETIME_SECONDS - BubbleSimulation.FADE_SECONDS / 2
        assertEquals(0.5f, b.alpha(), 0.001f)
        b.age = BubbleSimulation.LIFETIME_SECONDS
        assertEquals(0f, b.alpha(), 0.001f)
    }
}
