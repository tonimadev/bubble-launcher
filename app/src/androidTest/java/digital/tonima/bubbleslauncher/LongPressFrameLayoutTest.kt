package digital.tonima.bubbleslauncher

import android.view.View
import android.view.ViewConfiguration
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.center
import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.moveBy
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import digital.tonima.bubbleslauncher.widget.LongPressFrameLayout
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression: widgets whose root has a click PendingIntent (Calendar, Gmail...) used to launch
 * their app on release after a long-press instead of opening the launcher's menu.
 */
@RunWith(AndroidJUnit4::class)
class LongPressFrameLayoutTest {
    @get:Rule
    val rule = createComposeRule()

    private var longPresses = 0
    private var childClicks = 0

    private fun setContent() {
        longPresses = 0
        childClicks = 0
        rule.setContent {
            AndroidView(
                factory = { ctx ->
                    LongPressFrameLayout(ctx).apply {
                        onLongPress = { longPresses++ }
                        // A clickable child, like a widget root with a click PendingIntent.
                        addView(View(ctx).apply {
                            isClickable = true
                            setOnClickListener { childClicks++ }
                        })
                    }
                },
                modifier = Modifier
                    .size(200.dp)
                    .testTag("widget")
            )
        }
    }

    // The long-press timer is a real Handler delay, so the test waits in real time between events.
    private fun pause() = Thread.sleep(ViewConfiguration.getLongPressTimeout() + 300L)

    @Test
    fun quickTap_clicksChild_withoutLongPress() {
        setContent()
        rule.onNodeWithTag("widget").performTouchInput { down(center); up() }
        rule.waitForIdle()
        assertEquals(0, longPresses)
        assertEquals(1, childClicks)
    }

    @Test
    fun longPress_firesCallback_andDoesNotClickChild() {
        setContent()
        rule.onNodeWithTag("widget").performTouchInput { down(center) }
        pause()
        rule.onNodeWithTag("widget").performTouchInput { up() }
        rule.waitForIdle()
        assertEquals(1, longPresses)
        assertEquals("releasing after a long-press must not click the widget", 0, childClicks)
    }

    @Test
    fun movingBeforeTimeout_cancelsLongPress() {
        setContent()
        rule.onNodeWithTag("widget").performTouchInput {
            down(center)
            moveBy(Offset(120f, 0f))
        }
        pause()
        rule.onNodeWithTag("widget").performTouchInput { up() }
        rule.waitForIdle()
        assertEquals(0, longPresses)
    }
}
