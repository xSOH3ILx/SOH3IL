package com.soh3il.feature.reminder

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soh3il.core.designsystem.components.FarsiCard
import com.soh3il.core.designsystem.utils.toPersianDigits
import com.soh3il.core.jalali.JalaliDate
import com.soh3il.core.model.PreReminder
import com.soh3il.core.model.PreReminderUnit
import com.soh3il.core.model.RecurrenceRule
import com.soh3il.core.model.RecurrenceType
import com.soh3il.core.model.Reminder

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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

            // 2. Title Input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان یادآور") },
                placeholder = { Text("مثال: پرداخت قسط، جلسه کاری...") },
                leadingIcon = {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Description Input
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("توضیحات (اختیاری)") },
                leadingIcon = {
                    Icon(Icons.Default.Notes, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Interactive Time Selector Card
            FarsiCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "زمان یادآوری:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }

                        val timeFormatted = "\u200E${selectedHour.toPersianDigits().padStart(2, '۰')}:${selectedMinute.toPersianDigits().padStart(2, '۰')}"
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showTimePickerDialog = true },
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = timeFormatted,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Time Preset Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val presets = listOf(
                            Pair(8, 0) to "۰۸:۰۰",
                            Pair(12, 0) to "۱۲:۰۰",
                            Pair(16, 0) to "۱۶:۰۰",
                            Pair(20, 0) to "۲۰:۰۰"
                        )
                        presets.forEach { (time, label) ->
                            val isSelected = selectedHour == time.first && selectedMinute == time.second
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedHour = time.first
                                    selectedMinute = time.second
                                },
                                label = { Text(label, fontSize = 12.sp) },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedButton(
                        onClick = { showTimePickerDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("انتخاب ساعت دقیق (تنظیم دستی)", fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Jalali Recurrence Section (Polished Chips with Icons & Visual States)
            FarsiCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Repeat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "تکرار بر اساس گاه‌شمار شمسی",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val types = listOf(
                            RecurrenceType.NONE to "بدون تکرار (یکباره)",
                            RecurrenceType.DAILY to "روزانه",
                            RecurrenceType.WEEKLY to "هفتگی",
                            RecurrenceType.MONTHLY_JALALI to "ماهانه (${initialDate.day.toPersianDigits()} هر ماه شمسی)",
                            RecurrenceType.YEARLY_JALALI to "سالانه (${initialDate.day.toPersianDigits()} ${initialDate.monthNameFa})"
                        )

                        types.forEach { (type, label) ->
                            val isSelected = recurrenceType == type
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    recurrenceType = type
                                    recurrenceInterval = 1
                                },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // 5.1 Interval Selection when recurring
                    if (recurrenceType != RecurrenceType.NONE) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "بازه تکرار:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val presetIntervals = when (recurrenceType) {
                                RecurrenceType.MONTHLY_JALALI -> listOf(
                                    1 to "هر ماه",
                                    2 to "هر ۲ ماه یکبار",
                                    3 to "هر ۳ ماه (فصلی)",
                                    6 to "هر ۶ ماه (نیم‌سال)"
                                )
                                RecurrenceType.DAILY -> listOf(
                                    1 to "هر روز",
                                    2 to "یک روز در میان",
                                    3 to "هر ۳ روز",
                                    7 to "هر هفته (۷ روز)"
                                )
                                RecurrenceType.WEEKLY -> listOf(
                                    1 to "هر هفته",
                                    2 to "هر ۲ هفته یکبار",
                                    3 to "هر ۳ هفته یکبار",
                                    4 to "هر ۴ هفته یکبار"
                                )
                                RecurrenceType.YEARLY_JALALI -> listOf(
                                    1 to "هر سال",
                                    2 to "هر ۲ سال یکبار"
                                )
                                else -> emptyList()
                            }

                            presetIntervals.forEach { (interval, label) ->
                                val isSelected = recurrenceInterval == interval
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { recurrenceInterval = interval },
                                    label = {
                                        Text(
                                            text = label,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            // Custom interval chip
                            val isPreset = presetIntervals.any { it.first == recurrenceInterval }
                            FilterChip(
                                selected = !isPreset,
                                onClick = { showCustomRecurrenceDialog = true },
                                label = {
                                    Text(
                                        text = if (!isPreset) {
                                            val unitName = when (recurrenceType) {
                                                RecurrenceType.DAILY -> "روز"
                                                RecurrenceType.WEEKLY -> "هفته"
                                                RecurrenceType.MONTHLY_JALALI -> "ماه شمسی"
                                                RecurrenceType.YEARLY_JALALI -> "سال شمسی"
                                                else -> ""
                                            }
                                            "هر ${recurrenceInterval.toPersianDigits()} $unitName یکبار"
                                        } else {
                                            "+ بازه دلخواه..."
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = if (!isPreset) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        if (!isPreset) Icons.Default.Check else Icons.Default.Tune,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. Pre-reminder selector (Presets + Custom Choice)
            FarsiCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "پیش‌یادآور (اعلان قبل از زمان اصلی)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val preOptions = listOf(
                            null to "بدون پیش‌یادآور",
                            15 to "۱۵ دقیقه قبل",
                            30 to "۳۰ دقیقه قبل",
                            60 to "۱ ساعت قبل",
                            1440 to "۱ روز قبل",
                            2880 to "۲ روز قبل"
                        )

                        preOptions.forEach { (mins, label) ->
                            val isSelected = !isCustomPreReminderSelected && selectedPreReminderMinutes == mins
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    isCustomPreReminderSelected = false
                                    selectedPreReminderMinutes = mins
                                },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // Custom Pre-reminder Chip
                        if (customPreReminder != null) {
                            val unitName = when (customPreReminder!!.unit) {
                                PreReminderUnit.DAYS -> "روز"
                                PreReminderUnit.HOURS -> "ساعت"
                                PreReminderUnit.MINUTES -> "دقیقه"
                            }
                            val customLabel = "${customPreReminder!!.value.toPersianDigits()} $unitName قبل (سفارشی)"

                            FilterChip(
                                selected = isCustomPreReminderSelected,
                                onClick = {
                                    isCustomPreReminderSelected = true
                                    selectedPreReminderMinutes = null
                                },
                                label = {
                                    Text(
                                        text = customLabel,
                                        fontSize = 12.sp,
                                        fontWeight = if (isCustomPreReminderSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = if (isCustomPreReminderSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "ویرایش",
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { showCustomPreReminderDialog = true }
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        // Button to add / customize
                        FilterChip(
                            selected = false,
                            onClick = { showCustomPreReminderDialog = true },
                            label = { Text("+ زمان دلخواه...", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

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
                            isEnabled = true
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
                var showDeleteConfirmDialog by remember { mutableStateOf(false) }

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

                if (showDeleteConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirmDialog = false },
                        title = { Text("حذف یادآور", fontWeight = FontWeight.Bold) },
                        text = { Text("آیا از حذف یادآور «${existingReminder.title}» مطمئن هستید؟ این عمل غیرقابل بازگشت است.") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showDeleteConfirmDialog = false
                                    onDeleteReminder(existingReminder)
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("بله، حذف شود", fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirmDialog = false }) {
                                Text("انصراف")
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Interactive TimePicker Dialog
    if (showTimePickerDialog) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            is24Hour = true
        )

        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedHour = timePickerState.hour
                        selectedMinute = timePickerState.minute
                        showTimePickerDialog = false
                    }
                ) {
                    Text("تایید زمان", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = false }) {
                    Text("انصراف")
                }
            },
            title = {
                Text(
                    text = "تنظیم ساعت و دقیقه",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            }
        )
    }

    // Custom Pre-Reminder Dialog
    if (showCustomPreReminderDialog) {
        var amountText by remember { mutableStateOf(customPreReminder?.value?.toString() ?: "2") }
        var selectedUnit by remember { mutableStateOf(customPreReminder?.unit ?: PreReminderUnit.DAYS) }

        AlertDialog(
            onDismissRequest = { showCustomPreReminderDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amount = amountText.toIntOrNull() ?: 1
                        if (amount > 0) {
                            customPreReminder = PreReminder(amount, selectedUnit)
                            isCustomPreReminderSelected = true
                            selectedPreReminderMinutes = null
                            showCustomPreReminderDialog = false
                        }
                    }
                ) {
                    Text("تایید و انتخاب", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomPreReminderDialog = false }) {
                    Text("انصراف")
                }
            },
            title = {
                Text(
                    text = "تنظیم پیش‌یادآور دلخواه",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "چه مدت قبل از زمان یادآور اصلی به شما هشدار داده شود؟",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 4) {
                                amountText = input
                            }
                        },
                        label = { Text("مقدار زمان") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "واحد زمانی:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val units = listOf(
                            PreReminderUnit.MINUTES to "دقیقه",
                            PreReminderUnit.HOURS to "ساعت",
                            PreReminderUnit.DAYS to "روز"
                        )
                        units.forEach { (unit, name) ->
                            FilterChip(
                                selected = selectedUnit == unit,
                                onClick = { selectedUnit = unit },
                                label = { Text(name, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val amountVal = amountText.toIntOrNull() ?: 0
                    val unitStr = when (selectedUnit) {
                        PreReminderUnit.DAYS -> "روز"
                        PreReminderUnit.HOURS -> "ساعت"
                        PreReminderUnit.MINUTES -> "دقیقه"
                    }
                    Text(
                        text = "پیش‌نمایش: ${amountVal.toPersianDigits()} $unitStr قبل از موعد",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        )
    }

    // 10. Custom Recurrence Interval Dialog
    if (showCustomRecurrenceDialog) {
        var intervalInput by remember { mutableStateOf(recurrenceInterval.toString()) }
        val unitLabel = when (recurrenceType) {
            RecurrenceType.DAILY -> "روز"
            RecurrenceType.WEEKLY -> "هفته"
            RecurrenceType.MONTHLY_JALALI -> "ماه شمسی"
            RecurrenceType.YEARLY_JALALI -> "سال شمسی"
            else -> "دوره"
        }

        AlertDialog(
            onDismissRequest = { showCustomRecurrenceDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = intervalInput.toIntOrNull()
                        if (parsed != null && parsed > 0) {
                            recurrenceInterval = parsed
                            showCustomRecurrenceDialog = false
                        }
                    }
                ) {
                    Text("تایید بازه تکرار", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomRecurrenceDialog = false }) {
                    Text("انصراف")
                }
            },
            title = {
                Text(
                    text = "تنظیم بازه تکرار دلخواه",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "این یادآور هر چند $unitLabel یک‌بار تکرار شود؟",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = intervalInput,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() } && input.length <= 3) {
                                intervalInput = input
                            }
                        },
                        label = { Text("عدد بازه تکرار (به $unitLabel)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val valInt = intervalInput.toIntOrNull() ?: 1
                    val previewDetail = when (recurrenceType) {
                        RecurrenceType.MONTHLY_JALALI -> "هر ${valInt.toPersianDigits()} ماه شمسی در تاریخ ${initialDate.day.toPersianDigits()} هر ماه"
                        RecurrenceType.DAILY -> "هر ${valInt.toPersianDigits()} روز یکبار"
                        RecurrenceType.WEEKLY -> "هر ${valInt.toPersianDigits()} هفته یکبار"
                        RecurrenceType.YEARLY_JALALI -> "هر ${valInt.toPersianDigits()} سال یکبار در ${initialDate.day.toPersianDigits()} ${initialDate.monthNameFa}"
                        else -> ""
                    }

                    Text(
                        text = "پیش‌نمایش: تکرار $previewDetail",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        )
    }
}
