package com.soh3il.calendar.state

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.soh3il.core.designsystem.theme.Soh3ilTheme
import com.soh3il.core.designsystem.utils.toPersianDigits
import com.soh3il.core.jalali.JalaliDate
import com.soh3il.feature.calendar.CalendarScreen
import com.soh3il.feature.reminder.AddReminderSheet
import org.junit.Rule
import org.junit.Test

class UiStateRestorationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun reminderDraftSurvivesStateRestoration() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            Soh3ilTheme { AddReminderSheet(initialDate = JalaliDate(1405, 8, 23), onDismiss = {}, onSaveReminder = {}) }
        }
        compose.onNodeWithText("عنوان یادآور").performTextInput("آزمون حفظ پیش‌نویس")
        compose.onNodeWithText("توضیحات یا یادداشت (اختیاری)").performTextInput("یادداشت آزمایشی")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("عنوان یادآور").assertTextContains("آزمون حفظ پیش‌نویس")
        compose.onNodeWithText("توضیحات یا یادداشت (اختیاری)").assertTextContains("یادداشت آزمایشی")
    }

    @Test fun displayedCalendarMonthSurvivesStateRestoration() {
        val restoration = StateRestorationTester(compose)
        val next = JalaliDate.now().plusMonths(1)
        restoration.setContent { Soh3ilTheme { CalendarScreen() } }
        compose.onNodeWithContentDescription("ماه بعد").performClick()
        val title = "${next.monthNameFa} ${next.year.toPersianDigits()}"
        compose.onNodeWithText(title).assertExists()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText(title).assertExists()
    }
}
