package com.soh3il.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.soh3il.core.model.PreReminder
import com.soh3il.core.model.RecurrenceRule
import com.soh3il.core.model.Reminder

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val jalaliYear: Int,
    val jalaliMonth: Int,
    val jalaliDay: Int,
    val hour: Int,
    val minute: Int,
    val recurrenceRule: RecurrenceRule,
    val preReminders: List<PreReminder>,
    val isEnabled: Boolean = true,
    val colorHex: String = "#2563EB",
    val nextTriggerEpochMillis: Long = 0L
) {
    fun toDomain(): Reminder = Reminder(
        id = id,
        title = title,
        description = description,
        jalaliYear = jalaliYear,
        jalaliMonth = jalaliMonth,
        jalaliDay = jalaliDay,
        hour = hour,
        minute = minute,
        recurrence = recurrenceRule,
        preReminders = preReminders,
        isEnabled = isEnabled,
        colorHex = colorHex,
        nextTriggerEpochMillis = nextTriggerEpochMillis
    )

    companion object {
        fun fromDomain(domain: Reminder): ReminderEntity = ReminderEntity(
            id = domain.id,
            title = domain.title,
            description = domain.description,
            jalaliYear = domain.jalaliYear,
            jalaliMonth = domain.jalaliMonth,
            jalaliDay = domain.jalaliDay,
            hour = domain.hour,
            minute = domain.minute,
            recurrenceRule = domain.recurrence,
            preReminders = domain.preReminders,
            isEnabled = domain.isEnabled,
            colorHex = domain.colorHex,
            nextTriggerEpochMillis = domain.nextTriggerEpochMillis
        )
    }
}

@Entity(tableName = "occasions")
data class OccasionEntity(
    @PrimaryKey
    val id: String,
    val jalaliDate: String, // YYYY-MM-DD
    val jalaliYear: Int,
    val jalaliMonth: Int,
    val jalaliDay: Int,
    val titleFa: String,
    val titleEn: String? = null,
    val isHoliday: Boolean = false,
    val categories: List<String> = emptyList()
)
