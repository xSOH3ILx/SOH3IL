package com.soh3il.feature.reminder.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soh3il.core.designsystem.components.FarsiCard
import com.soh3il.core.designsystem.utils.toPersianDigits
import com.soh3il.core.model.RecurrenceType

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReminderRecurrenceSection(
    recurrenceType: RecurrenceType,
    recurrenceInterval: Int,
    onRecurrenceTypeChange: (RecurrenceType) -> Unit,
    onCustomizeIntervalClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FarsiCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Repeat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "تکرار بر اساس گاه‌شمار خورشیدی",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }

                if (recurrenceType != RecurrenceType.NONE) {
                    val intervalText = if (recurrenceInterval == 1) "هر دوره" else "هر ${recurrenceInterval.toPersianDigits()} دوره"
                    Text(
                        text = intervalText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val recurrenceOptions = listOf(
                    RecurrenceType.NONE to "بدون تکرار (یکبار)",
                    RecurrenceType.DAILY to "روزانه",
                    RecurrenceType.WEEKLY to "هفتگی",
                    RecurrenceType.MONTHLY_JALALI to "ماهانه شمسی",
                    RecurrenceType.YEARLY_JALALI to "سالانه خورشیدی"
                )

                recurrenceOptions.forEach { (type, label) ->
                    val isSelected = recurrenceType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { onRecurrenceTypeChange(type) },
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

                // Customize Recurrence Interval Button
                if (recurrenceType != RecurrenceType.NONE) {
                    FilterChip(
                        selected = recurrenceInterval > 1,
                        onClick = onCustomizeIntervalClick,
                        label = {
                            val label = if (recurrenceInterval > 1) "بازه: ${recurrenceInterval.toPersianDigits()}" else "تنظیم بازه..."
                            Text(label, fontSize = 12.sp, fontWeight = if (recurrenceInterval > 1) FontWeight.Bold else FontWeight.Normal)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }
    }
}
