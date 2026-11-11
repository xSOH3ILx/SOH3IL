package com.soh3il.core.model

/**
 * Recurrence frequency types for reminders
 */
enum class RecurrenceType {
    NONE,           // One-off reminder
    DAILY,          // Every day
    WEEKLY,         // Every week on specific days of the week
    MONTHLY_JALALI, // Every solar month on specific solar day (e.g. 3rd of every month)
    YEARLY_JALALI   // Every solar year on specific solar date (e.g. 1st of Farvardin)
}

/**
 * Policy for when a day does not exist in a shorter month (e.g. day 31 in a 30-day month)
 */
enum class ShortMonthPolicy {
    SNAP_TO_LAST_DAY, // Trigger on the last day of the shorter month (e.g. day 30 or 29)
    SKIP_MONTH        // Skip this occurrence entirely
}

/**
 * Defines the recurrence rule for an event/reminder
 */
data class RecurrenceRule(
    val type: RecurrenceType = RecurrenceType.NONE,
    val interval: Int = 1, // Every 1 month, every 2 weeks, etc.
    val dayOfMonth: Int? = null, // e.g., 3 for "3rd of every month"
    val daysOfWeek: Set<Int> = emptySet(), // 1=Shanbeh to 7=Jomeh
    val shortMonthPolicy: ShortMonthPolicy = ShortMonthPolicy.SNAP_TO_LAST_DAY
)

/**
 * Pre-reminder offset before main reminder time
 */
enum class PreReminderUnit {
    MINUTES,
    HOURS,
    DAYS
}

data class PreReminder(
    val value: Int,
    val unit: PreReminderUnit
) {
    fun toMinutes(): Long = when (unit) {
        PreReminderUnit.MINUTES -> value.toLong()
        PreReminderUnit.HOURS -> value.toLong() * 60
        PreReminderUnit.DAYS -> value.toLong() * 60 * 24
    }
}

/**
 * Core Reminder entity
 */
data class Reminder(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val jalaliYear: Int,
    val jalaliMonth: Int,
    val jalaliDay: Int,
    val hour: Int,
    val minute: Int,
    val recurrence: RecurrenceRule = RecurrenceRule(),
    val preReminders: List<PreReminder> = emptyList(),
    val isEnabled: Boolean = true,
    val colorHex: String = "#2563EB",
    val nextTriggerEpochMillis: Long = 0L
)

/**
 * Pre-defined reminder categories with dedicated brand colors
 */
data class ReminderCategory(
    val id: String,
    val nameFa: String,
    val colorHex: String
) {
    companion object {
        val ALL = listOf(
            ReminderCategory("work", "کاری", "#2563EB"),
            ReminderCategory("personal", "شخصی", "#10B981"),
            ReminderCategory("birthday", "تولد", "#F59E0B"),
            ReminderCategory("finance", "مالی", "#8B5CF6"),
            ReminderCategory("study", "تحصیلی", "#06B6D4"),
            ReminderCategory("general", "عمومی", "#64748B")
        )

        fun findByColor(colorHex: String): ReminderCategory =
            ALL.firstOrNull { it.colorHex.equals(colorHex, ignoreCase = true) } ?: ALL.first()
    }
}

/**
 * Occasions & Holidays sourced from FarsiUI dataset
 */
data class Occasion(
    val id: String,
    val jalaliDate: String, // YYYY-MM-DD
    val titleFa: String,
    val titleEn: String? = null,
    val isHoliday: Boolean = false,
    val categories: List<String> = emptyList()
)

/**
 * Application Theme preference
 */
enum class AppTheme {
    LIGHT,
    DARK,
    SYSTEM,
    AMOLED_DARK
}
