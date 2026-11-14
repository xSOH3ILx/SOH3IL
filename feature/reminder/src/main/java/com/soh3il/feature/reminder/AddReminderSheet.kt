package com.soh3il.feature.reminder

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soh3il.core.designsystem.utils.toPersianDigits
import com.soh3il.core.jalali.JalaliDate
import com.soh3il.core.model.PreReminder
import com.soh3il.core.model.PreReminderUnit
import com.soh3il.core.model.RecurrenceRule
import com.soh3il.core.model.RecurrenceType
import com.soh3il.core.model.Reminder
import com.soh3il.core.model.ReminderCategory
import com.soh3il.core.model.ShortMonthPolicy
import com.soh3il.feature.reminder.components.CustomPreReminderDialog
import com.soh3il.feature.reminder.components.CustomRecurrenceDialog
import com.soh3il.feature.reminder.components.DeleteReminderDialog
import com.soh3il.feature.reminder.components.ReminderCategorySection
import com.soh3il.feature.reminder.components.ReminderFormFields
import com.soh3il.feature.reminder.components.ReminderPreNotificationSection
import com.soh3il.feature.reminder.components.ReminderRecurrenceSection
import com.soh3il.feature.reminder.components.ReminderTimeCard
import com.soh3il.feature.reminder.components.ReminderTimePickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderSheet(
    initialDate: JalaliDate = JalaliDate.now(),
    existingReminder: Reminder? = null,
    onDismiss: () -> Unit,
    onSaveReminder: (Reminder) -> Unit,
    onDeleteReminder: ((Reminder) -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val draft = rememberSaveable(existingReminder?.id, saver = ReminderDraftState.Saver) {
        ReminderDraftState(initialDate, existingReminder)
    }
    with(draft) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // 1. Header with Close Button and Jalali Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (existingReminder != null) "ویرایش یادآور" else "افزودن یادآور جدید",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${selectedDate.dayOfWeekNameFa}، ${selectedDate.day.toPersianDigits()} ${selectedDate.monthNameFa} ${selectedDate.year.toPersianDigits()}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "بستن",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Title & Description Input Fields
                ReminderFormFields(
                    title = title,
                    onTitleChange = { title = it },
                    description = description,
                    onDescriptionChange = { description = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Time Picker Card
                ReminderTimeCard(
                    hour = selectedHour,
                    minute = selectedMinute,
                    onPickTimeClick = { showTimePickerDialog = true }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Solar Recurrence Section
                ReminderRecurrenceSection(
                    recurrenceType = recurrenceType,
                    recurrenceInterval = recurrenceInterval,
                    onRecurrenceTypeChange = { type ->
                        recurrenceType = type
                        recurrenceExpanded = true
                        if (type == RecurrenceType.NONE) recurrenceInterval = 1
                    },
                    onCustomizeIntervalClick = {
                        if (recurrenceType == RecurrenceType.NONE) {
                            recurrenceType = RecurrenceType.DAILY
                        }
                        showCustomRecurrenceDialog = true
                    },
                    expanded = recurrenceExpanded,
                    onExpandedChange = { recurrenceExpanded = !recurrenceExpanded }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 5. Pre-Reminder Section
                ReminderPreNotificationSection(
                    selectedPreReminderMinutes = selectedPreReminderMinutes,
                    customPreReminder = customPreReminder,
                    isCustomPreReminderSelected = isCustomPreReminderSelected,
                    onPresetSelected = { mins ->
                        isCustomPreReminderSelected = false
                        selectedPreReminderMinutes = mins
                    },
                    onCustomChipSelected = {
                        isCustomPreReminderSelected = true
                        selectedPreReminderMinutes = null
                    },
                    onCustomizeClick = { showCustomPreReminderDialog = true },
                    expanded = preReminderExpanded,
                    onExpandedChange = { preReminderExpanded = !preReminderExpanded }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 6. Category & Color Section
                ReminderCategorySection(
                    selectedColorHex = selectedColorHex,
                    onCategorySelected = { cat ->
                        selectedColorHex = cat.colorHex
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 7. Save CTA Button (M3 Pill shape)
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val preList = when {
                                isCustomPreReminderSelected && customPreReminder != null -> {
                                    listOf(customPreReminder!!)
                                }
                                selectedPreReminderMinutes != null -> {
                                    val mins = selectedPreReminderMinutes!!
                                    when {
                                        mins % 1440 == 0 -> listOf(PreReminder(mins / 1440, PreReminderUnit.DAYS))
                                        mins % 60 == 0 -> listOf(PreReminder(mins / 60, PreReminderUnit.HOURS))
                                        else -> listOf(PreReminder(mins, PreReminderUnit.MINUTES))
                                    }
                                }
                                else -> emptyList()
                            }

                            val reminder = Reminder(
                                id = existingReminder?.id ?: 0L,
                                title = title.trim(),
                                description = description.trim(),
                                jalaliYear = selectedDate.year,
                                jalaliMonth = selectedDate.month,
                                jalaliDay = selectedDate.day,
                                hour = selectedHour,
                                minute = selectedMinute,
                                recurrence = RecurrenceRule(
                                    type = recurrenceType,
                                    interval = recurrenceInterval.coerceAtLeast(1),
                                    dayOfMonth = if (recurrenceType == RecurrenceType.MONTHLY_JALALI) selectedDate.day else null,
                                    daysOfWeek = existingReminder
                                        ?.takeIf {
                                            recurrenceType == RecurrenceType.WEEKLY &&
                                                it.recurrence.type == RecurrenceType.WEEKLY
                                        }
                                        ?.recurrence
                                        ?.daysOfWeek
                                        ?: emptySet(),
                                    shortMonthPolicy = if (recurrenceType == RecurrenceType.MONTHLY_GREGORIAN || recurrenceType == RecurrenceType.YEARLY_GREGORIAN) {
                                        ShortMonthPolicy.SKIP_MONTH
                                    } else existingReminder?.recurrence?.shortMonthPolicy ?: ShortMonthPolicy.SNAP_TO_LAST_DAY
                                ),
                                preReminders = preList,
                                isEnabled = true,
                                colorHex = selectedColorHex
                            )
                            onSaveReminder(reminder)
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = title.isNotBlank(),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (existingReminder != null) "ذخیره تغییرات" else "ثبت و زمان‌بندی یادآور",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                // Delete Button (only in edit mode)
                if (existingReminder != null && onDeleteReminder != null) {
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("حذف این یادآور", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Interactive TimePicker Dialog
        if (showTimePickerDialog) {
            ReminderTimePickerDialog(
                initialHour = selectedHour,
                initialMinute = selectedMinute,
                onDismiss = { showTimePickerDialog = false },
                onConfirm = { h, m ->
                    selectedHour = h
                    selectedMinute = m
                    showTimePickerDialog = false
                }
            )
        }

        // Custom Pre-Reminder Dialog
        if (showCustomPreReminderDialog) {
            CustomPreReminderDialog(
                initialPreReminder = customPreReminder,
                onDismiss = { showCustomPreReminderDialog = false },
                onConfirm = { pre ->
                    customPreReminder = pre
                    isCustomPreReminderSelected = true
                    selectedPreReminderMinutes = null
                    showCustomPreReminderDialog = false
                }
            )
        }

        // Custom Recurrence Interval Dialog
        if (showCustomRecurrenceDialog) {
            CustomRecurrenceDialog(
                recurrenceType = recurrenceType,
                initialInterval = recurrenceInterval,
                initialDate = selectedDate,
                onDismiss = { showCustomRecurrenceDialog = false },
                onConfirm = { interval ->
                    recurrenceInterval = interval
                    showCustomRecurrenceDialog = false
                }
            )
        }

        // Delete Confirmation Dialog
        if (showDeleteConfirmDialog && existingReminder != null && onDeleteReminder != null) {
            DeleteReminderDialog(
                reminder = existingReminder,
                onDismiss = { showDeleteConfirmDialog = false },
                onConfirmDelete = {
                    showDeleteConfirmDialog = false
                    onDeleteReminder(existingReminder)
                    onDismiss()
                }
            )
        }
    }
}
