package com.soh3il.core.jalali

import com.soh3il.core.jalali.engine.LookupTableConverter
import com.soh3il.core.jalali.engine.jdnFromPersian
import com.soh3il.core.jalali.engine.persianFromJdn

/**
 * Single entry point for Jalali calendar calculations.
 *
 * The official modern-era lookup table remains the source of truth for the
 * supported Iranian calendar range. The astronomical implementation is used
 * only outside that published range, behind this one engine so callers cannot
 * accidentally mix algorithms.
 */
internal object JalaliCalendarEngine {

    private const val MODERN_FIRST_YEAR = 1304
    private const val MODERN_LAST_YEAR = 1498

    fun isLeap(year: Int): Boolean =
        if (year in MODERN_FIRST_YEAR..MODERN_LAST_YEAR) {
            LookupTableConverter.isLeap(year)
        } else {
            com.soh3il.core.jalali.engine.persianLeapYear(year, 52.5)
        }

    fun daysInMonth(year: Int, month: Int): Int = when (month) {
        in 1..6 -> 31
        in 7..11 -> 30
        12 -> if (isLeap(year)) 30 else 29
        else -> throw IllegalArgumentException("ماه نامعتبر: $month")
    }

    fun toJdn(year: Int, month: Int, day: Int): Long =
        LookupTableConverter.toJdn(year, month, day).takeIf { it >= 0 }
            ?: jdnFromPersian(year, month, day)

    fun fromJdn(jdn: Long): Triple<Int, Int, Int> {
        val date = LookupTableConverter.fromJdn(jdn) ?: persianFromJdn(jdn)
        return Triple(date.year, date.month, date.dayOfMonth)
    }
}
