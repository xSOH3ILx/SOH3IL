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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    var title by remember { mutableStateOf(existingReminder?.title ?: "") }
    var description by remember { mutableStateOf(existingReminder?.description ?: "") }
    var selectedHour by remember { mutableIntStateOf(existingReminder?.hour ?: 9) }
    var selectedMinute by remember { mutableIntStateOf(existingReminder?.minute ?: 0) }
    var selectedColorHex by remember { mutableStateOf(existingReminder?.colorHex ?: "#2563EB") }
    var showTimePickerDialog by remember { mutableStateOf(false) }

    var recurrenceType by remember { mutableStateOf(existingReminder?.recurrence?.type ?: RecurrenceType.NONE) }
    var recurrenceInterval by remember { mutableIntStateOf(existingReminder?.recurrence?.interval ?: 1) }
    var showCustomRecurrenceDialog by remember { mutableStateOf(false) }

    // Pre-reminder state: presets or custom
    val initialPreMinutes = existingReminder?.preReminders?.firstOrNull()?.toMinutes()?.toInt()
    var selectedPreReminderMinutes by remember {
        mutableStateOf(
            if (initialPreMinutes != null && initialPreMinutes in listOf(15, 30, 60, 1440, 2880)) initialPreMinutes
            else null
        )
    }
    var customPreReminder by remember {
        mutableStateOf(
            if (initialPreMinutes != null && initialPreMinutes !in listOf(15, 30, 60, 1440, 2880)) existingReminder.preReminders.firstOrNull()
            else null
        )
    }
    var isCustomPreReminderSelected by remember { mutableStateOf(customPreReminder != null) }
    var showCustomPreReminderDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

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
                        text = "${initialDate.dayOfWeekNameFa}، ${initialDate.day.toPersianDigits()} ${initialDate.monthNameFa} ${initialDate.year.toPersianDigits()}",
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
                    if (type == RecurrenceType.NONE) recurrenceInterval = 1
                },
                onCustomizeIntervalClick = { showCustomRecurrenceDialog = true }
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
                onCustomizeClick = { showCustomPreReminderDialog = true }
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
                            jalaliYear = existingReminder?.jalaliYear ?: initialDate.year,
                            jalaliMonth = existingReminder?.jalaliMonth ?: initialDate.month,
                            jalaliDay = existingReminder?.jalaliDay ?: initialDate.day,
                            hour = selectedHour,
                            minute = selectedMinute,
                            recurrence = RecurrenceRule(
                                type = recurrenceType,
                                interval = recurrenceInterval.coerceAtLeast(1),
                                dayOfMonth = if (recurrenceType == RecurrenceType.MONTHLY_JALALI) (existingReminder?.jalaliDay ?: initialDate.day) else null
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
            initialDate = initialDate,
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
