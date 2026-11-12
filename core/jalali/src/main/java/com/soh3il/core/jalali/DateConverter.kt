package com.soh3il.core.jalali

import com.soh3il.core.jalali.engine.civilFromJdn
import com.soh3il.core.jalali.engine.jdnFromCivil
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

/**
 * مبدل جامع و سه‌گانه تقویم‌های خورشیدی (جلالی)، میلادی (گرگوری) و قمری (هجری قمری)
 * به همراه تحلیل نجومی و گاه‌شماری (حیوان سال، کبیسه، روز هفته، اختلاف با امروز).
 */
object DateConverter {

    private val YEAR_ANIMALS = listOf(
        "مار", "اسب", "گوسفند", "میمون", "مرغ", "سگ", "خوک", "موش", "گاو", "پلنگ", "خرگوش", "نهنگ"
    )

    data class ConversionResult(
        val jalali: JalaliDate,
        val gregorian: LocalDate,
        val hijriYear: Int,
        val hijriMonth: Int,
        val hijriDay: Int,
        val dayOfWeekNameFa: String,
        val dayOfWeekNameEn: String,
        val isJalaliLeap: Boolean,
        val yearAnimal: String,
        val diffDaysFromToday: Long,
        val relativeDescriptionFa: String
    )

    val GREGORIAN_MONTHS_EN = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    val GREGORIAN_MONTHS_FA = listOf(
        "ژانویه", "فوریه", "مارس", "آوریل", "مه", "ژوئن",
        "ژوئیه", "اوت", "سپتامبر", "اکتبر", "نوامبر", "دسامبر"
    )

    val HIJRI_MONTHS_FA = listOf(
        "محرم", "صفر", "ربیع‌الاول", "ربیع‌الثانی",
        "جمادی‌الاول", "جمادی‌الثانی", "رجب", "شعبان",
        "رمضان", "شوال", "ذی‌القعده", "ذی‌الحجه"
    )

    fun getGregorianFormatted(jalaliDate: JalaliDate): String {
        val conv = fromJalali(jalaliDate.year, jalaliDate.month, jalaliDate.day)
        val monthName = GREGORIAN_MONTHS_EN.getOrElse(conv.gregorian.monthValue - 1) { "" }
        return "${conv.gregorian.year} $monthName ${conv.gregorian.dayOfMonth}"
    }

    fun getGregorianFormattedFa(jalaliDate: JalaliDate): String {
        val conv = fromJalali(jalaliDate.year, jalaliDate.month, jalaliDate.day)
        val monthName = GREGORIAN_MONTHS_FA.getOrElse(conv.gregorian.monthValue - 1) { "" }
        return "${conv.gregorian.dayOfMonth} $monthName ${conv.gregorian.year}"
    }

    fun getHijriFormatted(jalaliDate: JalaliDate): String {
        val conv = fromJalali(jalaliDate.year, jalaliDate.month, jalaliDate.day)
        val monthName = HIJRI_MONTHS_FA.getOrElse(conv.hijriMonth - 1) { "" }
        return "${conv.hijriDay} $monthName ${conv.hijriYear}"
    }

    fun isLeapYear(year: Int): Boolean = JalaliDate.isLeapYear(year)

    fun daysInMonth(year: Int, month: Int): Int = JalaliDate.daysInMonth(year, month)

    /**
     * تبدیل از تاریخ شمسی
     */
    fun fromJalali(year: Int, month: Int, day: Int): ConversionResult {
        require(year in 1..9999) { "سال شمسی باید بین ۱ تا ۹۹۹۹ باشد (مقدار دریافتی: $year)" }
        require(month in 1..12) { "ماه شمسی باید بین ۱ تا ۱۲ باشد (مقدار دریافتی: $month)" }
        val maxDays = JalaliDate.daysInMonth(year, month)
        require(day in 1..maxDays) { "روز ماه $month در سال $year باید بین ۱ تا $maxDays باشد (مقدار دریافتی: $day)" }

        val jDate = JalaliDate(year, month, day)
        val jdn = jDate.toJdn()
        val (gy, gm, gd) = civilFromJdn(jdn)
        val gDate = LocalDate.of(gy, gm, gd)
        return buildResult(jDate, gDate)
    }

    /**
     * تبدیل از تاریخ میلادی
     */
    fun fromGregorian(year: Int, month: Int, day: Int): ConversionResult {
        require(year in 1..9999) { "سال میلادی باید بین ۱ تا ۹۹۹۹ باشد (مقدار دریافتی: $year)" }
        require(month in 1..12) { "ماه میلادی باید بین ۱ تا ۱۲ باشد (مقدار دریافتی: $month)" }
        val maxDays = runCatching { java.time.YearMonth.of(year, month).lengthOfMonth() }.getOrDefault(31)
        require(day in 1..maxDays) { "روز ماه میلادی $month در سال $year باید بین ۱ تا $maxDays باشد (مقدار دریافتی: $day)" }

        val gDate = LocalDate.of(year, month, day)
        val jdn = jdnFromCivil(year, month, day)
        val jDate = JalaliDate.fromJdn(jdn)
        return buildResult(jDate, gDate)
    }

    /**
     * تبدیل از تاریخ هجری قمری
     */
    fun fromHijri(year: Int, month: Int, day: Int): ConversionResult {
        require(year in 1..9999) { "سال قمری باید بین ۱ تا ۹۹۹۹ باشد (مقدار دریافتی: $year)" }
        require(month in 1..12) { "ماه قمری باید بین ۱ تا ۱۲ باشد (مقدار دریافتی: $month)" }
        require(day in 1..30) { "روز ماه قمری باید بین ۱ تا ۳۰ باشد (مقدار دریافتی: $day)" }

        val hDate = try {
            HijrahDate.of(year, month, day)
        } catch (e: Exception) {
            throw IllegalArgumentException("تاریخ هجری قمری نامعتبر است: $year/$month/$day", e)
        }
        val gDate = LocalDate.from(hDate)
        val jdn = jdnFromCivil(gDate.year, gDate.monthValue, gDate.dayOfMonth)
        val jDate = JalaliDate.fromJdn(jdn)
        return buildResult(jDate, gDate)
    }

    private fun buildResult(jDate: JalaliDate, gDate: LocalDate): ConversionResult {
        val hDate = runCatching { HijrahDate.from(gDate) }.getOrNull()
        val hYear = hDate?.get(ChronoField.YEAR) ?: 0
        val hMonth = hDate?.get(ChronoField.MONTH_OF_YEAR) ?: 0
        val hDay = hDate?.get(ChronoField.DAY_OF_MONTH) ?: 0

        val todayG = LocalDate.now(JalaliDate.TEHRAN_ZONE)
        val diffDays = java.time.temporal.ChronoUnit.DAYS.between(todayG, gDate)

        val relativeFa = when {
            diffDays == 0L -> "امروز"
            diffDays == 1L -> "فردا"
            diffDays == -1L -> "دیروز"
            diffDays > 0 -> "$diffDays روز دیگر"
            else -> "${-diffDays} روز پیش"
        }

        val animal = YEAR_ANIMALS[((jDate.year % 12) + 12) % 12]

        return ConversionResult(
            jalali = jDate,
            gregorian = gDate,
            hijriYear = hYear,
            hijriMonth = hMonth,
            hijriDay = hDay,
            dayOfWeekNameFa = jDate.dayOfWeekNameFa,
            dayOfWeekNameEn = gDate.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() },
            isJalaliLeap = jDate.isLeapYear,
            yearAnimal = animal,
            diffDaysFromToday = diffDays,
            relativeDescriptionFa = relativeFa
        )
    }
}
