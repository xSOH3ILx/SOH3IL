package com.soh3il.feature.reminder

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.mapSaver
import com.soh3il.core.jalali.JalaliDate
import com.soh3il.core.model.PreReminder
import com.soh3il.core.model.PreReminderUnit
import com.soh3il.core.model.RecurrenceType
import com.soh3il.core.model.Reminder

internal class ReminderDraftState(initialDate: JalaliDate, existingReminder: Reminder?) {
    private val initialPreMinutes = existingReminder?.preReminders?.firstOrNull()?.toMinutes()?.toInt()
    var selectedDate by mutableStateOf(existingReminder?.let { JalaliDate(it.jalaliYear, it.jalaliMonth, it.jalaliDay) } ?: initialDate)
    var title by mutableStateOf(existingReminder?.title ?: "")
    var description by mutableStateOf(existingReminder?.description ?: "")
    var selectedHour by mutableIntStateOf(existingReminder?.hour ?: 9)
    var selectedMinute by mutableIntStateOf(existingReminder?.minute ?: 0)
    var selectedColorHex by mutableStateOf(existingReminder?.colorHex ?: "#2563EB")
    var showTimePickerDialog by mutableStateOf(false)
    var recurrenceType by mutableStateOf(existingReminder?.recurrence?.type ?: RecurrenceType.NONE)
    var recurrenceInterval by mutableIntStateOf(existingReminder?.recurrence?.interval ?: 1)
    var showCustomRecurrenceDialog by mutableStateOf(false)
    var recurrenceExpanded by mutableStateOf(false)
    var selectedPreReminderMinutes by mutableStateOf(initialPreMinutes?.takeIf { it in listOf(15, 30, 60, 1440, 2880) })
    var customPreReminder by mutableStateOf(existingReminder?.preReminders?.firstOrNull()?.takeIf { it.toMinutes().toInt() !in listOf(15, 30, 60, 1440, 2880) })
    var isCustomPreReminderSelected by mutableStateOf(customPreReminder != null)
    var showCustomPreReminderDialog by mutableStateOf(false)
    var preReminderExpanded by mutableStateOf(false)
    var showDeleteConfirmDialog by mutableStateOf(false)

    companion object {
        val Saver = mapSaver<ReminderDraftState>(
            save = {
                mapOf(
                    "selectedDate" to it.selectedDate.toJdn(),
                    "title" to it.title,
                    "description" to it.description,
                    "selectedHour" to it.selectedHour,
                    "selectedMinute" to it.selectedMinute,
                    "selectedColorHex" to it.selectedColorHex,
                    "showTimePickerDialog" to it.showTimePickerDialog,
                    "recurrenceType" to it.recurrenceType.name,
                    "recurrenceInterval" to it.recurrenceInterval,
                    "showCustomRecurrenceDialog" to it.showCustomRecurrenceDialog,
                    "recurrenceExpanded" to it.recurrenceExpanded,
                    "selectedPreReminderMinutes" to it.selectedPreReminderMinutes,
                    "customPreReminder" to it.customPreReminder?.let { pre -> "${pre.value}|${pre.unit.name}" },
                    "isCustomPreReminderSelected" to it.isCustomPreReminderSelected,
                    "showCustomPreReminderDialog" to it.showCustomPreReminderDialog,
                    "preReminderExpanded" to it.preReminderExpanded,
                    "showDeleteConfirmDialog" to it.showDeleteConfirmDialog
                )
            },
            restore = { values ->
                ReminderDraftState(JalaliDate.fromJdn(values.getValue("selectedDate") as Long), null).apply {
                    selectedDate = JalaliDate.fromJdn(values.getValue("selectedDate") as Long)
                    title = values.getValue("title") as String
                    description = values.getValue("description") as String
                    selectedHour = values.getValue("selectedHour") as Int
                    selectedMinute = values.getValue("selectedMinute") as Int
                    selectedColorHex = values.getValue("selectedColorHex") as String
                    showTimePickerDialog = values.getValue("showTimePickerDialog") as Boolean
                    recurrenceType = RecurrenceType.valueOf(values.getValue("recurrenceType") as String)
                    recurrenceInterval = values.getValue("recurrenceInterval") as Int
                    showCustomRecurrenceDialog = values.getValue("showCustomRecurrenceDialog") as Boolean
                    recurrenceExpanded = values.getValue("recurrenceExpanded") as Boolean
                    selectedPreReminderMinutes = values["selectedPreReminderMinutes"] as Int?
                    customPreReminder = (values["customPreReminder"] as String?)?.split('|')?.let { PreReminder(it[0].toInt(), PreReminderUnit.valueOf(it[1])) }
                    isCustomPreReminderSelected = values.getValue("isCustomPreReminderSelected") as Boolean
                    showCustomPreReminderDialog = values.getValue("showCustomPreReminderDialog") as Boolean
                    preReminderExpanded = values.getValue("preReminderExpanded") as Boolean
                    showDeleteConfirmDialog = values.getValue("showDeleteConfirmDialog") as Boolean
                }
            }
        )
    }
}
