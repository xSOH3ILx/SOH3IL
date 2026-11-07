package com.soh3il.core.jalali

import com.soh3il.core.jalali.engine.civilFromJdn
import com.soh3il.core.jalali.engine.jdnFromCivil
import com.soh3il.core.jalali.engine.jdnFromPersian
import com.soh3il.core.jalali.engine.persianFromJdn
import com.soh3il.core.jalali.engine.persianLeapYear
import com.soh3il.core.model.ShortMonthPolicy
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

data class JalaliDate(
    val year: Int,
    val month: Int,
    val day: Int
) : Comparable<JalaliDate> {

    init {
        require(month in 1..12) { "ماه باید بین ۱ تا ۱۲ باشد (مقدار دریافتی: $month)" }
        val maxDays = getDaysInMonth(year, month)
        require(day in 1..maxDays) { "روز ماه $month در سال $year باید بین ۱ تا $maxDays باشد (مقدار دریافتی: $day)" }
    }

    val isLeapYear: Boolean get() = isLeap(year)

    val daysInMonth: Int get() = getDaysInMonth(year, month)

    /**
     * Day of week: 1=Shanbeh, 2=Yekshanbeh, ..., 6=Panjshanbeh, 7=Jomeh
     */
    val dayOfWeek: Int
        get() {
            // Astronomical Julian Day Number to Persian week day (1=Shanbeh ... 7=Jomeh)
            return ((toJdn() + 2) % 7).toInt() + 1
        }

    val dayOfWeekNameFa: String
        get() = when (dayOfWeek) {
            1 -> "شنبه"
            2 -> "یکشنبه"
            3 -> "دوشنبه"
            4 -> "سه‌شنبه"
            5 -> "چهارشنبه"
            6 -> "پنج‌شنبه"
            7 -> "جمعه"
            else -> ""
        }

    val monthNameFa: String
        get() = when (month) {
            1 -> "فروردین"
            2 -> "اردیبهشت"
            3 -> "خرداد"
            4 -> "تیر"
            5 -> "مرداد"
            6 -> "شهریور"
            7 -> "مهر"
            8 -> "آبان"
            9 -> "آذر"
            10 -> "دی"
            11 -> "بهمن"
            12 -> "اسفند"
            else -> ""
        }

    fun toJdn(): Long {
        val lookup = com.soh3il.core.jalali.engine.LookupTableConverter.toJdn(year, month, day)
        return if (lookup != -1L) lookup else jdnFromPersian(year, month, day)
    }

    fun toEpochMillis(
        hour: Int = 0,
        minute: Int = 0,
        second: Int = 0,
        zoneId: ZoneId = TEHRAN_ZONE
    ): Long {
        val jdn = toJdn()
        val (gy, gm, gd) = civilFromJdn(jdn)
        val zdt = ZonedDateTime.of(gy, gm, gd, hour, minute, second, 0, zoneId)
        return zdt.toInstant().toEpochMilli()
    }

    fun plusDays(days: Long): JalaliDate {
        val newJdn = toJdn() + days
        return fromJdn(newJdn)
    }

    fun plusMonths(months: Int, policy: ShortMonthPolicy = ShortMonthPolicy.SNAP_TO_LAST_DAY): JalaliDate {
        val totalMonths = (year * 12 + (month - 1)) + months
        val targetYear = totalMonths / 12
        val targetMonth = (totalMonths % 12) + 1
        val maxDays = getDaysInMonth(targetYear, targetMonth)

        val targetDay = if (day > maxDays) {
            when (policy) {
                ShortMonthPolicy.SNAP_TO_LAST_DAY -> maxDays
                ShortMonthPolicy.SKIP_MONTH -> return plusMonths(months + 1, policy)
            }
        } else {
            day
        }

        return JalaliDate(targetYear, targetMonth, targetDay)
    }

    fun plusYears(years: Int, policy: ShortMonthPolicy = ShortMonthPolicy.SNAP_TO_LAST_DAY): JalaliDate {
        val targetYear = year + years
        val maxDays = getDaysInMonth(targetYear, month)
        val targetDay = if (day > maxDays) {
            when (policy) {
                ShortMonthPolicy.SNAP_TO_LAST_DAY -> maxDays
                ShortMonthPolicy.SKIP_MONTH -> maxDays
            }
        } else {
            day
        }
        return JalaliDate(targetYear, month, targetDay)
    }

    fun format(pattern: String = "YYYY/MM/DD"): String {
        val y = year.toString().padStart(4, '0')
        val m = month.toString().padStart(2, '0')
        val d = day.toString().padStart(2, '0')
        return pattern
            .replace("YYYY", y)
            .replace("MM", m)
            .replace("DD", d)
    }

    override fun compareTo(other: JalaliDate): Int {
        if (year != other.year) return year.compareTo(other.year)
        if (month != other.month) return month.compareTo(other.month)
        return day.compareTo(other.day)
    }

    companion object {
        val TEHRAN_ZONE: ZoneId = ZoneId.of("Asia/Tehran")

        fun isLeap(year: Int): Boolean {
            if (year in 1304..1497) {
                return com.soh3il.core.jalali.engine.LookupTableConverter.isLeap(year)
            }
            return persianLeapYear(year, 52.5)
        }

        fun getDaysInMonth(year: Int, month: Int): Int = when (month) {
            in 1..6 -> 31
            in 7..11 -> 30
            12 -> if (isLeap(year)) 30 else 29
            else -> throw IllegalArgumentException("ماه نامعتبر: $month")
        }

        fun fromJdn(jdn: Long): JalaliDate {
            val (y, m, d) = com.soh3il.core.jalali.engine.LookupTableConverter.fromJdn(jdn)
                ?: persianFromJdn(jdn)
            return JalaliDate(y, m, d)
        }

        fun fromEpochMillis(epochMillis: Long, zoneId: ZoneId = TEHRAN_ZONE): JalaliDate {
            val zdt = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
            val jdn = jdnFromCivil(zdt.year, zdt.monthValue, zdt.dayOfMonth)
            return fromJdn(jdn)
        }

        fun now(zoneId: ZoneId = TEHRAN_ZONE): JalaliDate {
            return fromEpochMillis(System.currentTimeMillis(), zoneId)
        }
    }
}
