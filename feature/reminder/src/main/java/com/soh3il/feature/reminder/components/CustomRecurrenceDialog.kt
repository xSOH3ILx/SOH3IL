package com.soh3il.feature.reminder.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soh3il.core.designsystem.utils.toPersianDigits
import com.soh3il.core.jalali.JalaliDate
import com.soh3il.core.model.RecurrenceType

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomRecurrenceDialog(
    recurrenceType: RecurrenceType,
    initialInterval: Int,
    initialDate: JalaliDate,
    onDismiss: () -> Unit,
    onConfirm: (interval: Int) -> Unit
) {
    var intervalInput by remember { mutableStateOf(initialInterval.toString()) }
    val unitLabel = when (recurrenceType) {
        RecurrenceType.DAILY -> "روز"
        RecurrenceType.WEEKLY -> "هفته"
        RecurrenceType.MONTHLY_JALALI -> "ماه شمسی"
        RecurrenceType.YEARLY_JALALI -> "سال شمسی"
        else -> "دوره"
    }

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
            2 to "هر ۲ سال یکبار",
            3 to "هر ۳ سال یکبار"
        )
        else -> emptyList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val parsed = intervalInput.toIntOrNull()
                    if (parsed != null && parsed > 0) {
                        onConfirm(parsed)
                    }
                }
            ) {
                Text("تایید بازه تکرار", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        },
        title = {
            Text(
                text = "تنظیم بازه تکرار گاه‌شمار",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column {
                Text(
                    text = "انتخاب بازه‌های متداول یا وارد کردن عدد دلخواه:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (presetIntervals.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetIntervals.forEach { (presetVal, presetName) ->
                            val isChosen = intervalInput == presetVal.toString()
                            FilterChip(
                                selected = isChosen,
                                onClick = { intervalInput = presetVal.toString() },
                                label = {
                                    Text(
                                        text = presetName,
                                        fontSize = 12.sp,
                                        fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = intervalInput,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() } && input.length <= 3) {
                            intervalInput = input
                        }
                    },
                    label = { Text("عدد بازه دلخواه (به $unitLabel)") },
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
