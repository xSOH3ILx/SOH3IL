package com.soh3il.feature.reminder.components

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Edit
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
import com.soh3il.core.model.PreReminder
import com.soh3il.core.model.PreReminderUnit

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReminderPreNotificationSection(
    selectedPreReminderMinutes: Int?,
    customPreReminder: PreReminder?,
    isCustomPreReminderSelected: Boolean,
    onPresetSelected: (Int?) -> Unit,
    onCustomChipSelected: () -> Unit,
    onCustomizeClick: () -> Unit,
    expanded: Boolean,
    onExpandedChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    val summary = when {
        isCustomPreReminderSelected && customPreReminder != null -> {
            val unit = when (customPreReminder.unit) {
                PreReminderUnit.MINUTES -> "دقیقه"
                PreReminderUnit.HOURS -> "ساعت"
                PreReminderUnit.DAYS -> "روز"
            }
            "${customPreReminder.value.toPersianDigits()} $unit قبل"
        }
        selectedPreReminderMinutes == null -> "بدون پیش‌یادآور"
        selectedPreReminderMinutes < 60 -> "${selectedPreReminderMinutes.toPersianDigits()} دقیقه قبل"
        selectedPreReminderMinutes % 1440 == 0 -> "${(selectedPreReminderMinutes / 1440).toPersianDigits()} روز قبل"
        else -> "${(selectedPreReminderMinutes / 60).toPersianDigits()} ساعت قبل"
    }

    FarsiAccordion(
        title = "پیش‌یادآور",
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
                        onClick = { onPresetSelected(mins) },
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
                    val unitName = when (customPreReminder.unit) {
                        PreReminderUnit.DAYS -> "روز"
                        PreReminderUnit.HOURS -> "ساعت"
                        PreReminderUnit.MINUTES -> "دقیقه"
                    }
                    val customLabel = "${customPreReminder.value.toPersianDigits()} $unitName قبل (سفارشی)"

                    FilterChip(
                        selected = isCustomPreReminderSelected,
                        onClick = onCustomChipSelected,
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
                                    .clickable { onCustomizeClick() }
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
                    onClick = onCustomizeClick,
                    label = { Text("+ زمان دلخواه...", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    shape = RoundedCornerShape(12.dp)
                )
            }
    }
}
