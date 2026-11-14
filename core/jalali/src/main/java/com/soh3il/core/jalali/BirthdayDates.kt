package com.soh3il.core.jalali

import java.time.LocalDate
import java.time.MonthDay

/** Projects standard Gregorian contact birthdays into a complete solar-year interval. */
object BirthdayDates {
    private val DATE_PATTERN = Regex("(?:(\\d{4})[-/]|--)?(\\d{1,2})[-/](\\d{1,2})")

    fun inSolarYear(rawDate: String, solarYear: Int): List<JalaliDate> {
        val normalized = rawDate.trim().map { ch ->
            when (ch) {
                in '۰'..'۹' -> '0' + (ch - '۰')
                in '٠'..'٩' -> '0' + (ch - '٠')
                else -> ch
            }
        }.joinToString("")
        val match = DATE_PATTERN.matchEntire(normalized) ?: return emptyList()
        val month = match.groupValues[2].toInt()
        val day = match.groupValues[3].toInt()
        val monthDay = runCatching { MonthDay.of(month, day) }.getOrNull() ?: return emptyList()
        val originalYear = match.groupValues[1].toIntOrNull()
        if (originalYear != null && runCatching { LocalDate.of(originalYear, month, day) }.isFailure) return emptyList()

        val start = DateConverter.fromJalali(solarYear, 1, 1).gregorian
        val end = DateConverter.fromJalali(solarYear, 12, JalaliDate.daysInMonth(solarYear, 12)).gregorian.plusDays(1)
        return (start.year..end.year).mapNotNull { year ->
            if (!monthDay.isValidYear(year)) return@mapNotNull null
            val date = monthDay.atYear(year)
            if (date < start || date >= end) return@mapNotNull null
            DateConverter.fromGregorian(date.year, date.monthValue, date.dayOfMonth).jalali
        }
    }
}
