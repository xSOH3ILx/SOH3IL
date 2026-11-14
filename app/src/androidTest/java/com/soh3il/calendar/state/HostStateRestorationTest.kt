package com.soh3il.calendar.state

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.soh3il.calendar.MainActivity
import com.soh3il.core.designsystem.utils.toPersianDigits
import com.soh3il.core.jalali.JalaliDate
import org.junit.Rule
import org.junit.Test

class HostStateRestorationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun displayedMonthSurvivesActivityRecreation() {
        val date = JalaliDate.now().plusMonths(1)
        val title = "${date.monthNameFa} ${date.year.toPersianDigits()}"
        compose.onNodeWithContentDescription("ماه بعد").performClick()
        compose.onNodeWithText(title).assertExists()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText(title).assertExists()
    }

    @Test fun openReminderAndDraftSurviveActivityRecreation() {
        compose.onNodeWithContentDescription("افزودن یادآور").performClick()
        compose.onNodeWithText("عنوان یادآور").performTextInput("آزمون بازسازی صفحه")
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("عنوان یادآور").assertTextContains("آزمون بازسازی صفحه")
    }
}
