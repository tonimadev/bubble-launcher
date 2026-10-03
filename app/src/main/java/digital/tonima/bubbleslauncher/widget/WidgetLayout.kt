package digital.tonima.bubbleslauncher.widget

import digital.tonima.bubbleslauncher.data.model.WidgetEntry

/** Pure grouping of widgets into display rows (unit-testable). */
object WidgetLayout {
    /**
     * Full-width widgets get a row each. Consecutive half-width widgets are paired side by side;
     * a half-width widget with no half-width neighbour sits alone in its row (left half).
     */
    fun rows(widgets: List<WidgetEntry>): List<List<WidgetEntry>> {
        val rows = ArrayList<List<WidgetEntry>>()
        var i = 0
        while (i < widgets.size) {
            val w = widgets[i]
            if (w.halfWidth && i + 1 < widgets.size && widgets[i + 1].halfWidth) {
                rows += listOf(w, widgets[i + 1])
                i += 2
            } else {
                rows += listOf(w)
                i += 1
            }
        }
        return rows
    }
}
