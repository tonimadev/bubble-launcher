package digital.tonima.bubbleslauncher.widget

import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import kotlin.math.hypot

/**
 * Wraps a widget host view and detects long-presses itself.
 *
 * A plain `setOnLongClickListener` on the host view is not enough: widgets whose root has a
 * click PendingIntent (Calendar, Gmail...) consume the touch, never report a long-click, and then
 * fire their click on release, launching the app. Here the touch is intercepted once the
 * long-press timeout elapses, the child receives ACTION_CANCEL, and no click happens.
 */
class LongPressFrameLayout(context: Context) : FrameLayout(context) {

    var onLongPress: (() -> Unit)? = null

    private var downX = 0f
    private var downY = 0f
    private var fired = false
    private val slop = ViewConfiguration.get(context).scaledTouchSlop

    private val trigger = Runnable {
        fired = true
        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        onLongPress?.invoke()
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                fired = false
                downX = ev.x
                downY = ev.y
                postDelayed(trigger, ViewConfiguration.getLongPressTimeout().toLong())
            }
            MotionEvent.ACTION_MOVE ->
                if (!fired && hypot(ev.x - downX, ev.y - downY) > slop) removeCallbacks(trigger)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> removeCallbacks(trigger)
        }
        // Once fired, take over the rest of the gesture so the child is cancelled.
        return fired
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            removeCallbacks(trigger)
        }
        return fired
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(trigger)
        super.onDetachedFromWindow()
    }
}
