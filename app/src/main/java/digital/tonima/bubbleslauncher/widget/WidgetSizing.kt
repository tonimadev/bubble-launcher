package digital.tonima.bubbleslauncher.widget

import kotlin.math.roundToInt

/** Pure sizing rules for hosted widgets (unit-testable; no Android types). */
object WidgetSizing {
    const val MIN_HEIGHT_DP = 96
    const val MAX_HEIGHT_DP = 360
    const val DEFAULT_HEIGHT_DP = 140

    /**
     * Provider sizes ([minHeightPx], [minResizeHeightPx]) are in **pixels**, not dp.
     * Uses the larger of the two so the widget is never rendered smaller than it supports.
     */
    fun heightDp(minHeightPx: Int, minResizeHeightPx: Int, density: Float): Int {
        val px = maxOf(minHeightPx, minResizeHeightPx)
        if (px <= 0 || density <= 0f) return DEFAULT_HEIGHT_DP
        return (px / density).roundToInt().coerceIn(MIN_HEIGHT_DP, MAX_HEIGHT_DP)
    }

    fun pxToDp(px: Int, density: Float): Int = if (density <= 0f) 0 else (px / density).roundToInt()
}
