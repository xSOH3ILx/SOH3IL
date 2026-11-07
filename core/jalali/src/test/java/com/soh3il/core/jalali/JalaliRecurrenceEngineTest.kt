package com.soh3il.core.jalali

import com.soh3il.core.model.PreReminder
import com.soh3il.core.model.PreReminderUnit
import com.soh3il.core.model.RecurrenceRule
import com.soh3il.core.model.RecurrenceType
import com.soh3il.core.model.Reminder
import com.soh3il.core.model.ShortMonthPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JalaliRecurrenceEngineTest {

    /**
     * Requirement: When user sets a recurring reminder for the 3rd of every month,
     * it MUST fire on the 3rd of EVERY solar month without shifting to 2nd or 4th!
     */
    @Test
    fun testRecurrenceDay3OfEverySolarMonthNeverDrifts() {
        val reminder = Reminder(
            id = 1,
            title = "پرداخت اجاره / یادآور ۳ هر ماه",
            jalaliYear = 1405,
            jalaliMonth = 1,
            jalaliDay = 3,
            hour = 10,
            minute = 0,
            recurrence = RecurrenceRule(
                type = RecurrenceType.MONTHLY_JALALI,
                dayOfMonth = 3
            )
        )

        // Starting from 1405-01-01 00:00
        var currentRefEpoch = JalaliDate(1405, 1, 1).toEpochMillis(0, 0, 0)

        for (expectedMonth in 1..12) {
            val nextTrigger = JalaliRecurrenceEngine.calculateNextTrigger(
                reminder = reminder,
                referenceEpochMillis = currentRefEpoch
            )

            assertNotNull("باید تریگر برای ماه $expectedMonth پیدا شود", nextTrigger)

            val triggerDate = JalaliDate.fromEpochMillis(nextTrigger!!)
            assertEquals("سال باید ۱۴۰۵ باشد", 1405, triggerDate.year)
            assertEquals("ماه باید $expectedMonth باشد", expectedMonth, triggerDate.month)
            assertEquals("روز یادآور باید دقیقاً ۳ام ماه شمسی باشد و هرگز جابجا نشود", 3, triggerDate.day)

            // Advance reference time past this trigger to simulate moving to next month
            currentRefEpoch = nextTrigger + 1000L
        }
    }

    /**
     * Test edge case: When user sets reminder on the 31st of every month:
     * - Months 1-6 (Farvardin to Shahrivar) -> Day 31
     * - Months 7-11 (Mehr to Bahman) -> Day 30 (Snap to end of month)
     * - Month 12 (Esfand 1405) -> Day 29 (Snap to end of month)
     */
    @Test
    fun testRecurrenceDay31SnapsCorrectlyToEndOfShorterMonths() {
        val reminder = Reminder(
            id = 2,
            title = "تسویه حساب پایان ماه",
            jalaliYear = 1405,
            jalaliMonth = 1,
            jalaliDay = 31,
            hour = 18,
            minute = 0,
            recurrence = RecurrenceRule(
                type = RecurrenceType.MONTHLY_JALALI,
                dayOfMonth = 31,
                shortMonthPolicy = ShortMonthPolicy.SNAP_TO_LAST_DAY
            )
        )

        var currentRefEpoch = JalaliDate(1405, 1, 1).toEpochMillis(0, 0, 0)

        for (expectedMonth in 1..12) {
            val nextTrigger = JalaliRecurrenceEngine.calculateNextTrigger(
                reminder = reminder,
                referenceEpochMillis = currentRefEpoch
            )

            assertNotNull("باید تریگر برای ماه $expectedMonth باشد", nextTrigger)
            val triggerDate = JalaliDate.fromEpochMillis(nextTrigger!!)

            val expectedDay = when (expectedMonth) {
                in 1..6 -> 31
                in 7..11 -> 30
                12 -> 29 // Normal year 1405
                else -> 30
            }

            assertEquals("ماه باید $expectedMonth باشد", expectedMonth, triggerDate.month)
            assertEquals("در ماه $expectedMonth باید روز $expectedDay فعال شود", expectedDay, triggerDate.day)

            currentRefEpoch = nextTrigger + 1000L
        }
    }

    /**
     * Test SKIP_MONTH policy for day 31:
     * Should only trigger in months 1-6 (Farvardin to Shahrivar).
     * In months 7-12 it should skip directly to next year's Farvardin!
     */
    @Test
    fun testRecurrenceDay31SkipMonthPolicy() {
        val reminder = Reminder(
            id = 3,
            title = "یادآور ویژه روزهای ۳۱ام",
            jalaliYear = 1405,
            jalaliMonth = 6,
            jalaliDay = 31,
            hour = 12,
            minute = 0,
            recurrence = RecurrenceRule(
                type = RecurrenceType.MONTHLY_JALALI,
                dayOfMonth = 31,
                shortMonthPolicy = ShortMonthPolicy.SKIP_MONTH
            )
        )

        // Reference time is just after 1405-06-31
        val afterShahrivar = JalaliDate(1405, 6, 31).toEpochMillis(13, 0, 0)

        val nextTrigger = JalaliRecurrenceEngine.calculateNextTrigger(
            reminder = reminder,
            referenceEpochMillis = afterShahrivar
        )

        assertNotNull(nextTrigger)
        val triggerDate = JalaliDate.fromEpochMillis(nextTrigger!!)
        // Because months 7, 8, 9, 10, 11, 12 do not have 31 days, it should jump to 1406-01-31!
        assertEquals(1406, triggerDate.year)
        assertEquals(1, triggerDate.month)
        assertEquals(31, triggerDate.day)
    }

    @Test
    fun testYearlyJalaliRecurrence() {
        val reminder = Reminder(
            id = 4,
            title = "تولد / ۱ فروردین",
            jalaliYear = 1405,
            jalaliMonth = 1,
            jalaliDay = 1,
            hour = 9,
            minute = 0,
            recurrence = RecurrenceRule(type = RecurrenceType.YEARLY_JALALI)
        )

        // After 1405-01-01
        val afterNowruz1405 = JalaliDate(1405, 1, 1).toEpochMillis(10, 0, 0)
        val nextTrigger = JalaliRecurrenceEngine.calculateNextTrigger(reminder, afterNowruz1405)

        assertNotNull(nextTrigger)
        val nextDate = JalaliDate.fromEpochMillis(nextTrigger!!)
        assertEquals(1406, nextDate.year)
        assertEquals(1, nextDate.month)
        assertEquals(1, nextDate.day)
    }

    @Test
    fun testPreRemindersCalculation() {
        val mainTrigger = JalaliDate(1405, 1, 10).toEpochMillis(10, 0, 0)

        val preReminders = listOf(
            PreReminder(1, PreReminderUnit.DAYS),      // 1 day before
            PreReminder(30, PreReminderUnit.MINUTES)   // 30 min before
        )

        val triggers = JalaliRecurrenceEngine.calculatePreReminderTriggers(preReminders, mainTrigger)

        assertEquals(2, triggers.size)
        assertEquals(mainTrigger - 24 * 3600 * 1000L, triggers[0])
        assertEquals(mainTrigger - 30 * 60 * 1000L, triggers[1])
    }

    @Test
    fun testPreReminderCrossingYearBoundary() {
        // Event on 1405-01-01 at 10:00
        val nowruzEpoch = JalaliDate(1405, 1, 1).toEpochMillis(10, 0, 0)
        val oneDayBefore = listOf(PreReminder(1, PreReminderUnit.DAYS))

        val triggers = JalaliRecurrenceEngine.calculatePreReminderTriggers(oneDayBefore, nowruzEpoch)
        assertEquals(1, triggers.size)

        val preDate = JalaliDate.fromEpochMillis(triggers[0])
        // Should fall on 1404-12-29!
        assertEquals(1404, preDate.year)
        assertEquals(12, preDate.month)
        assertEquals(29, preDate.day)
    }

    @Test
    fun testCustomPreRemindersWithVariousUnits() {
        val mainTrigger = JalaliDate(1405, 5, 20).toEpochMillis(14, 0, 0)

        val customList = listOf(
            PreReminder(7, PreReminderUnit.DAYS),      // 7 days before
            PreReminder(3, PreReminderUnit.HOURS),     // 3 hours before
            PreReminder(45, PreReminderUnit.MINUTES)   // 45 min before
        )

        assertEquals(10080L, customList[0].toMinutes())
        assertEquals(180L, customList[1].toMinutes())
        assertEquals(45L, customList[2].toMinutes())

        val triggers = JalaliRecurrenceEngine.calculatePreReminderTriggers(customList, mainTrigger)
        assertEquals(3, triggers.size)

        // 7 days before
        assertEquals(mainTrigger - 7L * 24 * 3600 * 1000L, triggers[0])
        val sevenDaysBeforeDate = JalaliDate.fromEpochMillis(triggers[0])
        assertEquals(13, sevenDaysBeforeDate.day) // 20 - 7 = 13 Mordad

        // 3 hours before
        assertEquals(mainTrigger - 3L * 3600 * 1000L, triggers[1])

        // 45 min before
        assertEquals(mainTrigger - 45L * 60 * 1000L, triggers[2])
    }

    @Test
    fun testWeeklyRecurrenceOnlyOnSelectedDays() {
        val reminder = Reminder(
            id = 10,
            title = "جلسه شنبه‌ها و سه‌شنبه‌ها",
            jalaliYear = 1405,
            jalaliMonth = 1,
            jalaliDay = 1,
            hour = 9,
            minute = 0,
            recurrence = RecurrenceRule(
                type = RecurrenceType.WEEKLY,
                daysOfWeek = setOf(1, 4) // 1=Shanbeh, 4=Seshanbeh
            )
        )

        var refEpoch = JalaliDate(1405, 1, 1).toEpochMillis(0, 0, 0)

        // Find next 10 occurrences
        for (i in 1..10) {
            val nextTrigger = JalaliRecurrenceEngine.calculateNextTrigger(reminder, refEpoch)
            assertNotNull(nextTrigger)

            val triggerDate = JalaliDate.fromEpochMillis(nextTrigger!!)
            assertTrue(
                "روز هفته باید یا ۱ (شنبه) یا ۴ (سه‌شنبه) باشد اما ${triggerDate.dayOfWeek} بود",
                triggerDate.dayOfWeek == 1 || triggerDate.dayOfWeek == 4
            )

            refEpoch = nextTrigger + 1000L
        }
    }

    @Test
    fun testLeapYearEsfandRecurrenceSnapping() {
        // Recurring reminder on 30th of Esfand (end of year)
        val reminder = Reminder(
            id = 20,
            title = "انبارگردانی پایان سال",
            jalaliYear = 1403,
            jalaliMonth = 12,
            jalaliDay = 30,
            hour = 12,
            minute = 0,
            recurrence = RecurrenceRule(
                type = RecurrenceType.YEARLY_JALALI,
                shortMonthPolicy = ShortMonthPolicy.SNAP_TO_LAST_DAY
            )
        )

        // In 1403 (leap year) -> 1403-12-30
        val t1403 = JalaliRecurrenceEngine.calculateNextTrigger(
            reminder,
            JalaliDate(1403, 12, 1).toEpochMillis(0, 0, 0)
        )
        assertNotNull(t1403)
        val d1403 = JalaliDate.fromEpochMillis(t1403!!)
        assertEquals(1403, d1403.year)
        assertEquals(12, d1403.month)
        assertEquals(30, d1403.day)

        // In 1404 (common year, 29 days) -> snaps to 1404-12-29
        val t1404 = JalaliRecurrenceEngine.calculateNextTrigger(
            reminder,
            JalaliDate(1404, 1, 1).toEpochMillis(0, 0, 0)
        )
        assertNotNull(t1404)
        val d1404 = JalaliDate.fromEpochMillis(t1404!!)
        assertEquals(1404, d1404.year)
        assertEquals(12, d1404.month)
        assertEquals(29, d1404.day)
    }
}
