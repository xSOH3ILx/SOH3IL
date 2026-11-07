// Ported from
// https://github.com/roozbehp/persiancalendar/blob/daf8fb2b46466a324cee98833c19c36aa5d97f39/persiancalendar.py
// Which is released under Apache 2.0 license
package com.soh3il.core.jalali.engine


import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

// Fixed date of start of the Persian calendar.
private val PERSIAN_EPOCH = fixedFromJulian(622, 3, 19)

// Location of Tehran, Iran.
// Specifically location of "Dar ul-Funun", https://w.wiki/DjPM
private val TEHRAN = doubleArrayOf(35.683789, 51.421864, 1100.0, +3.5)

// Middle of Iran.
private val IRAN = doubleArrayOf(35.5, 52.5, 0.0, +3.5)

/** Fixed date of Astronomical Persian New Year on or before fixed date. */
internal fun persianNewYearOnOrBefore(date: Int, longitude: Double): Int {
    // Approximate time of equinox.
    val approx = estimatePriorSolarLongitude(SPRING, midday(date, longitude))
    var day = floor(approx).toInt() - 1
    while (solarLongitude(midday(day, longitude)) > SPRING + 2) day += 1
    return day
}

/** Fixed date of Borji Persian new month on or before fixed date. */
internal fun persianBorjiNewMonthOnOrBefore(date: Int, month: Int, longitude: Double): Int {
    // Approximate time of equinox.
    val targetLong = (month - 1) * 30.0
    val approx = estimatePriorSolarLongitude(targetLong, midday(date, longitude))
    var day = floor(approx).toInt() - 1
    while (true) {
        val solarLong = solarLongitude(midday(day, longitude))
        if ((targetLong + 2 > solarLong && solarLong >= targetLong)) break
        day += 1
    }
    return day
}

/** Fixed date of Astronomical Persian date p_date. */
internal fun fixedFromPersian(year: Int, month: Int, day: Int, longitude: Double): Int {
    val newYear = persianNewYearOnOrBefore(
        PERSIAN_EPOCH + 180  // Fall after epoch.
                + floor(
            MEAN_TROPICAL_YEAR *
                    (if (0 < year) year - 1 else year)
        ).toInt(),
        longitude
    )  // No year zero.
    return (newYear - 1  // Days in prior years.
            // Days in prior months this year.
            + (if (month <= 7) 31 * (month - 1) else 30 * (month - 1) + 6)
            + day)  // Days so far this month.
}

/** Fixed date of Borji Persian date p_date. */
internal fun fixedFromPersianBorji(year: Int, month: Int, day: Int, longitude: Double): Int {
    val newMonth = persianBorjiNewMonthOnOrBefore(
        PERSIAN_EPOCH + 180
                + floor(
            MEAN_TROPICAL_YEAR *
                    ((if (0 < year) year - 1 else year) + (month - 1) / 12.0)
        ).toInt(),
        month,
        longitude
    )
    return (newMonth - 1  // Days in prior months.
            + day)  // Days so far this month.
}

/** Astronomical Persian date corresponding to fixed date. */
internal fun persianFromFixed(date: Int, longitude: Double): DateTriplet {
    val newYear = persianNewYearOnOrBefore(date, longitude)
    val y = round((newYear - PERSIAN_EPOCH) / MEAN_TROPICAL_YEAR).toInt() + 1
    val year = if (0 < y) y else y - 1  // No year zero
    val dayOfYear = date - fixedFromPersian(year, 1, 1, longitude) + 1
    val month =
        if (dayOfYear <= 186) ceil(dayOfYear / 31.0).toInt()
        else ceil((dayOfYear - 6) / 30.0).toInt()
    // Calculate the day by subtraction
    val day = date - fixedFromPersian(year, month, 1, longitude) + 1
    return DateTriplet(year = year, month = month, dayOfMonth = day)
}

/** Borji Persian date corresponding to fixed date. */
internal fun persianBorjiFromFixed(date: Int, longitude: Double): DateTriplet {
    val newYear = persianNewYearOnOrBefore(date, longitude)
    val y = round((newYear - PERSIAN_EPOCH) / MEAN_TROPICAL_YEAR).toInt() + 1
    val year = if (0 < y) y else y - 1  // No year zero
    var month = 1
    while (month < 12 && date >= fixedFromPersianBorji(year, month + 1, 1, longitude)) {
        month += 1
    }
    // Calculate the day by subtraction
    val day = date - fixedFromPersianBorji(year, month, 1, longitude) + 1
    return DateTriplet(year = year, month = month, dayOfMonth = day)
}

/** Fixed date of Persian New Year (Nowruz) in Gregorian year g_year. */
private fun nowruz(gYear: Int, longitude: Double): Int {
    val persianYear = gYear - gregorianYearFromFixed(PERSIAN_EPOCH) + 1
    val y = if (persianYear <= 0) persianYear - 1 else persianYear  // No Persian year 0
    return fixedFromPersian(y, 1, 1, longitude)
}

/** True if year is a leap year on the Persian calendar. */
internal fun persianLeapYear(year: Int, longitude: Double): Boolean {
    val thisNowruz = fixedFromPersian(year, 1, 1, longitude)
    val nextNowruz = fixedFromPersian(year + 1, 1, 1, longitude)
    return nextNowruz - thisNowruz == 366
}

private const val OFFSET_JDN = 1_721_425L
private const val START_OF_MODERN_ERA_JDN = 2424231 // PersianDate(1304, 1, 1).toJdn()
private const val START_OF_MODERN_ERA_YEAR = 1304

internal fun persianFromJdn(jdn: Long, alwaysBorji: Boolean = false): DateTriplet {
    val isModernEra = jdn >= START_OF_MODERN_ERA_JDN
    val fixed = (jdn - OFFSET_JDN).toInt()
    val longitude = (if (isModernEra) IRAN else TEHRAN)[1]
    return if (isModernEra && !alwaysBorji) persianFromFixed(fixed, longitude)
    else persianBorjiFromFixed(fixed, longitude)
}

internal fun jdnFromPersian(year: Int, month: Int, dayOfMonth: Int): Long {
    val isModernEra = year >= START_OF_MODERN_ERA_YEAR
    val longitude = (if (isModernEra) IRAN else TEHRAN)[1]
    val fixed = if (isModernEra) fixedFromPersian(year, month, dayOfMonth, longitude)
    else fixedFromPersianBorji(year, month, dayOfMonth, longitude)
    return fixed.toLong() + OFFSET_JDN
}

// Expose this so the app can display Julian date for now
// but it's unstable and maybe we would decide to expose it differently
fun julianFromJdn(jdn: Long): DateTriplet = julianFromFixed((jdn - OFFSET_JDN).toInt())

internal fun jdnFromCivil(year: Int, month: Int, dayOfMonth: Int): Long {
    return OFFSET_JDN + if (
        (year > 1582)
        || ((year == 1582) && (month > 10))
        || ((year == 1582) && (month == 10) && (dayOfMonth > 14))
    ) fixedFromGregorian(year, month, dayOfMonth) else fixedFromJulian(year, month, dayOfMonth)
}

internal fun civilFromJdn(jdn: Long): DateTriplet =
    if (jdn > 2299160) gregorianFromFixed((jdn - OFFSET_JDN).toInt()) else julianFromJdn(jdn)
