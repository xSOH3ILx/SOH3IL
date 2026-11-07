// Ported from
// https://github.com/roozbehp/persiancalendar/blob/daf8fb2b46466a324cee98833c19c36aa5d97f39/persiancalendar.py
// Which is released under Apache 2.0 license
package com.soh3il.core.jalali.engine


import kotlin.math.floor

/**
 * Fixed date of start of the Julian calendar.
 */
private val JULIAN_EPOCH = fixedFromGregorian(0, 12, 30)

/**
 * True if j_year is a leap year on the Julian calendar.
 */
private fun julianLeapYear(jYear: Int): Boolean = (jYear % 4) == (if (jYear > 0) 0 else 3)

/**
 * True if jYear is a leap year on the Julian calendar.
 */
private fun isJulianLeapYear(jYear: Int): Boolean = jYear % 4 == (if (jYear > 0) 0 else 3)

/**
 * Fixed date equivalent to the Julian date.
 */
internal fun fixedFromJulian(year: Int, month: Int, day: Int): Int {
    val y = if (year < 0) year + 1 else year  // No year zero
    return (JULIAN_EPOCH - 1  // Days before start of calendar
            + 365 * (y - 1)  // Ordinary days since epoch.
            + (y - 1).floorDiv(4)   // Leap days since epoch...
            // Days in prior months this year...
            + ((367 * month - 362) / 12)  // ...assuming 30-day Feb
            // Correct for 28- or 29-day Feb
            + (if (month <= 2) 0 else if (julianLeapYear(year)) -1 else -2)
            + day)           // Days so far this month.
}

/**
 * Julian (year month day) corresponding to fixed $date$.
 */
internal fun julianFromFixed(date: Int): DateTriplet {
    // Nominal year.
    val approx = floor((4 * (date - JULIAN_EPOCH) + 1464) / 1461.0).toInt()
    val year = if (approx <= 0) approx - 1 else approx  // No year 0.

    // This year
    val priorDays = date - fixedFromJulian(year, 1, 1)

    // To simulate a 30-day Feb
    val correction = if (date < fixedFromJulian(year, 3, 1)) 0 else {
        if (isJulianLeapYear(year)) 1 else 2
    }

    // Assuming a 30-day Feb
    val month = floor((12 * (priorDays + correction) + 373) / 367.0).toInt()

    // Calculate the day by subtraction.
    val day = 1 + (date - fixedFromJulian(year, month, 1))

    return DateTriplet(year = year, month = month, dayOfMonth = day)
}
