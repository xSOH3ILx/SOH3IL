package com.soh3il.core.jalali

import com.soh3il.core.model.RecurrenceType
import com.soh3il.core.model.Reminder
import com.soh3il.core.model.ShortMonthPolicy
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

internal object GregorianRecurrenceEngine {
    private fun baseDate(reminder: Reminder): LocalDate = DateConverter.fromJalali(
        reminder.jalaliYear, reminder.jalaliMonth, reminder.jalaliDay
    ).gregorian

    private fun distance(reminder: Reminder, base: LocalDate, date: LocalDate): Long =
        if (reminder.recurrence.type == RecurrenceType.YEARLY_GREGORIAN) {
            (date.year - base.year).toLong()
        } else {
            ChronoUnit.MONTHS.between(YearMonth.from(base), YearMonth.from(date))
        }

    private fun occurrence(reminder: Reminder, base: LocalDate, step: Long): LocalDate? {
        val month = if (reminder.recurrence.type == RecurrenceType.YEARLY_GREGORIAN) {
            YearMonth.from(base).plusYears(step)
        } else {
            YearMonth.from(base).plusMonths(step)
        }
        val anchor = reminder.recurrence.dayOfMonth ?: base.dayOfMonth
        require(anchor in 1..31) { "روز تکرار میلادی باید بین ۱ و ۳۱ باشد" }
        if (anchor > month.lengthOfMonth() && reminder.recurrence.shortMonthPolicy == ShortMonthPolicy.SKIP_MONTH) return null
        return month.atDay(minOf(anchor, month.lengthOfMonth()))
    }

    fun calculateNextTrigger(reminder: Reminder, reference: Long, zoneId: ZoneId): Long {
        val base = baseDate(reminder)
        val today = Instant.ofEpochMilli(reference).atZone(zoneId).toLocalDate()
        val interval = reminder.recurrence.interval.coerceAtLeast(1).toLong()
        val difference = distance(reminder, base, today).coerceAtLeast(0)
        var step = difference - difference % interval
        // Gregorian month lengths repeat within 4,800 months, including interval phases.
        repeat(4_801) {
            val date = occurrence(reminder, base, step)
            if (date != null) {
                val epoch = date.atTime(reminder.hour, reminder.minute).atZone(zoneId).toInstant().toEpochMilli()
                if (epoch > reference) return epoch
            }
            step += interval
        }
        throw IllegalArgumentException("قاعدهٔ تکرار میلادی هیچ تاریخ معتبری ندارد")
    }

    fun matchesDate(reminder: Reminder, date: JalaliDate): Boolean {
        val base = baseDate(reminder)
        val gregorian = DateConverter.fromJalali(date.year, date.month, date.day).gregorian
        val difference = distance(reminder, base, gregorian)
        val interval = reminder.recurrence.interval.coerceAtLeast(1)
        return difference >= 0 && difference % interval == 0L && occurrence(reminder, base, difference) == gregorian
    }
}
