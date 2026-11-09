package com.soh3il.feature.calendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soh3il.core.designsystem.components.FarsiBadge
import com.soh3il.core.designsystem.components.FarsiCard
import com.soh3il.core.designsystem.theme.FarsiPrimary
import com.soh3il.core.designsystem.utils.toPersianDigits
import com.soh3il.core.jalali.JalaliDate
import com.soh3il.core.model.Occasion
import com.soh3il.core.model.RecurrenceType
import com.soh3il.core.model.Reminder

@Composable
fun SelectedDayDetails(
    selectedDate: JalaliDate,
    occasions: List<Occasion>,
    reminders: List<Reminder>,
    onReminderClick: (Reminder) -> Unit,
    onAddReminderClick: ((JalaliDate) -> Unit)? = null
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selectedDate.dayOfWeekNameFa}، ${selectedDate.day.toPersianDigits()} ${selectedDate.monthNameFa}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (onAddReminderClick != null) {
                    TextButton(
                        onClick = { onAddReminderClick(selectedDate) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "افزودن یادآور",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "یادآور جدید",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Occasions list
        if (occasions.isNotEmpty()) {
            items(occasions) { occ ->
                FarsiBadge(
                    text = occ.titleFa,
                    isHoliday = occ.isHoliday,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        }

        // Reminders list
        if (reminders.isNotEmpty()) {
            item {
                Text(
                    text = "یادآورها",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }
            items(reminders) { rem ->
                FarsiCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    onClick = { onReminderClick(rem) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(FarsiPrimary)
                        )
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(
                                text = rem.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val timeStr = "\u200E${rem.hour.toPersianDigits().padStart(2, '۰')}:${rem.minute.toPersianDigits().padStart(2, '۰')}"
                            val recurrenceDetail = when (rem.recurrence.type) {
                                RecurrenceType.NONE -> null
                                RecurrenceType.DAILY -> if (rem.recurrence.interval <= 1) "روزانه" else "هر ${rem.recurrence.interval.toPersianDigits()} روز یکبار"
                                RecurrenceType.WEEKLY -> if (rem.recurrence.interval <= 1) "هفتگی" else "هر ${rem.recurrence.interval.toPersianDigits()} هفته یکبار"
                                RecurrenceType.MONTHLY_JALALI -> if (rem.recurrence.interval <= 1) "ماهانه" else "هر ${rem.recurrence.interval.toPersianDigits()} ماه یکبار"
                                RecurrenceType.YEARLY_JALALI -> if (rem.recurrence.interval <= 1) "سالانه" else "هر ${rem.recurrence.interval.toPersianDigits()} سال یکبار"
                            }
                            val subtitle = if (recurrenceDetail != null) "ساعت $timeStr • $recurrenceDetail" else "ساعت $timeStr"
                            Text(
                                text = subtitle,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "ویرایش یادآور",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
