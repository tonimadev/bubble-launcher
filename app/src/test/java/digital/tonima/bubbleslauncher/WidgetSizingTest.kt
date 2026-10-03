package digital.tonima.bubbleslauncher

import digital.tonima.bubbleslauncher.data.model.WidgetEntry
import digital.tonima.bubbleslauncher.widget.WidgetLayout
import digital.tonima.bubbleslauncher.widget.WidgetSizing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetSizingTest {
    @Test fun convertsPixelsToDp() = assertEquals(200, WidgetSizing.heightDp(600, 0, 3f))

    @Test fun usesLargerOfMinAndMinResize() = assertEquals(150, WidgetSizing.heightDp(300, 450, 3f))

    @Test fun clampsTooSmall() = assertEquals(WidgetSizing.MIN_HEIGHT_DP, WidgetSizing.heightDp(30, 0, 3f))

    @Test fun clampsHugeProviderSizes() =
        assertEquals(WidgetSizing.MAX_HEIGHT_DP, WidgetSizing.heightDp(46081, 46081, 2.75f))

    @Test fun unknownSizeFallsBackToDefault() {
        assertEquals(WidgetSizing.DEFAULT_HEIGHT_DP, WidgetSizing.heightDp(0, 0, 3f))
        assertEquals(WidgetSizing.DEFAULT_HEIGHT_DP, WidgetSizing.heightDp(300, 0, 0f))
    }

    @Test fun pxToDp() {
        assertEquals(360, WidgetSizing.pxToDp(1080, 3f))
        assertEquals(0, WidgetSizing.pxToDp(100, 0f))
    }

    @Test fun heightBounds_defaultsWhenProviderDeclaresNothing() =
        assertEquals(WidgetSizing.MIN_RESIZE_DP..WidgetSizing.MAX_RESIZE_DP, WidgetSizing.heightBounds(0, 0, 3f))

    @Test fun heightBounds_usesProviderLimits() = assertEquals(100..300, WidgetSizing.heightBounds(300, 900, 3f))

    @Test fun heightBounds_capsAbsurdMinimumAndKeepsMaxAboveMin() {
        // e.g. providers that report a huge min size must still leave a usable range
        val b = WidgetSizing.heightBounds(46081, 100, 2.75f)
        assertEquals(200, b.first)
        assertTrue(b.last >= b.first)
    }

    @Test fun clampHeight() {
        assertEquals(100, WidgetSizing.clampHeight(50, 100..300))
        assertEquals(300, WidgetSizing.clampHeight(999, 100..300))
        assertEquals(150, WidgetSizing.clampHeight(150, 100..300))
    }

    @Test fun resizeModeFlags() {
        assertTrue(WidgetSizing.canResizeVertically(2))
        assertTrue(WidgetSizing.canResizeVertically(3))
        assertFalse(WidgetSizing.canResizeVertically(1))
        assertFalse(WidgetSizing.canResizeVertically(0))
        assertTrue(WidgetSizing.canResizeHorizontally(1))
        assertFalse(WidgetSizing.canResizeHorizontally(2))
    }

    private fun w(id: Int, half: Boolean) = WidgetEntry(id = id, appWidgetId = id, halfWidth = half)

    @Test fun rows_fullWidthGetOneRowEach() {
        val rows = WidgetLayout.rows(listOf(w(1, false), w(2, false)))
        assertEquals(listOf(listOf(1), listOf(2)), rows.map { r -> r.map { it.appWidgetId } })
    }

    @Test fun rows_pairsConsecutiveHalves() {
        val rows = WidgetLayout.rows(listOf(w(1, true), w(2, true), w(3, false), w(4, true), w(5, true)))
        assertEquals(listOf(listOf(1, 2), listOf(3), listOf(4, 5)), rows.map { r -> r.map { it.appWidgetId } })
    }

    @Test fun rows_loneHalfStaysAlone() {
        val rows = WidgetLayout.rows(listOf(w(1, true), w(2, false), w(3, true)))
        assertEquals(listOf(listOf(1), listOf(2), listOf(3)), rows.map { r -> r.map { it.appWidgetId } })
    }

    @Test fun rows_threeHalvesMakePairPlusSingle() {
        val rows = WidgetLayout.rows(listOf(w(1, true), w(2, true), w(3, true)))
        assertEquals(listOf(listOf(1, 2), listOf(3)), rows.map { r -> r.map { it.appWidgetId } })
    }

    @Test fun rows_emptyAndKeepsEveryWidget() {
        assertTrue(WidgetLayout.rows(emptyList()).isEmpty())
        val all = (1..7).map { w(it, it % 2 == 1) }
        assertEquals(7, WidgetLayout.rows(all).sumOf { it.size })
    }
}
