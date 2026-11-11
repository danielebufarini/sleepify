package it.danielebufarini.sleepify

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.provider.AlarmClock
import android.view.View
import android.widget.TimePicker
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.hamcrest.Matchers.allOf

class SleepifyUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun resultsOpenOnSeparateScreenAndBackPreservesInputs() {
        rule.onNodeWithTag("reference_time").assertTextContains("07:00")
        rule.onNodeWithTag("result_6").assertDoesNotExist()
        openResults()
        rule.onNodeWithTag("reference_time").assertDoesNotExist()
        rule.onNodeWithTag("result_6").performScrollTo()
        rule.onNodeWithText("21:45").assertIsDisplayed()
        (3..6).forEach { rule.onNodeWithTag("copy_$it").assertDoesNotExist() }
        pressBack()
        rule.onNodeWithTag("reference_time").assertTextContains("07:00")
        rule.onNodeWithTag("mode_wake").performScrollTo().performClick().assertIsSelected()
        rule.onNodeWithTag("reference_time").assertTextContains("23:00")
        openResults()
        rule.onNodeWithTag("result_3").performScrollTo()
        rule.onNodeWithText("03:45").assertIsDisplayed()
        (3..6).forEach { rule.onNodeWithTag("copy_$it").assertDoesNotExist() }
        rule.onNodeWithTag("back_to_inputs").performClick()
        rule.onNodeWithTag("mode_wake").assertIsSelected()
        rule.onNodeWithTag("reference_time").assertTextContains("23:00")
    }

    @Test fun aboutOpensFromBothScreensWithoutLosingTheCalculation() {
        assertAbout()
        rule.onNodeWithTag("reference_time").assertTextContains("07:00")
        rule.onNodeWithTag("mode_wake").performScrollTo().performClick()
        openResults()
        assertAbout()
        rule.onNodeWithTag("result_3").performScrollTo()
        rule.onNodeWithText("03:45").assertIsDisplayed()
        rule.onNodeWithTag("back_to_inputs").performClick()
        rule.onNodeWithTag("mode_wake").assertIsSelected()
    }

    @Test fun nativeControlsRecalculateAndNormalizeCycleRange() {
        rule.onNodeWithTag("latency").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(30f) }
        openResults()
        rule.onNodeWithTag("result_6").performScrollTo()
        rule.onNodeWithText("21:30").assertIsDisplayed()
        rule.onNodeWithTag("back_to_inputs").performClick()
        rule.onNodeWithTag("min_cycles").performScrollTo().performClick()
        rule.onNodeWithTag("min_cycles_5").performClick()
        rule.onNodeWithTag("max_cycles").performClick()
        rule.onNodeWithTag("max_cycles_4").performClick()
        openResults()
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
        openResults()
        rule.activityRule.scenario.recreate()
        rule.onNodeWithTag("reference_time").assertDoesNotExist()
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
        openResults()
        rule.onNodeWithTag("result_6").performScrollTo()
        rule.onNodeWithText("22:55").assertIsDisplayed()
    }

    @Test fun systemAlarmUsesTheSelectedWakeTimeAndCalculatedWakeTimes() {
        Intents.init()
        try {
            Intents.intending(hasAction(AlarmClock.ACTION_SET_ALARM))
                .respondWith(ActivityResult(Activity.RESULT_OK, null))
            rule.onNodeWithTag("reference_time").performClick()
            onView(isAssignableFrom(TimePicker::class.java)).perform(object : ViewAction {
                override fun getConstraints() = isAssignableFrom(TimePicker::class.java)
                override fun getDescription() = "Set wake time to 08:10"
                override fun perform(uiController: UiController, view: View) {
                    (view as TimePicker).hour = 8
                    view.minute = 10
                }
            })
            onView(withId(android.R.id.button1)).perform(click())
            rule.onNodeWithTag("set_reference_alarm").performScrollTo().performClick()
            assertAlarmRequest(8, 10)
            openResults()
            rule.onNodeWithTag("alarm_6").assertDoesNotExist()
            rule.onNodeWithTag("back_to_inputs").performClick()

            rule.onNodeWithTag("mode_wake").performScrollTo().performClick()
            rule.onNodeWithTag("set_reference_alarm").assertDoesNotExist()
            openResults()
            rule.onNodeWithTag("alarm_3").performScrollTo().performClick()
            assertAlarmRequest(3, 45)
        } finally {
            Intents.release()
        }
    }

    private fun openResults() {
        rule.onNodeWithTag("show_results").performScrollTo().performClick()
        rule.onNodeWithTag("results_heading").assertIsDisplayed()
    }

    private fun assertAbout() {
        rule.onNodeWithTag("app_menu").performClick()
        rule.onNodeWithTag("menu_about").performClick()
        rule.onNodeWithTag("about_dialog").assertIsDisplayed()
        val info = rule.activity.packageManager.getPackageInfo(rule.activity.packageName, 0)
        rule.onNodeWithTag("about_version").assertTextContains("${info.versionName} (${info.longVersionCode})", substring = true)
        rule.onNodeWithTag("close_about").performClick()
        rule.onNodeWithTag("about_dialog").assertDoesNotExist()
    }

    private fun assertAlarmRequest(hour: Int, minute: Int) {
        Intents.intended(allOf(
            hasAction(AlarmClock.ACTION_SET_ALARM),
            hasExtra(AlarmClock.EXTRA_HOUR, hour),
            hasExtra(AlarmClock.EXTRA_MINUTES, minute),
            hasExtra(AlarmClock.EXTRA_MESSAGE, "Sleepify"),
            hasExtra(AlarmClock.EXTRA_SKIP_UI, false),
        ))
    }
}
