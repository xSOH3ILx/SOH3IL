package com.soh3il.core.jalali

import com.soh3il.core.model.PreReminder
import com.soh3il.core.model.RecurrenceRule
import com.soh3il.core.model.RecurrenceType
import com.soh3il.core.model.Reminder
import com.soh3il.core.model.ShortMonthPolicy
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

object JalaliRecurrenceEngine {

    /**
     * Calculates the exact next epoch millis when this reminder should fire,
     * strictly adhering to the Solar Hijri (Jalali) calendar rules without any drift.
     */
    fun calculateNextTrigger(
        reminder: Reminder,
        referenceEpochMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = JalaliDate.TEHRAN_ZONE
    ): Long? {
        if (!reminder.isEnabled) return null

        val refZdt = Instant.ofEpochMilli(referenceEpochMillis).atZone(zoneId)
        val todayJalali = JalaliDate.fromEpochMillis(referenceEpochMillis, zoneId)

        return when (reminder.recurrence.type) {
            RecurrenceType.NONE -> {
                val targetDate = JalaliDate(reminder.jalaliYear, reminder.jalaliMonth, reminder.jalaliDay)
                val targetEpoch = targetDate.toEpochMillis(reminder.hour, reminder.minute, 0, zoneId)
                if (targetEpoch > referenceEpochMillis) targetEpoch else null
            }

            RecurrenceType.DAILY -> {
                val interval = reminder.recurrence.interval.coerceAtLeast(1)
                var candidate = todayJalali
                var targetEpoch = candidate.toEpochMillis(reminder.hour, reminder.minute, 0, zoneId)
                if (targetEpoch <= referenceEpochMillis) {
                    candidate = candidate.plusDays(interval.toLong())
                    targetEpoch = candidate.toEpochMillis(reminder.hour, reminder.minute, 0, zoneId)
                }
                targetEpoch
            }

            RecurrenceType.WEEKLY -> {
                val targetDaysOfWeek = if (reminder.recurrence.daysOfWeek.isNotEmpty()) {
                    reminder.recurrence.daysOfWeek
                } else {
                    setOf(JalaliDate(reminder.jalaliYear, reminder.jalaliMonth, reminder.jalaliDay).dayOfWeek)
                }

                var candidate = todayJalali
                // Check up to 14 days ahead to find the next matching day of the week
                for (offset in 0..14) {
                    val dateToCheck = candidate.plusDays(offset.toLong())
                    if (dateToCheck.dayOfWeek in targetDaysOfWeek) {
                        val epoch = dateToCheck.toEpochMillis(reminder.hour, reminder.minute, 0, zoneId)
                        if (epoch > referenceEpochMillis) {
                            return epoch
                        }
                    }
                }
                null
            }

            RecurrenceType.MONTHLY_JALALI -> {
                calculateNextMonthlyJalaliTrigger(
                    reminder = reminder,
                    referenceEpochMillis = referenceEpochMillis,
                    zoneId = zoneId
                )
            }

            RecurrenceType.YEARLY_JALALI -> {
                val targetMonth = reminder.jalaliMonth
                val targetDay = reminder.jalaliDay
                var targetYear = todayJalali.year

                // Try this year first
                val maxDaysThisYear = JalaliDate.getDaysInMonth(targetYear, targetMonth)
                val dayThisYear = targetDay.coerceAtMost(maxDaysThisYear)
                val thisYearEpoch = JalaliDate(targetYear, targetMonth, dayThisYear)
                    .toEpochMillis(reminder.hour, reminder.minute, 0, zoneId)

                if (thisYearEpoch > referenceEpochMillis) {
                    thisYearEpoch
                } else {
                    // Otherwise next year
                    targetYear += reminder.recurrence.interval.coerceAtLeast(1)
                    val maxDaysNextYear = JalaliDate.getDaysInMonth(targetYear, targetMonth)
                    val dayNextYear = targetDay.coerceAtMost(maxDaysNextYear)
                    JalaliDate(targetYear, targetMonth, dayNextYear)
                        .toEpochMillis(reminder.hour, reminder.minute, 0, zoneId)
                }
            }
        }
    }

    /**
     * Computes the exact monthly trigger in Solar Hijri calendar.
     * Guarantees that if the user configured day 3 of every month, it will ALWAYS
     * trigger on day 3 of that Jalali month.
     */
    private fun calculateNextMonthlyJalaliTrigger(
        reminder: Reminder,
        referenceEpochMillis: Long,
        zoneId: ZoneId
    ): Long {
        val todayJalali = JalaliDate.fromEpochMillis(referenceEpochMillis, zoneId)
        val targetDayOfMonth = reminder.recurrence.dayOfMonth ?: reminder.jalaliDay
        val interval = reminder.recurrence.interval.coerceAtLeast(1)
        val policy = reminder.recurrence.shortMonthPolicy

        // 1. Check if it can trigger in the current Jalali month
        val daysInCurrentMonth = todayJalali.daysInMonth
        val candidateDay = resolveDayForMonth(targetDayOfMonth, daysInCurrentMonth, policy)

        if (candidateDay != null) {
            val candidateEpoch = JalaliDate(todayJalali.year, todayJalali.month, candidateDay)
                .toEpochMillis(reminder.hour, reminder.minute, 0, zoneId)

            if (candidateEpoch > referenceEpochMillis) {
                return candidateEpoch
            }
        }

        // 2. Otherwise search in subsequent months step-by-step
        var monthsToAdd = interval
        while (true) {
            val totalMonths = (todayJalali.year * 12 + (todayJalali.month - 1)) + monthsToAdd
            val nextYear = totalMonths / 12
            val nextMonth = (totalMonths % 12) + 1
            val daysInNextMonth = JalaliDate.getDaysInMonth(nextYear, nextMonth)

            val resolvedDay = resolveDayForMonth(targetDayOfMonth, daysInNextMonth, policy)
            if (resolvedDay != null) {
                val nextEpoch = JalaliDate(nextYear, nextMonth, resolvedDay)
                    .toEpochMillis(reminder.hour, reminder.minute, 0, zoneId)
                if (nextEpoch > referenceEpochMillis) {
                    return nextEpoch
                }
            }
            monthsToAdd += interval
        }
    }

    private fun resolveDayForMonth(
        targetDay: Int,
        maxDaysInMonth: Int,
        policy: ShortMonthPolicy
    ): Int? {
        return if (targetDay <= maxDaysInMonth) {
            targetDay
        } else {
            when (policy) {
                ShortMonthPolicy.SNAP_TO_LAST_DAY -> maxDaysInMonth
                ShortMonthPolicy.SKIP_MONTH -> null
            }
        }
    }

    /**
     * Calculates the trigger times for pre-reminders (e.g. 15 minutes before, 1 day before)
     */
    fun calculatePreReminderTriggers(
        preReminders: List<PreReminder>,
        mainTriggerEpochMillis: Long
    ): List<Long> {
        return preReminders.mapNotNull { pre ->
            val offsetMillis = pre.toMinutes() * 60 * 1000L
            val trigger = mainTriggerEpochMillis - offsetMillis
            if (trigger > 0) trigger else null
        }.sorted()
    }
}
