package com.soh3il.feature.reminder.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import com.soh3il.core.designsystem.components.FarsiAccordion
import com.soh3il.core.designsystem.utils.toPersianDigits
import com.soh3il.core.model.RecurrenceType

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReminderRecurrenceSection(
    recurrenceType: RecurrenceType,
    recurrenceInterval: Int,
    onRecurrenceTypeChange: (RecurrenceType) -> Unit,
    onCustomizeIntervalClick: () -> Unit,
    expanded: Boolean,
    onExpandedChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    val summary = when (recurrenceType) {
        RecurrenceType.NONE -> "بدون تکرار"
        RecurrenceType.DAILY -> "هر ${recurrenceInterval.toPersianDigits()} روز"
        RecurrenceType.WEEKLY -> "هر ${recurrenceInterval.toPersianDigits()} هفته"
        RecurrenceType.MONTHLY_JALALI -> "هر ${recurrenceInterval.toPersianDigits()} ماه خورشیدی"
        RecurrenceType.YEARLY_JALALI -> "هر ${recurrenceInterval.toPersianDigits()} سال خورشیدی"
        RecurrenceType.MONTHLY_GREGORIAN -> "هر ${recurrenceInterval.toPersianDigits()} ماه میلادی"
        RecurrenceType.YEARLY_GREGORIAN -> "هر ${recurrenceInterval.toPersianDigits()} سال میلادی"
    }

    FarsiAccordion(
        title = "تکرار یادآور",
        summary = summary,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = modifier.fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.height(4.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val recurrenceOptions = listOf(
                    RecurrenceType.NONE to "بدون تکرار (یکبار)",
                    RecurrenceType.DAILY to "روزانه",
                    RecurrenceType.WEEKLY to "هفتگی",
                    RecurrenceType.MONTHLY_JALALI to "ماهانه خورشیدی",
                    RecurrenceType.YEARLY_JALALI to "سالانه خورشیدی",
                    RecurrenceType.MONTHLY_GREGORIAN to "ماهانه میلادی",
                    RecurrenceType.YEARLY_GREGORIAN to "سالانه میلادی"
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

                FilterChip(
                    selected = recurrenceInterval > 1,
                    onClick = onCustomizeIntervalClick,
                    label = {
                        val label = if (recurrenceInterval > 1) {
                            "تکرار دلخواه: ${recurrenceInterval.toPersianDigits()}"
                        } else {
                            "تکرار دلخواه"
                        }
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
