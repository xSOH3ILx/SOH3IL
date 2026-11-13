package com.soh3il.core.jalali

import com.soh3il.core.model.Occasion

/**
 * مرجع رسمی تعطیلات و مناسبت‌های تقویم رسمی جمهوری اسلامی ایران
 * استخراج‌شده بر اساس تقویم مصوب شورای مرکز تقویم مؤسسه ژئوفیزیک دانشگاه تهران و شورای فرهنگ عمومی.
 *
 * در این کلاس تفکیک کامل بین:
 * ۱. «تعطیلات رسمی» (isHoliday = true) که رنگ قرمز دارند.
 * ۲. «مناسبت‌های ملی، باستانی و فرهنگی غیرتعطیل» (isHoliday = false) که فقط به صورت بج خاکستری نمایش می‌یابند و روز را تعطیل نمی‌کنند.
 */
object IranianHolidays {

    private data class HolidayEntry(
        val month: Int,
        val day: Int,
        val title: String,
        val isHoliday: Boolean
    )

    // ۱. تعطیلات رسمی ثابت خورشیدی (در تمامی سال‌ها تعطیل رسمی هستند)
    private val FIXED_SOLAR_HOLIDAYS = listOf(
        HolidayEntry(1, 1, "آغاز سال نو / عید نوروز", isHoliday = true),
        HolidayEntry(1, 2, "عید نوروز", isHoliday = true),
        HolidayEntry(1, 3, "عید نوروز", isHoliday = true),
        HolidayEntry(1, 4, "عید نوروز", isHoliday = true),
        HolidayEntry(1, 12, "روز جمهوری اسلامی ایران", isHoliday = true),
        HolidayEntry(1, 13, "روز طبیعت (سیزده‌بدر)", isHoliday = true),
        HolidayEntry(3, 14, "رحلت حضرت امام خمینی (ره)", isHoliday = true),
        HolidayEntry(3, 15, "قیام خونین ۱۵ خرداد", isHoliday = true),
        HolidayEntry(11, 22, "سالروز پیروزی انقلاب اسلامی ایران", isHoliday = true),
        HolidayEntry(12, 29, "روز ملی شدن صنعت نفت ایران", isHoliday = true)
    )

    // ۲. مناسبت‌های ملی، باستانی و فرهنگی خورشیدی (غیرتعطیل - isHoliday = false)
    private val SOLAR_CULTURAL_OCCASIONS = listOf(
        HolidayEntry(1, 18, "روز سلامت (روز جهانی بهداشت)", isHoliday = false),
        HolidayEntry(1, 25, "روز بزرگداشت عطار نیشابوری", isHoliday = false),
        HolidayEntry(1, 29, "روز ارتش جمهوری اسلامی ایران", isHoliday = false),
        HolidayEntry(2, 1, "روز بزرگداشت سعدی", isHoliday = false),
        HolidayEntry(2, 2, "جشن اردیبهشت‌گان / روز زمین پاک", isHoliday = false),
        HolidayEntry(2, 10, "روز ملی خلیج فارس", isHoliday = false),
        HolidayEntry(2, 12, "روز معلم (شهادت استاد مرتضی مطهری)", isHoliday = false),
        HolidayEntry(2, 25, "روز بزرگداشت حکیم ابوالقاسم فردوسی و پاسداشت زبان فارسی", isHoliday = false),
        HolidayEntry(2, 28, "روز بزرگداشت حکیم عمر خیام", isHoliday = false),
        HolidayEntry(3, 1, "روز بزرگداشت ملاصدرا", isHoliday = false),
        HolidayEntry(4, 1, "جشن آب‌پاشونک (آغاز تابستان)", isHoliday = false),
        HolidayEntry(4, 14, "روز قلم", isHoliday = false),
        HolidayEntry(5, 8, "روز بزرگداشت شیخ شهاب‌الدین سهروردی", isHoliday = false),
        HolidayEntry(6, 1, "روز پزشک (بزرگداشت ابوعلی سینا)", isHoliday = false),
        HolidayEntry(6, 4, "جشن شهریورگان / روز کارمند", isHoliday = false),
        HolidayEntry(6, 5, "روز داروسازی (بزرگداشت زکریای رازی)", isHoliday = false),
        HolidayEntry(6, 27, "روز شعر و ادب فارسی (بزرگداشت استاد شهریار)", isHoliday = false),
        HolidayEntry(7, 8, "روز بزرگداشت مولوی", isHoliday = false),
        HolidayEntry(7, 10, "جشن مهرگان", isHoliday = false),
        HolidayEntry(7, 20, "روز بزرگداشت حافظ شیرازی", isHoliday = false),
        HolidayEntry(8, 7, "روز بزرگداشت کوروش بزرگ", isHoliday = false),
        HolidayEntry(9, 1, "جشن آذرگان", isHoliday = false),
        HolidayEntry(9, 16, "روز دانشجو", isHoliday = false),
        HolidayEntry(9, 30, "شب یلدا (جشن چله)", isHoliday = false),
        HolidayEntry(10, 5, "جشن دی‌گان / روز ایمنی در برابر زلزله", isHoliday = false),
        HolidayEntry(11, 10, "جشن سده (آتش‌افروزی زمستان)", isHoliday = false),
        HolidayEntry(12, 5, "جشن سپندارمذگان (روز مهر و عشق ایرانی) و روز مهندس", isHoliday = false),
        HolidayEntry(12, 15, "روز درختکاری", isHoliday = false)
    )

    // ۳. تعطیلات رسمی متغیر قمری استخراج‌شده از تقویم رسمی ژئوفیزیک دانشگاه تهران
    private val LUNAR_HOLIDAYS_MAP: Map<Int, List<HolidayEntry>> = mapOf(
        1403 to listOf(
            HolidayEntry(1, 13, "شهادت حضرت علی (ع)", isHoliday = true),
            HolidayEntry(1, 22, "عید سعید فطر", isHoliday = true),
            HolidayEntry(1, 23, "تعطیل به مناسبت عید سعید فطر", isHoliday = true),
            HolidayEntry(2, 15, "شهادت امام جعفر صادق (ع)", isHoliday = true),
            HolidayEntry(3, 28, "عید سعید قربان", isHoliday = true),
            HolidayEntry(4, 5, "عید سعید غدیر خم", isHoliday = true),
            HolidayEntry(4, 25, "تاسوعای حسینی", isHoliday = true),
            HolidayEntry(4, 26, "عاشورای حسینی", isHoliday = true),
            HolidayEntry(6, 4, "اربعین حسینی", isHoliday = true),
            HolidayEntry(6, 12, "رحلت رسول اکرم (ص) و شهادت امام حسن مجتبی (ع)", isHoliday = true),
            HolidayEntry(6, 14, "شهادت امام رضا (ع)", isHoliday = true),
            HolidayEntry(6, 22, "شهادت امام حسن عسکری (ع) و آغاز امامت امام زمان (عج)", isHoliday = true),
            HolidayEntry(6, 31, "ولادت حضرت رسول اکرم (ص) و امام جعفر صادق (ع)", isHoliday = true),
            HolidayEntry(9, 15, "شهادت حضرت فاطمه زهرا (س)", isHoliday = true),
            HolidayEntry(10, 25, "ولادت امام علی (ع) و روز پدر", isHoliday = true),
            HolidayEntry(11, 9, "مبعث حضرت رسول اکرم (ص)", isHoliday = true),
            HolidayEntry(11, 26, "ولادت حضرت قائم (عج) و نیمه شعبان", isHoliday = true)
        ),
        1404 to listOf(
            HolidayEntry(1, 2, "شهادت حضرت علی (ع)", isHoliday = true),
            HolidayEntry(1, 11, "عید سعید فطر", isHoliday = true),
            HolidayEntry(1, 12, "تعطیل به مناسبت عید سعید فطر", isHoliday = true),
            HolidayEntry(2, 4, "شهادت امام جعفر صادق (ع)", isHoliday = true),
            HolidayEntry(3, 16, "عید سعید قربان", isHoliday = true),
            HolidayEntry(3, 24, "عید سعید غدیر خم", isHoliday = true),
            HolidayEntry(4, 14, "تاسوعای حسینی", isHoliday = true),
            HolidayEntry(4, 15, "عاشورای حسینی", isHoliday = true),
            HolidayEntry(5, 23, "اربعین حسینی", isHoliday = true),
            HolidayEntry(5, 31, "رحلت رسول اکرم (ص) و شهادت امام حسن مجتبی (ع)", isHoliday = true),
            HolidayEntry(6, 2, "شهادت امام رضا (ع)", isHoliday = true),
            HolidayEntry(6, 10, "شهادت امام حسن عسکری (ع) و آغاز امامت حضرت مهدی (عج)", isHoliday = true),
            HolidayEntry(6, 19, "میلاد رسول اکرم (ص) و امام جعفر صادق (ع)", isHoliday = true),
            HolidayEntry(9, 3, "شهادت حضرت فاطمه زهرا (س)", isHoliday = true),
            HolidayEntry(10, 13, "ولادت امام علی (ع) و روز پدر", isHoliday = true),
            HolidayEntry(10, 27, "مبعث حضرت رسول اکرم (ص)", isHoliday = true),
            HolidayEntry(11, 15, "ولادت حضرت مهدی (عج) و نیمه شعبان", isHoliday = true),
            HolidayEntry(12, 20, "شهادت حضرت علی (ع)", isHoliday = true)
        ),
        1405 to listOf(
            HolidayEntry(1, 1, "عید سعید فطر", isHoliday = true),
            HolidayEntry(1, 2, "تعطیل به مناسبت عید سعید فطر", isHoliday = true),
            HolidayEntry(1, 25, "شهادت امام جعفر صادق (ع)", isHoliday = true),
            HolidayEntry(3, 6, "عید سعید قربان", isHoliday = true),
            HolidayEntry(3, 14, "عید سعید غدیر خم", isHoliday = true),
            HolidayEntry(4, 3, "تاسوعای حسینی", isHoliday = true),
            HolidayEntry(4, 4, "عاشورای حسینی", isHoliday = true),
            HolidayEntry(5, 13, "اربعین حسینی", isHoliday = true),
            HolidayEntry(5, 21, "رحلت رسول اکرم (ص) و شهادت امام حسن مجتبی (ع)", isHoliday = true),
            HolidayEntry(5, 22, "شهادت امام رضا (ع)", isHoliday = true),
            HolidayEntry(5, 30, "شهادت امام حسن عسکری (ع)", isHoliday = true),
            HolidayEntry(6, 8, "میلاد رسول اکرم (ص) و امام جعفر صادق (ع)", isHoliday = true),
            HolidayEntry(8, 22, "شهادت حضرت فاطمه زهرا (س)", isHoliday = true),
            HolidayEntry(10, 2, "ولادت امام علی (ع) و روز پدر", isHoliday = true),
            HolidayEntry(10, 16, "مبعث حضرت رسول اکرم (ص)", isHoliday = true),
            HolidayEntry(11, 4, "ولادت حضرت مهدی (عج) و نیمه شعبان", isHoliday = true),
            HolidayEntry(12, 9, "شهادت حضرت علی (ع)", isHoliday = true),
            HolidayEntry(12, 19, "عید سعید فطر", isHoliday = true),
            HolidayEntry(12, 20, "تعطیل به مناسبت عید سعید فطر", isHoliday = true)
        ),
        1406 to listOf(
            HolidayEntry(1, 14, "شهادت امام جعفر صادق (ع)", isHoliday = true),
            HolidayEntry(2, 26, "عید سعید قربان", isHoliday = true),
            HolidayEntry(3, 3, "عید سعید غدیر خم", isHoliday = true),
            HolidayEntry(3, 23, "تاسوعای حسینی", isHoliday = true),
            HolidayEntry(3, 24, "عاشورای حسینی", isHoliday = true),
            HolidayEntry(5, 2, "اربعین حسینی", isHoliday = true),
            HolidayEntry(5, 10, "رحلت رسول اکرم (ص) و شهادت امام حسن مجتبی (ع)", isHoliday = true),
            HolidayEntry(5, 12, "شهادت امام رضا (ع)", isHoliday = true),
            HolidayEntry(5, 20, "شهادت امام حسن عسکری (ع)", isHoliday = true),
            HolidayEntry(5, 29, "میلاد رسول اکرم (ص) و امام جعفر صادق (ع)", isHoliday = true),
            HolidayEntry(8, 12, "شهادت حضرت فاطمه زهرا (س)", isHoliday = true),
            HolidayEntry(9, 21, "ولادت امام علی (ع) و روز پدر", isHoliday = true),
            HolidayEntry(10, 5, "مبعث حضرت رسول اکرم (ص)", isHoliday = true),
            HolidayEntry(10, 23, "ولادت حضرت مهدی (عج) و نیمه شعبان", isHoliday = true),
            HolidayEntry(11, 28, "شهادت حضرت علی (ع)", isHoliday = true),
            HolidayEntry(12, 8, "عید سعید فطر", isHoliday = true),
            HolidayEntry(12, 9, "تعطیل به مناسبت عید سعید فطر", isHoliday = true)
        )
    )

    /**
     * استخراج تمامی مناسبت‌ها و تعطیلات برای یک ماه خاص جلالی
     */
    fun hasOfficialLunarHolidays(year: Int): Boolean = year in LUNAR_HOLIDAYS_MAP

    fun getOccasionsForMonth(year: Int, month: Int): List<Occasion> {
        val list = mutableListOf<Occasion>()

        // ۱. تعطیلات رسمی ثابت خورشیدی (تعطیل رسمی = true)
        FIXED_SOLAR_HOLIDAYS.filter { it.month == month }.forEach { entry ->
            val dateStr = formatJalaliDate(year, month, entry.day)
            list.add(
                Occasion(
                    id = "solar_holiday_${month}_${entry.day}",
                    jalaliDate = dateStr,
                    titleFa = entry.title,
                    isHoliday = true
                )
            )
        }

        // ۲. مناسبت‌های ملی و باستانی خورشیدی (غیرتعطیل = false)
        SOLAR_CULTURAL_OCCASIONS.filter { it.month == month }.forEach { entry ->
            val dateStr = formatJalaliDate(year, month, entry.day)
            list.add(
                Occasion(
                    id = "solar_cultural_${month}_${entry.day}",
                    jalaliDate = dateStr,
                    titleFa = entry.title,
                    isHoliday = false
                )
            )
        }

        // ۳. بررسی روز ۳۰ اسفند در سال‌های کبیسه (تعطیل رسمی سال کبیسه)
        if (month == 12 && JalaliDate.isLeap(year)) {
            list.add(
                Occasion(
                    id = "solar_12_30_leap",
                    jalaliDate = formatJalaliDate(year, 12, 30),
                    titleFa = "آخرین روز سال (تعطیل رسمی سال کبیسه)",
                    isHoliday = true
                )
            )
        }

        // ۴. تعطیلات رسمی متغیر قمری سال مورد نظر (تعطیل رسمی = true)
        LUNAR_HOLIDAYS_MAP[year]?.filter { it.month == month }?.forEach { entry ->
            val dateStr = formatJalaliDate(year, month, entry.day)
            list.add(
                Occasion(
                    id = "lunar_${year}_${month}_${entry.day}_${entry.title.hashCode()}",
                    jalaliDate = dateStr,
                    titleFa = entry.title,
                    isHoliday = entry.isHoliday
                )
            )
        }

        return list.sortedWith(
            compareBy<Occasion> { occ ->
                occ.jalaliDate.split("-").lastOrNull()?.toIntOrNull() ?: 0
            }.thenByDescending { it.isHoliday }
        )
    }

    private fun formatJalaliDate(year: Int, month: Int, day: Int): String {
        val mStr = month.toString().padStart(2, '0')
        val dStr = day.toString().padStart(2, '0')
        return "$year-$mStr-$dStr"
    }
}
