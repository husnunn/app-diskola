package id.diskola.app.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import id.diskola.app.ui.theme.DiskolaTheme
import org.junit.Rule
import org.junit.Test

class LoadingStateTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun skeletonIsAnnouncedAsMemuatAndShowsNoSpinnerText() {
        composeRule.setContent { DiskolaTheme { LoadingState() } }

        // TalkBack gets one description for the whole skeleton; the old visible "Memuat…" label is gone.
        composeRule.onNodeWithContentDescription("Memuat").assertExists()
        composeRule.onNodeWithText("Memuat…").assertDoesNotExist()
    }

    @Test
    fun everyVariantIsAnnouncedTheSameWay() {
        SkeletonVariant.entries.forEach { variant ->
            composeRule.setContent { DiskolaTheme { LoadingState(variant = variant) } }
            composeRule.onNodeWithContentDescription("Memuat").assertExists()
        }
    }
}
