package com.soh3il.core.jalali.engine

/**
 * Official Iranian Calendar Leap Year Lookup Table (1304 to 1498).
 * Directly compatible with:
 * https://calendar.ut.ac.ir/Fa/News/Data/Doc/KabiseShamsi1206-1498-new.pdf
 */
object LookupTableConverter {
    private const val startingYear = 1304
    private val yearsStartingJdn = LongArray(1498 - startingYear)

    private val daysToMonth =
        intArrayOf(0, 31, 62, 93, 124, 155, 186, 216, 246, 276, 306, 336, 366)

    fun daysInPreviousMonths(month: Int): Int = daysToMonth[month - 1]
    fun monthFromDaysCount(days: Int): Int = daysToMonth.indexOfFirst { it >= days }

    init {
        val leapYears = intArrayOf(
            1304, 1309, 1313, 1317, 1321, 1325, 1329, 1333, 1337, 1342, 1346, 1350, 1354, 1358,
            1362, 1366, 1370, 1375, 1379, 1383, 1387, 1391, 1395, 1399, 1403, 1408, 1412, 1416,
            1420, 1424, 1428, 1432, 1436, 1441, 1445, 1449, 1453, 1457, 1461, 1465, 1469, 1474,
            1478, 1482, 1486, 1490, 1494, 1498
        )
        yearsStartingJdn[0] = 2424231L /* jdn of 1304-01-01 */
        var i = 0
        var j = 0
        while (i < yearsStartingJdn.size - 1) {
            val year = i + startingYear
            yearsStartingJdn[i + 1] = yearsStartingJdn[i] + if (leapYears[j] == year) 366 else 365
            if (year >= leapYears[j] && j + 1 < leapYears.size) j++
            ++i
        }
    }

    fun toJdn(year: Int, month: Int, day: Int): Long {
        if (year < startingYear || year >= startingYear + yearsStartingJdn.size) return -1L
        return yearsStartingJdn[year - startingYear] + daysInPreviousMonths(month) + day - 1
    }

    fun fromJdn(jdn: Long): DateTriplet? {
        if (jdn < yearsStartingJdn[0] || jdn > yearsStartingJdn[yearsStartingJdn.size - 1]) return null
        var year = (jdn - yearsStartingJdn[0]).toInt() / 366
        while (year < yearsStartingJdn.size - 1) {
            if (jdn < yearsStartingJdn[year + 1]) break
            ++year
        }
        val startOfYearJdn = yearsStartingJdn[year]
        year += startingYear
        val dayOfYear = (jdn - startOfYearJdn).toInt() + 1
        val month = monthFromDaysCount(dayOfYear)
        val day = dayOfYear - daysInPreviousMonths(month)
        return DateTriplet(year = year, month = month, dayOfMonth = day)
    }

    fun isLeap(year: Int): Boolean {
        if (year < startingYear || year >= startingYear + yearsStartingJdn.size - 1) return false
        val diff = yearsStartingJdn[year - startingYear + 1] - yearsStartingJdn[year - startingYear]
        return diff == 366L
    }
}
