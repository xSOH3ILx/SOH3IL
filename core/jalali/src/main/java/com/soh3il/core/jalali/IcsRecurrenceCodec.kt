package com.soh3il.core.jalali

import com.soh3il.core.model.RecurrenceRule
import com.soh3il.core.model.RecurrenceType
import com.soh3il.core.model.Reminder
import com.soh3il.core.model.ShortMonthPolicy

internal object IcsRecurrenceCodec {
    private val ICS_DAY_TO_PERSIAN = mapOf(
        "SA" to 1, "SU" to 2, "MO" to 3, "TU" to 4, "WE" to 5, "TH" to 6, "FR" to 7
    )
    private val PERSIAN_TO_ICS_DAY = ICS_DAY_TO_PERSIAN.entries.associate { (ics, day) -> day to ics }

    fun standardRrule(rem: Reminder): String? {
        val interval = rem.recurrence.interval.coerceAtLeast(1)
        return when (rem.recurrence.type) {
            RecurrenceType.DAILY -> "RRULE:FREQ=DAILY;INTERVAL=$interval"
            RecurrenceType.WEEKLY -> {
                val days = rem.recurrence.daysOfWeek.sorted().mapNotNull { PERSIAN_TO_ICS_DAY[it] }
                val byDay = if (days.isEmpty()) "" else ";BYDAY=${days.joinToString(",")}"
                "RRULE:FREQ=WEEKLY;INTERVAL=$interval$byDay"
            }
            RecurrenceType.MONTHLY_JALALI, RecurrenceType.MONTHLY_GREGORIAN -> "RRULE:FREQ=MONTHLY;INTERVAL=$interval"
            RecurrenceType.YEARLY_JALALI, RecurrenceType.YEARLY_GREGORIAN -> "RRULE:FREQ=YEARLY;INTERVAL=$interval"
            RecurrenceType.NONE -> null
        }
    }

    fun xRecurrence(rem: Reminder): String {
        val days = rem.recurrence.daysOfWeek.sorted().joinToString(",")
        val day = rem.recurrence.dayOfMonth?.toString().orEmpty()
        return "${rem.recurrence.type.name};INTERVAL=${rem.recurrence.interval.coerceAtLeast(1)};DAY=$day;WEEKDAYS=$days;POLICY=${rem.recurrence.shortMonthPolicy.name}"
    }

    fun parseXRecurrence(value: String): RecurrenceRule {
        val parts = value.split(";")
        val type = runCatching { RecurrenceType.valueOf(parts.first()) }.getOrDefault(RecurrenceType.NONE)
        fun field(name: String): String? = parts.firstOrNull { it.startsWith("$name=") }?.substringAfter("=")
        val interval = field("INTERVAL")?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val dayOfMonth = field("DAY")?.toIntOrNull()
        require(dayOfMonth == null || dayOfMonth in 1..31) { "روز تکرار تقویم معتبر نیست" }
        val daysOfWeek = field("WEEKDAYS")
            ?.takeIf { it.isNotBlank() }
            ?.split(",")
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet()
            ?: emptySet()
        val policy = field("POLICY")?.let {
            runCatching { ShortMonthPolicy.valueOf(it) }.getOrNull()
        } ?: ShortMonthPolicy.SNAP_TO_LAST_DAY
        return RecurrenceRule(type, interval, dayOfMonth, daysOfWeek, policy)
    }

    fun parseStandardRrule(rrule: String): RecurrenceRule {
        var recType = RecurrenceType.NONE
        var recInterval = 1
        if (rrule.contains("FREQ=DAILY", ignoreCase = true)) {
            recType = RecurrenceType.DAILY
        } else if (rrule.contains("FREQ=WEEKLY", ignoreCase = true)) {
            recType = RecurrenceType.WEEKLY
        } else if (rrule.contains("FREQ=MONTHLY", ignoreCase = true)) {
            recType = RecurrenceType.MONTHLY_GREGORIAN
        } else if (rrule.contains("FREQ=YEARLY", ignoreCase = true)) {
            recType = RecurrenceType.YEARLY_GREGORIAN
        }

        if (rrule.contains("INTERVAL=", ignoreCase = true)) {
            val intervalStr = rrule.substringAfter("INTERVAL=").substringBefore(";")
            recInterval = intervalStr.toIntOrNull() ?: 1
        }
        val daysOfWeek = if (rrule.contains("BYDAY=", ignoreCase = true)) {
            rrule.substringAfter("BYDAY=")
                .substringBefore(";")
                .split(",")
                .mapNotNull { ICS_DAY_TO_PERSIAN[it.trim().uppercase()] }
                .toSet()
        } else {
            emptySet()
        }
        return RecurrenceRule(
            type = recType,
            interval = recInterval.coerceAtLeast(1),
            daysOfWeek = daysOfWeek,
            shortMonthPolicy = ShortMonthPolicy.SKIP_MONTH
        )
    }

}
