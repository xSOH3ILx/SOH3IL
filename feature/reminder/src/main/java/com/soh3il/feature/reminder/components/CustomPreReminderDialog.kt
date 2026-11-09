package com.soh3il.feature.reminder.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
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
import com.soh3il.core.designsystem.utils.toIntStandardOrNull
import com.soh3il.core.designsystem.utils.toPersianDigits
import com.soh3il.core.designsystem.utils.toStandardDigits
import com.soh3il.core.model.PreReminder
import com.soh3il.core.model.PreReminderUnit

@Composable
fun CustomPreReminderDialog(
    initialPreReminder: PreReminder?,
    onDismiss: () -> Unit,
    onConfirm: (PreReminder) -> Unit
) {
    var amountText by remember { mutableStateOf(initialPreReminder?.value?.toString() ?: "2") }
    var selectedUnit by remember { mutableStateOf(initialPreReminder?.unit ?: PreReminderUnit.DAYS) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountText.toIntStandardOrNull() ?: 1
                    if (amount > 0) {
                        onConfirm(PreReminder(amount, selectedUnit))
                    }
                }
            ) {
                Text("تایید و انتخاب", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
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
                        if (input.toStandardDigits().all { it.isDigit() } && input.length <= 4) {
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
