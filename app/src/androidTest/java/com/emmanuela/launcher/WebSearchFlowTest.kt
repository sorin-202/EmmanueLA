package com.emmanuela.launcher

import android.content.Intent
import android.os.SystemClock
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.emmanuela.launcher.data.AppFolder
import com.emmanuela.launcher.data.Preferences
import com.emmanuela.launcher.data.UiPreferences
import com.emmanuela.launcher.data.ExperiencePreferences
import com.emmanuela.launcher.data.WebSearchHistory
import com.emmanuela.launcher.data.WebSearchHistoryRepository
import com.emmanuela.launcher.data.WebSearchEngine
import com.emmanuela.launcher.platform.WebSearch
import com.emmanuela.launcher.ui.apps.CompactFolderSearchResult
import com.emmanuela.launcher.ui.apps.CursorSearchField
import com.emmanuela.launcher.ui.apps.WebSearchHistoryList
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class WebSearchFlowTest {
    @get:Rule val compose = createComposeRule()

    private fun SemanticsNodeInteraction.assertExactSearchText(expected: String): SemanticsNodeInteraction {
        val config = fetchSemanticsNode().config
        val input = config[SemanticsProperties.InputText]
        val editable = config[SemanticsProperties.EditableText]
        // CoreTextField exposes composition underline spans in EditableText. Those
        // IME-owned annotations may change while the exact input characters do not.
        assertEquals("Raw search text (spans=${input.spanStyles})", expected, input.text)
        assertEquals("Displayed search text (spans=${editable.spanStyles})", expected, editable.text)
        return this
    }

    @Test fun keyboardSearchSubmitsImmediatelyWithoutAResultTap() {
        var submitted: String? = null
        compose.setContent {
            var input by remember { mutableStateOf("") }
            MaterialTheme {
                CursorSearchField(input, { input = it }, Preferences(), onSearch = {
                    submitted = WebSearchHistory.fromInput(input)
                })
            }
        }
        compose.onNode(hasSetTextAction()).performTextInput("/celestial navigation")
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.runOnIdle { assertEquals("celestial navigation", submitted) }
    }

    @Test fun hardwareEnterSubmitsOnce() {
        var submissions = 0
        compose.setContent {
            MaterialTheme { CursorSearchField("/navigation", {}, Preferences(), onSearch = { submissions++ }) }
        }
        compose.onNode(hasSetTextAction()).performClick().performKeyInput { pressKey(Key.Enter) }
        compose.runOnIdle { assertEquals(1, submissions) }
    }

    @Test fun focusedCustomCaretSurvivesReplacementSelectionClearAndSearch() {
        val submitted = mutableListOf<String>()
        val events = CopyOnWriteArrayList<String>()
        fun record(event: String) { events.add("${SystemClock.uptimeMillis()}: $event") }
        val preferences = Preferences(ui = UiPreferences(searchCursorBlinkMs = 0,
            searchCursorColor = 0xFFFF00FFL,
            experience = ExperiencePreferences(cursorStyle = "Custom", cursorCharacter = "|")))
        compose.setContent {
            var input by remember { mutableStateOf("") }
            MaterialTheme {
                CursorSearchField(input, {
                    record("change '$input' -> '$it'")
                    input = it
                }, preferences, onSearch = {
                    record("submit '$input'")
                    WebSearchHistory.fromInput(input)?.let(submitted::add)
                })
            }
        }
        val queries = listOf("/celestial navigation", "/x", "/a much longer replacement query", "/🌙 café 🛰️", "/final query")
        val field = compose.onNode(hasSetTextAction())
        // No delay between edits: every replacement/clear can invalidate the layout used
        // by the draw-only caret while input and selection already contain the new text.
        queries.forEach { query ->
            record("replace '$query'")
            field.performClick().performTextReplacement(query)
            field.assertExactSearchText(query)
            field.assert(SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, TextRange(query.length)))
            val selection = TextRange(1, query.length)
            field.performTextInputSelection(selection)
            field.assert(SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, selection))
            field.performTextInputSelection(TextRange(query.length))
            field.performImeAction()
            record("tap Clear for '$query'")
            try {
                compose.onNodeWithText("Clear").performClick()
                // Keep exact characters: the trace distinguishes a missed touch
                // from a subsequent input callback restoring the previous query.
                field.assertExactSearchText("")
                field.assert(SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, TextRange.Zero))
                record("Clear verified for '$query'")
            } catch (failure: AssertionError) {
                throw AssertionError("Clear failed for '$query'. Input callback trace:\n${events.joinToString("\n")}", failure)
            }
        }
        compose.runOnIdle { assertEquals(queries.mapNotNull(WebSearchHistory::fromInput), submitted) }
        field.performClick().performTextReplacement("/ready")
        field.assertExactSearchText("/ready")
        field.assert(SemanticsMatcher.expectValue(SemanticsProperties.TextSelectionRange, TextRange(6)))
        val pixels = field.captureToImage().toPixelMap()
        val hasCustomCaret = (0 until pixels.height).any { y -> (0 until pixels.width).any { x ->
            val color = pixels[x, y]
            color.red > .8f && color.blue > .8f && color.green < .6f
        } }
        assertTrue("The configured custom caret must resume drawing after the new text is laid out", hasCustomCaret)
        field.performImeAction()
        compose.runOnIdle { assertEquals("ready", submitted.last()) }
    }

    @Test fun historyTapExecutesAndClearRemovesRows() {
        var searched: String? = null
        compose.setContent {
            var history by remember { mutableStateOf(listOf("celestial navigation", "lunar orbit")) }
            MaterialTheme {
                WebSearchHistoryList(history, { searched = it }, { history = emptyList() })
            }
        }
        compose.onNodeWithText("lunar orbit").performClick()
        compose.runOnIdle { assertEquals("lunar orbit", searched) }
        compose.onNodeWithText("Clear history").performClick()
        compose.onNodeWithText("lunar orbit").assertDoesNotExist()
        compose.onNodeWithText("celestial navigation").assertDoesNotExist()
    }

    @Test fun historySurvivesRepositoryRecreationAndClearPersists() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = WebSearchHistoryRepository(context)
        repository.clear()
        try {
            repository.setEngine(WebSearchEngine.BING)
            (1..7).forEach { repository.record("search $it") }
            repository.record("SEARCH 5")
            assertEquals(listOf("SEARCH 5", "search 7", "search 6", "search 4", "search 3"), WebSearchHistoryRepository(context).queries.first())
            repository.clear()
            assertTrue(WebSearchHistoryRepository(context).queries.first().isEmpty())
            assertEquals(WebSearchEngine.BING, WebSearchHistoryRepository(context).engine.first())
            repository.reset()
            assertEquals(WebSearchEngine.ANDROID_DEFAULT, WebSearchHistoryRepository(context).engine.first())
        } finally { repository.reset() }
    }

    @Test fun browserFallbackEncodesQueryAsData() {
        val query = "stars & moon /?q=other #🌙"
        WebSearchEngine.entries.forEach { engine ->
            val intent = WebSearch.browserIntent(query, engine)
            assertEquals(Intent.ACTION_VIEW, intent.action)
            assertTrue(intent.hasCategory(Intent.CATEGORY_BROWSABLE))
            assertEquals("https", intent.data?.scheme)
            assertEquals(engine.host, intent.data?.host)
            assertEquals(query, intent.data?.getQueryParameter("q"))
            assertNull(intent.data?.fragment)
        }
    }

    @Test fun userCanChooseAnEngineWithoutExecutingASearch() {
        var selected: WebSearchEngine? = null
        var searched = false
        compose.setContent {
            MaterialTheme { WebSearchHistoryList(emptyList(), { searched = true }, {}, chooseEngine = { selected = it }) }
        }
        compose.onNodeWithText("Search engine: Android default").performClick()
        compose.onNodeWithText("Google").performClick()
        compose.runOnIdle {
            assertEquals(WebSearchEngine.GOOGLE, selected)
            assertFalse(searched)
        }
    }

    @Test fun protectedFolderResultIsCompactAndUsesNormalOpenCallback() {
        val folder = AppFolder("protected", "Navigation", "folder", listOf("private.app/.Main"), deviceProtected = true)
        var opened: String? = null
        compose.setContent {
            MaterialTheme { Column { CompactFolderSearchResult(folder) { opened = it } } }
        }
        compose.onNodeWithText("private.app/.Main").assertDoesNotExist()
        compose.onNodeWithContentDescription("Folder: Navigation, protected").performClick()
        compose.runOnIdle { assertEquals("protected", opened) }
    }
}
