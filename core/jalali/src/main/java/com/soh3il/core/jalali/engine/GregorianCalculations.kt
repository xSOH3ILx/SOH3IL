// Ported from
// https://github.com/roozbehp/persiancalendar/blob/daf8fb2b46466a324cee98833c19c36aa5d97f39/persiancalendar.py
// Which is released under Apache 2.0 license
package com.soh3il.core.jalali.engine


import kotlin.math.floor

/**
 * Fixed date equivalent to the Gregorian date g_date.
 */
internal fun fixedFromGregorian(year: Int, month: Int, day: Int): Int {
    return (GREGORIAN_EPOCH - 1  // Days before start of calendar
            + 365 * (year - 1)  // Ordinary days since epoch
            + (year - 1).floorDiv(4)   // Julian leap days since epoch...
            - (year - 1).floorDiv(100)  // ...minus century years since epoch...
            + (year - 1).floorDiv(400)  // plus years since epoch divisible by 400.
            // Days in prior months this year assuming 30-day Feb
            + (367 * month - 362) / 12
            // Correct for 28- or 29-day Feb
            + (if (month <= 2) 0 else if (gregorianLeapYear(year)) -1 else -2)
            + day)  // Days so far this month.
}

/**
 * True if g_year is a leap year on the Gregorian calendar.
 */
internal fun gregorianLeapYear(gYear: Int): Boolean {
    return gYear % 4 == 0 && when (gYear % 400) {
        100, 200, 300 -> false
        else -> true
    }
}

/**
 * Fixed date of start of the (proleptic) Gregorian calendar.
 */
internal val GREGORIAN_EPOCH = rd(1)

/**
 * Gregorian year corresponding to the fixed date.
 */
internal fun gregorianYearFromFixed(date: Int): Int {
    val d0 = date - GREGORIAN_EPOCH  // Prior days.
    val n400 = d0 / 146097  // Completed 400-year cycles.
    val d1 = d0 % 146097  // Prior days not in n400.
    val n100 = d1 / 36524  // 100-year cycles not in n400.
    val d2 = d1 % 36524  // Prior days not in n400 or n100.
    val n4 = d2 / 1461  // 4-year cycles not in n400 or n100.
    val d3 = d2 % 1461  // Prior days not in n400, n100, or n4.
    val n1 = d3 / 365  // Years not in n400, n100, or n4.
    val year = 400 * n400 + 100 * n100 + 4 * n4 + n1
    return if (n100 == 4 || n1 == 4) {
        year  // Date is day 366 in a leap year.
    } else {
        year + 1  // Date is ordinal day (d % 365 + 1) in (year + 1).
    }
}

/**
 * Fixed date of January 1 in g_year.
 */
internal fun gregorianNewYear(gYear: Int): Int = fixedFromGregorian(gYear, 1, 1)

/**
 * Gregorian (year, month, day) corresponding to fixed date.
 */
internal fun gregorianFromFixed(date: Int): DateTriplet {
    val year = gregorianYearFromFixed(date)
    val priorDays = date - gregorianNewYear(year)  // This year
    // To simulate a 30-day Feb
    val correction = when {
        date < fixedFromGregorian(year, 3, 1) -> 0
        gregorianLeapYear(year) -> 1
        else -> 2
    }
    val month = (12 * (priorDays + correction) + 373) / 367  // Assuming a 30-day Feb
    // Calculate the day by subtraction.
    val day = date - fixedFromGregorian(year, month, 1) + 1
    return DateTriplet(year = year, month = month, dayOfMonth = day)
}

/**
 * Number of days from Gregorian date g_date1 until g_date2.
 */
internal fun gregorianDateDifference(
    year1: Int, month1: Int, day1: Int,
    year2: Int, month2: Int, day2: Int,
): Int = fixedFromGregorian(year2, month2, day2) - fixedFromGregorian(year1, month1, day1)
