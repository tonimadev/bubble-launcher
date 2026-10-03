package digital.tonima.bubbleslauncher.widget

import kotlin.math.roundToInt

/** Pure sizing rules for hosted widgets (unit-testable; no Android types). */
object WidgetSizing {
    const val MIN_HEIGHT_DP = 110
    const val MAX_HEIGHT_DP = 360
    const val DEFAULT_HEIGHT_DP = 140

    /** Bounds used when a provider declares no resize limits. */
    const val MIN_RESIZE_DP = 72
    const val MAX_RESIZE_DP = 520

    // Mirror AppWidgetProviderInfo.RESIZE_HORIZONTAL / RESIZE_VERTICAL.
    private const val RESIZE_HORIZONTAL = 1
    private const val RESIZE_VERTICAL = 2

    /**
     * Initial height for a newly added widget. Provider sizes ([minHeightPx], [minResizeHeightPx])
     * are in **pixels**, not dp. Uses the larger of the two so it is never smaller than supported.
     */
    fun heightDp(minHeightPx: Int, minResizeHeightPx: Int, density: Float): Int {
        val px = maxOf(minHeightPx, minResizeHeightPx)
        if (px <= 0 || density <= 0f) return DEFAULT_HEIGHT_DP
        return (px / density).roundToInt().coerceIn(MIN_HEIGHT_DP, MAX_HEIGHT_DP)
    }

    /** Allowed height range for the resize handle, from the provider's declared limits (px). */
    fun heightBounds(minResizeHeightPx: Int, maxResizeHeightPx: Int, density: Float): IntRange {
        val lo = if (minResizeHeightPx > 0 && density > 0f) {
            (minResizeHeightPx / density).roundToInt().coerceIn(MIN_RESIZE_DP, 200)
        } else MIN_RESIZE_DP
        val hi = if (maxResizeHeightPx > 0 && density > 0f) {
            (maxResizeHeightPx / density).roundToInt().coerceIn(lo, MAX_RESIZE_DP)
        } else MAX_RESIZE_DP
        return lo..hi
    }

    fun clampHeight(heightDp: Int, bounds: IntRange): Int = heightDp.coerceIn(bounds.first, bounds.last)

    fun canResizeVertically(resizeMode: Int): Boolean = resizeMode and RESIZE_VERTICAL != 0

    fun canResizeHorizontally(resizeMode: Int): Boolean = resizeMode and RESIZE_HORIZONTAL != 0

    fun pxToDp(px: Int, density: Float): Int = if (density <= 0f) 0 else (px / density).roundToInt()
}
