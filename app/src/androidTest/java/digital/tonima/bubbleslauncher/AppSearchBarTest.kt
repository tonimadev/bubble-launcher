package digital.tonima.bubbleslauncher

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import digital.tonima.bubbleslauncher.ui.search.AppSearchBar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppSearchBarTest {
    @get:Rule
    val rule = createComposeRule()

    private var query by mutableStateOf("")
    private var searches = 0

    private fun setContent() {
        query = ""
        searches = 0
        rule.setContent {
            MaterialTheme {
                AppSearchBar(query = query, onQueryChange = { query = it }, onSearch = { searches++ })
            }
        }
    }

    @Test
    fun typing_updatesQuery() {
        setContent()
        rule.onNode(hasSetTextAction()).performTextInput("cam")
        // contains(): some emulator images (Gboard on Android 17) inject stray "-" characters.
        assertTrue(query, query.contains("cam"))
    }

    @Test
    fun imeSearchKey_invokesOnSearch() {
        setContent()
        rule.onNode(hasSetTextAction()).performTextInput("cam")
        rule.onNode(hasSetTextAction()).performImeAction()
        assertEquals(1, searches)
        assertTrue("the query must be kept", query.contains("cam"))
    }

    @Test
    fun clearButton_emptiesQuery() {
        setContent()
        rule.onNode(hasSetTextAction()).performTextInput("cam")
        rule.onNodeWithText("✕").performClick()
        assertEquals("", query)
    }
}
