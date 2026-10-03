package digital.tonima.bubbleslauncher

import digital.tonima.bubbleslauncher.widget.WidgetSizing
import org.junit.Assert.assertEquals
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
}
