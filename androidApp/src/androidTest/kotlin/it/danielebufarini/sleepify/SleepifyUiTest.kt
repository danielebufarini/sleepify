package it.danielebufarini.sleepify

import android.content.ClipboardManager
import android.content.Context
import android.view.View
import android.widget.TimePicker
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SleepifyUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun nativeModesRenderSharedResultsAndCopyToClipboard() {
        rule.onNodeWithTag("reference_time").assertTextContains("07:00")
        rule.onNodeWithTag("result_6").performScrollTo()
        rule.onNodeWithText("21:45").assertIsDisplayed()
        rule.onNodeWithTag("copy_6").performClick()
        rule.runOnIdle {
            val clipboard = rule.activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            assertEquals("21:45", clipboard.primaryClip!!.getItemAt(0).text.toString())
        }
        rule.onNodeWithTag("mode_wake").performScrollTo().performClick().assertIsSelected()
        rule.onNodeWithTag("reference_time").assertTextContains("23:00")
        rule.onNodeWithTag("result_3").performScrollTo()
        rule.onNodeWithText("03:45").assertIsDisplayed()
    }

    @Test fun nativeControlsRecalculateAndNormalizeCycleRange() {
        rule.onNodeWithTag("latency").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(30f) }
        rule.onNodeWithTag("result_6").performScrollTo()
        rule.onNodeWithText("21:30").assertIsDisplayed()
        rule.onNodeWithTag("min_cycles").performScrollTo().performClick()
        rule.onNodeWithTag("min_cycles_5").performClick()
        rule.onNodeWithTag("max_cycles").performClick()
        rule.onNodeWithTag("max_cycles_4").performClick()
        rule.onNodeWithTag("result_5").performScrollTo()
        rule.onNodeWithText("23:00").assertIsDisplayed()
        rule.onNodeWithTag("result_6").assertDoesNotExist()
        rule.onNodeWithTag("result_4").assertDoesNotExist()
    }

    @Test fun activityRecreationRestoresSharedInputs() {
        rule.onNodeWithTag("mode_wake").performClick()
        rule.onNodeWithTag("latency").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(45f) }
        rule.activityRule.scenario.recreate()
        rule.onNodeWithTag("mode_wake").assertIsSelected()
        rule.onNodeWithTag("reference_time").assertTextContains("23:00")
        rule.onNodeWithTag("result_3").performScrollTo()
        rule.onNodeWithText("04:15").assertIsDisplayed()
    }

    @Test fun nativeTimeDialogUpdatesSharedCalculation() {
        rule.onNodeWithTag("reference_time").performClick()
        onView(isAssignableFrom(TimePicker::class.java)).perform(object : ViewAction {
            override fun getConstraints() = isAssignableFrom(TimePicker::class.java)
            override fun getDescription() = "Set the native time picker to 08:10"
            override fun perform(uiController: UiController, view: View) {
                (view as TimePicker).hour = 8
                view.minute = 10
            }
        })
        onView(withId(android.R.id.button1)).perform(click())
        rule.onNodeWithTag("reference_time").assertTextContains("08:10")
        rule.onNodeWithTag("result_6").performScrollTo()
        rule.onNodeWithText("22:55").assertIsDisplayed()
    }
}
