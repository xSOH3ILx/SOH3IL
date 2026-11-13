package com.soh3il.core.database.converter

import androidx.room.TypeConverter
import com.soh3il.core.model.PreReminder
import com.soh3il.core.model.PreReminderUnit
import com.soh3il.core.model.RecurrenceRule
import com.soh3il.core.model.RecurrenceType
import com.soh3il.core.model.ShortMonthPolicy

class DatabaseConverters {

    @TypeConverter
    fun fromRecurrenceRule(rule: RecurrenceRule): String {
        val days = rule.daysOfWeek.joinToString(",")
        return "${rule.type.name}|${rule.interval}|${rule.dayOfMonth ?: ""}|$days|${rule.shortMonthPolicy.name}"
    }

    @TypeConverter
    fun toRecurrenceRule(data: String): RecurrenceRule {
        val parts = data.split("|")
        val type = runCatching {
            RecurrenceType.valueOf(parts.getOrElse(0) { RecurrenceType.NONE.name })
        }.getOrDefault(RecurrenceType.NONE)
        val interval = (parts.getOrNull(1)?.toIntOrNull() ?: 1).coerceAtLeast(1)
        val dayOfMonth = parts.getOrNull(2)?.toIntOrNull()
        val daysOfWeek = parts.getOrNull(3)
            ?.takeIf { it.isNotEmpty() }
            ?.split(",")
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet() ?: emptySet()
        val policy = parts.getOrNull(4)?.let {
            runCatching { ShortMonthPolicy.valueOf(it) }.getOrNull()
        } ?: ShortMonthPolicy.SNAP_TO_LAST_DAY

        return RecurrenceRule(
            type = type,
            interval = interval,
            dayOfMonth = dayOfMonth,
            daysOfWeek = daysOfWeek,
            shortMonthPolicy = policy
        )
    }

    @TypeConverter
    fun fromPreReminders(list: List<PreReminder>): String {
        return list.joinToString(";") { "${it.value}:${it.unit.name}" }
    }

    @TypeConverter
    fun toPreReminders(data: String): List<PreReminder> {
        if (data.isBlank()) return emptyList()
        return data.split(";").mapNotNull { item ->
            val parts = item.split(":")
            if (parts.size == 2) {
                val value = parts[0].toIntOrNull()
                val unit = runCatching { PreReminderUnit.valueOf(parts[1]) }.getOrNull()
                if (value != null && value > 0 && unit != null) PreReminder(value, unit) else null
            } else null
        }
    }

    @TypeConverter
    fun fromStringList(list: List<String>): String = list.joinToString(",")

    @TypeConverter
    fun toStringList(data: String): List<String> =
        if (data.isBlank()) emptyList() else data.split(",")
}
