package it.danielebufarini.sleepify

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class HomeLayoutTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun showTimesIsFullyVisibleWithoutScrollingInBothModes() {
        assertShowTimesFullyVisible()
        rule.onNodeWithTag("show_results").performTouchInput { click() }
        rule.onNodeWithTag("results_heading").assertIsDisplayed()
        rule.onNodeWithTag("back_to_inputs").performClick()
        rule.onNodeWithTag("reference_time").assertTextContains("07:00")
        rule.onNodeWithTag("mode_wake").performClick()
        assertShowTimesFullyVisible()
        rule.onNodeWithTag("show_results").performTouchInput { click() }
        rule.onNodeWithTag("result_3").assertIsDisplayed()
        rule.onNodeWithText("03:45").assertIsDisplayed()
    }

    @Test fun homeHasNoRedundantAppTitleAndKeepsAboutAccessible() {
        rule.onNodeWithText("Sleepify").assertDoesNotExist()
        rule.onNodeWithTag("app_menu").assertIsDisplayed().performTouchInput { click() }
        rule.onNodeWithTag("menu_about").performClick()
        rule.onNodeWithTag("about_dialog").assertIsDisplayed()
        rule.onNodeWithText("Sleepify").assertIsDisplayed()
        rule.onNodeWithTag("close_about").performClick()
        assertShowTimesFullyVisible()
    }

    private fun assertShowTimesFullyVisible() {
        val button = rule.onNodeWithTag("show_results").assertIsDisplayed()
        assertEquals("Show times must be fully visible without scrolling",
            button.getUnclippedBoundsInRoot(), button.getBoundsInRoot())
    }
}
