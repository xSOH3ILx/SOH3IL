package com.soh3il.core.jalali

import com.soh3il.core.model.Occasion

object IranianHolidays {

    private data class HolidayEntry(
        val month: Int,
        val day: Int,
        val title: String,
        val isHoliday: Boolean = true
    )

    // Fixed Solar Holidays that occur in EVERY solar year
    private val FIXED_SOLAR_HOLIDAYS = listOf(
        HolidayEntry(1, 1, "آغاز نوروز (عید نوروز)"),
        HolidayEntry(1, 2, "عید نوروز"),
        HolidayEntry(1, 3, "عید نوروز"),
        HolidayEntry(1, 4, "عید نوروز"),
        HolidayEntry(1, 12, "روز جمهوری اسلامی ایران"),
        HolidayEntry(1, 13, "روز طبیعت (سیزده‌بدر)"),
        HolidayEntry(3, 14, "رحلت حضرت امام خمینی (ره)"),
        HolidayEntry(3, 15, "قیام خونین ۱۵ خرداد"),
        HolidayEntry(11, 22, "سالروز پیروزی انقلاب اسلامی ایران"),
        HolidayEntry(12, 29, "روز ملی شدن صنعت نفت ایران")
    )

    // Lunar-based official holidays pre-computed for each solar year
    private val LUNAR_HOLIDAYS_MAP: Map<Int, List<HolidayEntry>> = mapOf(
        1403 to listOf(
            HolidayEntry(1, 23, "عید سعید فطر"),
            HolidayEntry(1, 24, "تعطیل به مناسبت عید سعید فطر"),
            HolidayEntry(2, 15, "شهادت امام جعفر صادق (ع)"),
            HolidayEntry(3, 28, "عید سعید قربان"),
            HolidayEntry(4, 5, "عید سعید غدیر خم"),
            HolidayEntry(4, 25, "تاسوعای حسینی"),
            HolidayEntry(4, 26, "عاشورای حسینی"),
            HolidayEntry(6, 4, "اربعین حسینی"),
            HolidayEntry(6, 12, "رحلت رسول اکرم (ص) و شهادت امام حسن مجتبی (ع)"),
            HolidayEntry(6, 14, "شهادت امام رضا (ع)"),
            HolidayEntry(6, 22, "شهادت امام حسن عسکری (ع) و آغاز امامت حضرت مهدی (عج)"),
            HolidayEntry(6, 31, "میلاد رسول اکرم (ص) و امام جعفر صادق (ع)"),
            HolidayEntry(9, 15, "شهادت حضرت فاطمه زهرا (س)"),
            HolidayEntry(10, 25, "ولادت امام علی (ع) و روز پدر"),
            HolidayEntry(11, 9, "مبعث حضرت رسول اکرم (ص)"),
            HolidayEntry(11, 26, "ولادت حضرت قائم (عج) و نیمه شعبان")
        ),
        1404 to listOf(
            HolidayEntry(1, 11, "عید سعید فطر"),
            HolidayEntry(1, 12, "تعطیل به مناسبت عید سعید فطر"),
            HolidayEntry(2, 5, "شهادت امام جعفر صادق (ع)"),
            HolidayEntry(3, 16, "عید سعید قربان"),
            HolidayEntry(3, 24, "عید سعید غدیر خم"),
            HolidayEntry(4, 14, "تاسوعای حسینی"),
            HolidayEntry(4, 15, "عاشورای حسینی"),
            HolidayEntry(5, 24, "اربعین حسینی"),
            HolidayEntry(6, 1, "رحلت رسول اکرم (ص) و شهادت امام حسن مجتبی (ع)"),
            HolidayEntry(6, 3, "شهادت امام رضا (ع)"),
            HolidayEntry(6, 11, "شهادت امام حسن عسکری (ع) و آغاز امامت حضرت مهدی (عج)"),
            HolidayEntry(6, 20, "میلاد رسول اکرم (ص) و امام جعفر صادق (ع)"),
            HolidayEntry(9, 3, "شهادت حضرت فاطمه زهرا (س)"),
            HolidayEntry(10, 14, "ولادت امام علی (ع) و روز پدر"),
            HolidayEntry(10, 28, "مبعث حضرت رسول اکرم (ص)"),
            HolidayEntry(11, 15, "ولادت حضرت مهدی (عج) و نیمه شعبان"),
            HolidayEntry(12, 20, "شهادت حضرت علی (ع)")
        ),
        1405 to listOf(
            HolidayEntry(1, 31, "عید سعید فطر"),
            HolidayEntry(2, 1, "تعطیل به مناسبت عید سعید فطر"),
            HolidayEntry(2, 25, "شهادت امام جعفر صادق (ع)"),
            HolidayEntry(3, 6, "عید سعید قربان"),
            HolidayEntry(3, 14, "عید سعید غدیر خم"),
            HolidayEntry(4, 4, "تاسوعای حسینی"),
            HolidayEntry(4, 5, "عاشورای حسینی"),
            HolidayEntry(5, 14, "اربعین حسینی"),
            HolidayEntry(5, 22, "رحلت رسول اکرم (ص) و شهادت امام حسن مجتبی (ع)"),
            HolidayEntry(5, 24, "شهادت امام رضا (ع)"),
            HolidayEntry(6, 2, "شهادت امام حسن عسکری (ع)"),
            HolidayEntry(6, 11, "میلاد رسول اکرم (ص) و امام جعفر صادق (ع)"),
            HolidayEntry(8, 23, "شهادت حضرت فاطمه زهرا (س)"),
            HolidayEntry(10, 4, "ولادت امام علی (ع) و روز پدر"),
            HolidayEntry(10, 18, "مبعث حضرت رسول اکرم (ص)"),
            HolidayEntry(11, 5, "ولادت حضرت مهدی (عج) و نیمه شعبان"),
            HolidayEntry(12, 9, "شهادت حضرت علی (ع)"),
            HolidayEntry(12, 19, "عید سعید فطر"),
            HolidayEntry(12, 20, "تعطیل به مناسبت عید فطر")
        ),
        1406 to listOf(
            HolidayEntry(2, 14, "شهادت امام جعفر صادق (ع)"),
            HolidayEntry(2, 26, "عید سعید قربان"),
            HolidayEntry(3, 3, "عید سعید غدیر خم"),
            HolidayEntry(3, 23, "تاسوعای حسینی"),
            HolidayEntry(3, 24, "عاشورای حسینی"),
            HolidayEntry(5, 2, "اربعین حسینی"),
            HolidayEntry(5, 10, "رحلت رسول اکرم (ص) و شهادت امام حسن مجتبی (ع)"),
            HolidayEntry(5, 12, "شهادت امام رضا (ع)"),
            HolidayEntry(5, 21, "شهادت امام حسن عسکری (ع)"),
            HolidayEntry(5, 30, "میلاد رسول اکرم (ص) و امام جعفر صادق (ع)"),
            HolidayEntry(8, 12, "شهادت حضرت فاطمه زهرا (س)"),
            HolidayEntry(9, 23, "ولادت امام علی (ع) و روز پدر"),
            HolidayEntry(10, 7, "مبعث حضرت رسول اکرم (ص)"),
            HolidayEntry(10, 24, "ولادت حضرت مهدی (عج) و نیمه شعبان"),
            HolidayEntry(11, 28, "شهادت حضرت علی (ع)"),
            HolidayEntry(12, 8, "عید سعید فطر"),
            HolidayEntry(12, 9, "تعطیل به مناسبت عید سعید فطر")
        )
    )

    /**
     * Returns all official holidays and occasions for a specific Jalali month.
     */
    fun getOccasionsForMonth(year: Int, month: Int): List<Occasion> {
        val list = mutableListOf<Occasion>()

        // 1. Fixed solar holidays
        FIXED_SOLAR_HOLIDAYS.filter { it.month == month }.forEach { entry ->
            val dateStr = formatJalaliDate(year, month, entry.day)
            list.add(
                Occasion(
                    id = "solar_${month}_${entry.day}",
                    jalaliDate = dateStr,
                    titleFa = entry.title,
                    isHoliday = entry.isHoliday
                )
            )
        }

        // Check Esfand 30th in leap years
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

        // 2. Pre-computed lunar holidays for known years
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

        return list.sortedBy { occasion ->
            occasion.jalaliDate.split("-").lastOrNull()?.toIntOrNull() ?: 0
        }
    }

    private fun formatJalaliDate(year: Int, month: Int, day: Int): String {
        val mStr = month.toString().padStart(2, '0')
        val dStr = day.toString().padStart(2, '0')
        return "$year-$mStr-$dStr"
    }
}
