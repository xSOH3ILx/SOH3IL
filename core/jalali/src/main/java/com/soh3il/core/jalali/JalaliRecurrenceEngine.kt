package com.soh3il.core.jalali

import com.soh3il.core.model.PreReminder
import com.soh3il.core.model.RecurrenceRule
import com.soh3il.core.model.RecurrenceType
import com.soh3il.core.model.Reminder
import com.soh3il.core.model.ShortMonthPolicy
import java.time.Instant
import java.time.ZoneId

object JalaliRecurrenceEngine {

    /**
     * Calculates the exact next epoch millis when this reminder should fire,
     * strictly adhering to the Solar Hijri (Jalali) calendar rules without any drift,
     * fully supporting customizable recurrence intervals (every X days, weeks, months, years).
     */
    fun calculateNextTrigger(
        reminder: Reminder,
        referenceEpochMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = JalaliDate.TEHRAN_ZONE
    ): Long? {
        if (!reminder.isEnabled) return null

        val todayJalali = JalaliDate.fromEpochMillis(referenceEpochMillis, zoneId)
        val baseDate = JalaliDate(reminder.jalaliYear, reminder.jalaliMonth, reminder.jalaliDay)

        return when (reminder.recurrence.type) {
            RecurrenceType.NONE -> {
                val targetEpoch = baseDate.toEpochMillis(reminder.hour, reminder.minute, 0, zoneId)
                if (targetEpoch > referenceEpochMillis) targetEpoch else null
            }

            RecurrenceType.DAILY -> {
                val interval = reminder.recurrence.interval.coerceAtLeast(1)
                val daysDiff = todayJalali.toJdn() - baseDate.toJdn()

                if (daysDiff >= 0 && daysDiff % interval == 0L) {
                    val candidateEpoch = todayJalali.toEpochMillis(reminder.hour, reminder.minute, 0, zoneId)
                    if (candidateEpoch > referenceEpochMillis) {
                        return candidateEpoch
                    }
                }

                val step = if (daysDiff >= 0) {
                    val rem = (daysDiff % interval).toInt()
                    if (rem == 0) interval else interval - rem
                } else {
                    (-daysDiff).toInt()
                }

                val nextDate = todayJalali.plusDays(step.toLong())
                nextDate.toEpochMillis(reminder.hour, reminder.minute, 0, zoneId)
            }

            RecurrenceType.WEEKLY -> {
                val interval = reminder.recurrence.interval.coerceAtLeast(1)
                val targetDaysOfWeek = if (reminder.recurrence.daysOfWeek.isNotEmpty()) {
                    reminder.recurrence.daysOfWeek
                } else {
                    setOf(baseDate.dayOfWeek)
                }

                // Search up to (10 * interval) weeks ahead
                val maxDaysToSearch = (7 * interval * 10).coerceAtLeast(60)
                for (dayOffset in 0..maxDaysToSearch) {
                    val dateToCheck = todayJalali.plusDays(dayOffset.toLong())
                    if (dateToCheck.toJdn() < baseDate.toJdn()) continue
                    if (dateToCheck.dayOfWeek in targetDaysOfWeek) {
                        val daysFromBase = dateToCheck.toJdn() - baseDate.toJdn()
                        val weeksFromBase = (daysFromBase / 7).toInt()
                        if (interval == 1 || (weeksFromBase >= 0 && weeksFromBase % interval == 0)) {
                            val epoch = dateToCheck.toEpochMillis(reminder.hour, reminder.minute, 0, zoneId)
                            if (epoch > referenceEpochMillis) {
                                return epoch
                            }
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
                calculateNextYearlyJalaliTrigger(
                    reminder = reminder,
                    referenceEpochMillis = referenceEpochMillis,
                    zoneId = zoneId
                )
            }
            RecurrenceType.MONTHLY_GREGORIAN, RecurrenceType.YEARLY_GREGORIAN ->
                GregorianRecurrenceEngine.calculateNextTrigger(reminder, referenceEpochMillis, zoneId)
        }
    }

    /**
     * Computes the exact monthly trigger in Solar Hijri calendar.
     * Supports intervals (e.g. every 2 months, every 3 months).
     */
    private fun calculateNextMonthlyJalaliTrigger(
        reminder: Reminder,
        referenceEpochMillis: Long,
        zoneId: ZoneId
    ): Long {
        val todayJalali = JalaliDate.fromEpochMillis(referenceEpochMillis, zoneId)
        val baseDate = JalaliDate(reminder.jalaliYear, reminder.jalaliMonth, reminder.jalaliDay)
        val targetDayOfMonth = reminder.recurrence.dayOfMonth ?: reminder.jalaliDay
        val interval = reminder.recurrence.interval.coerceAtLeast(1)
        val policy = reminder.recurrence.shortMonthPolicy

        val currentTotalMonths = todayJalali.year * 12 + (todayJalali.month - 1)
        val baseTotalMonths = baseDate.year * 12 + (baseDate.month - 1)
        val diffMonths = currentTotalMonths - baseTotalMonths

        // 1. Check current month if aligned
        if (diffMonths >= 0 && diffMonths % interval == 0) {
            val daysInCurrentMonth = todayJalali.daysInMonth
            val candidateDay = resolveDayForMonth(targetDayOfMonth, daysInCurrentMonth, policy)
            if (candidateDay != null) {
                val candidateEpoch = JalaliDate(todayJalali.year, todayJalali.month, candidateDay)
                    .toEpochMillis(reminder.hour, reminder.minute, 0, zoneId)
                if (candidateEpoch > referenceEpochMillis) {
                    return candidateEpoch
                }
            }
        }

        // 2. Next aligned month step
        var step = if (diffMonths >= 0) {
            val rem = diffMonths % interval
            if (rem == 0) interval else interval - rem
        } else {
            -diffMonths
        }

        while (true) {
            val nextTotalMonths = currentTotalMonths + step
            val nextYear = nextTotalMonths / 12
            val nextMonth = (nextTotalMonths % 12) + 1
            val daysInNextMonth = JalaliDate.daysInMonth(nextYear, nextMonth)

            val resolvedDay = resolveDayForMonth(targetDayOfMonth, daysInNextMonth, policy)
            if (resolvedDay != null) {
                val nextEpoch = JalaliDate(nextYear, nextMonth, resolvedDay)
                    .toEpochMillis(reminder.hour, reminder.minute, 0, zoneId)
                if (nextEpoch > referenceEpochMillis) {
                    return nextEpoch
                }
            }
            step += interval
        }
    }

    private fun calculateNextYearlyJalaliTrigger(
        reminder: Reminder,
        referenceEpochMillis: Long,
        zoneId: ZoneId
    ): Long {
        val todayJalali = JalaliDate.fromEpochMillis(referenceEpochMillis, zoneId)
        val baseDate = JalaliDate(reminder.jalaliYear, reminder.jalaliMonth, reminder.jalaliDay)
        val interval = reminder.recurrence.interval.coerceAtLeast(1)
        val targetMonth = reminder.jalaliMonth
        val anchorDay = reminder.recurrence.dayOfMonth ?: reminder.jalaliDay
        val targetYear = todayJalali.year

        val yearDiff = targetYear - baseDate.year
        if (yearDiff >= 0 && yearDiff % interval == 0) {
            val maxDaysThisYear = JalaliDate.daysInMonth(targetYear, targetMonth)
            val effectiveDay = resolveDayForMonth(anchorDay, maxDaysThisYear, reminder.recurrence.shortMonthPolicy)
            if (effectiveDay != null) {
                val thisYearEpoch = JalaliDate(targetYear, targetMonth, effectiveDay)
                    .toEpochMillis(reminder.hour, reminder.minute, 0, zoneId)
                if (thisYearEpoch > referenceEpochMillis) {
                    return thisYearEpoch
                }
            }
        }

        val step = if (yearDiff >= 0) {
            val rem = yearDiff % interval
            if (rem == 0) interval else interval - rem
        } else {
            -yearDiff
        }

        var nextYear = targetYear + step
        while (true) {
            val maxDaysNextYear = JalaliDate.daysInMonth(nextYear, targetMonth)
            val effectiveDay = resolveDayForMonth(anchorDay, maxDaysNextYear, reminder.recurrence.shortMonthPolicy)
            if (effectiveDay != null) {
                val nextEpoch = JalaliDate(nextYear, targetMonth, effectiveDay)
                    .toEpochMillis(reminder.hour, reminder.minute, 0, zoneId)
                if (nextEpoch > referenceEpochMillis) {
                    return nextEpoch
                }
            }
            nextYear += interval
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

    /**
     * Determines whether a reminder should occur on a given Jalali date,
     * taking into account its recurrence rules, start date, interval, and solar calendar rules.
     */
    fun matchesDate(reminder: Reminder, date: JalaliDate): Boolean {
        if (!reminder.isEnabled) return false

        val baseDate = JalaliDate(reminder.jalaliYear, reminder.jalaliMonth, reminder.jalaliDay)
        if (date.toJdn() < baseDate.toJdn()) return false

        return when (reminder.recurrence.type) {
            RecurrenceType.NONE -> {
                date == baseDate
            }

            RecurrenceType.DAILY -> {
                val interval = reminder.recurrence.interval.coerceAtLeast(1)
                val daysDiff = date.toJdn() - baseDate.toJdn()
                daysDiff >= 0 && (daysDiff % interval == 0L)
            }

            RecurrenceType.WEEKLY -> {
                val interval = reminder.recurrence.interval.coerceAtLeast(1)
                val targetDaysOfWeek = if (reminder.recurrence.daysOfWeek.isNotEmpty()) {
                    reminder.recurrence.daysOfWeek
                } else {
                    setOf(baseDate.dayOfWeek)
                }
                if (date.dayOfWeek !in targetDaysOfWeek) return false

                val daysDiff = date.toJdn() - baseDate.toJdn()
                val weeksDiff = daysDiff / 7
                weeksDiff >= 0 && (interval == 1 || weeksDiff % interval == 0L)
            }

            RecurrenceType.MONTHLY_JALALI -> {
                val interval = reminder.recurrence.interval.coerceAtLeast(1)
                val monthsDiff = (date.year - baseDate.year) * 12 + (date.month - baseDate.month)
                if (monthsDiff < 0 || monthsDiff % interval != 0) return false

                val anchorDay = reminder.recurrence.dayOfMonth ?: baseDate.day
                val maxDaysInMonth = JalaliDate.daysInMonth(date.year, date.month)
                val effectiveDay = resolveDayForMonth(anchorDay, maxDaysInMonth, reminder.recurrence.shortMonthPolicy)
                effectiveDay != null && date.day == effectiveDay
            }

            RecurrenceType.YEARLY_JALALI -> {
                val interval = reminder.recurrence.interval.coerceAtLeast(1)
                val yearsDiff = date.year - baseDate.year
                if (yearsDiff < 0 || yearsDiff % interval != 0) return false
                if (date.month != baseDate.month) return false

                val anchorDay = reminder.recurrence.dayOfMonth ?: baseDate.day
                val maxDaysInMonth = JalaliDate.daysInMonth(date.year, date.month)
                val effectiveDay = resolveDayForMonth(anchorDay, maxDaysInMonth, reminder.recurrence.shortMonthPolicy)
                effectiveDay != null && date.day == effectiveDay
            }
            RecurrenceType.MONTHLY_GREGORIAN, RecurrenceType.YEARLY_GREGORIAN ->
                GregorianRecurrenceEngine.matchesDate(reminder, date)
        }
    }
}

/**
 * Extension helper to check if a reminder occurs on a specific Jalali date
 */
fun Reminder.matchesDate(date: JalaliDate): Boolean = JalaliRecurrenceEngine.matchesDate(this, date)

/**
 * Calendar visibility. Completed one-off reminders stay on their own date so they can be reopened.
 * Disabled recurring reminders stay hidden; they are not a single completed occurrence.
 */
fun Reminder.visibleOn(date: JalaliDate): Boolean {
    if (isEnabled) return matchesDate(date)
    if (recurrence.type != com.soh3il.core.model.RecurrenceType.NONE) return false
    return jalaliYear == date.year && jalaliMonth == date.month && jalaliDay == date.day
}
