package com.soh3il.feature.reminder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddReminderSheet(
    initialDate: JalaliDate = JalaliDate.now(),
    onDismiss: () -> Unit,
    onSaveReminder: (Reminder) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedHour by remember { mutableIntStateOf(9) }
    var selectedMinute by remember { mutableIntStateOf(0) }

    var recurrenceType by remember { mutableStateOf(RecurrenceType.NONE) }
    var selectedPreReminderMinutes by remember { mutableStateOf<Int?>(null) } // null = none, 15, 30, 60, 1440

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Text(
                text = "افزودن یادآور جدید",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("عنوان یادآور") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("توضیحات (اختیاری)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Jalali Recurrence Section
            Text(
                text = "نوع تکرار شمسی",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val types = listOf(
                    RecurrenceType.NONE to "بدون تکرار",
                    RecurrenceType.DAILY to "روزانه",
                    RecurrenceType.WEEKLY to "هفتگی",
                    RecurrenceType.MONTHLY_JALALI to "ماهانه (${initialDate.day.toPersianDigits()} هر ماه شمسی)",
                    RecurrenceType.YEARLY_JALALI to "سالانه (${initialDate.day.toPersianDigits()} ${initialDate.monthNameFa})"
                )

                types.forEach { (type, label) ->
                    FilterChip(
                        selected = recurrenceType == type,
                        onClick = { recurrenceType = type },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pre-reminder selector
            Text(
                text = "پیش‌یادآور (اعلان قبل از زمان اصلی)",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val preOptions = listOf(
                    null to "بدون پیش‌یادآور",
                    15 to "۱۵ دقیقه قبل",
                    30 to "۳۰ دقیقه قبل",
                    60 to "۱ ساعت قبل",
                    1440 to "۱ روز قبل"
                )

                preOptions.forEach { (mins, label) ->
                    FilterChip(
                        selected = selectedPreReminderMinutes == mins,
                        onClick = { selectedPreReminderMinutes = mins },
                        label = { Text(label, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val preList = if (selectedPreReminderMinutes != null) {
                            val mins = selectedPreReminderMinutes!!
                            if (mins >= 1440) {
                                listOf(PreReminder(mins / 1440, PreReminderUnit.DAYS))
                            } else if (mins >= 60) {
                                listOf(PreReminder(mins / 60, PreReminderUnit.HOURS))
                            } else {
                                listOf(PreReminder(mins, PreReminderUnit.MINUTES))
                            }
                        } else emptyList()

                        val reminder = Reminder(
                            title = title.trim(),
                            description = description.trim(),
                            jalaliYear = initialDate.year,
                            jalaliMonth = initialDate.month,
                            jalaliDay = initialDate.day,
                            hour = selectedHour,
                            minute = selectedMinute,
                            recurrence = RecurrenceRule(
                                type = recurrenceType,
                                dayOfMonth = if (recurrenceType == RecurrenceType.MONTHLY_JALALI) initialDate.day else null
                            ),
                            preReminders = preList
                        )
                        onSaveReminder(reminder)
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("ذخیره یادآور", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
