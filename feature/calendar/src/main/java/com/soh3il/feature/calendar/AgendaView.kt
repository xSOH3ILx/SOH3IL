package com.soh3il.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soh3il.core.designsystem.components.FarsiBadge
import com.soh3il.core.designsystem.components.FarsiCard
import com.soh3il.core.designsystem.theme.FarsiHolidayRed
import com.soh3il.core.designsystem.theme.FarsiPrimary
import com.soh3il.core.designsystem.utils.toPersianDigits
import com.soh3il.core.jalali.IranianHolidays
import com.soh3il.core.jalali.JalaliDate
import com.soh3il.core.model.Occasion
import com.soh3il.core.model.RecurrenceType
import com.soh3il.core.model.Reminder

data class AgendaDayItem(
    val date: JalaliDate,
    val occasions: List<Occasion>,
    val reminders: List<Reminder>,
    val isToday: Boolean,
    val isTomorrow: Boolean
)

@Composable
fun AgendaView(
    today: JalaliDate,
    reminders: List<Reminder>,
    onReminderClick: (Reminder) -> Unit,
    modifier: Modifier = Modifier
) {
    // Generate agenda items for the next 45 days
    val agendaItems = remember(today, reminders) {
        val list = mutableListOf<AgendaDayItem>()
        val tomorrow = today.plusDays(1)

        for (i in 0..45) {
            val date = today.plusDays(i.toLong())
            val dateStr = date.format("YYYY-MM-DD")
            val monthOccasions = IranianHolidays.getOccasionsForMonth(date.year, date.month)
            val dayOccasions = monthOccasions.filter { it.jalaliDate == dateStr }

            val dayReminders = reminders.filter { rem ->
                rem.jalaliYear == date.year &&
                        rem.jalaliMonth == date.month &&
                        rem.jalaliDay == date.day &&
                        rem.isEnabled
            }

            // Only show days that have reminders, occasions, or are today/tomorrow
            if (dayReminders.isNotEmpty() || dayOccasions.isNotEmpty() || i in 0..2) {
                list.add(
                    AgendaDayItem(
                        date = date,
                        occasions = dayOccasions,
                        reminders = dayReminders,
                        isToday = date == today,
                        isTomorrow = date == tomorrow
                    )
                )
            }
        }
        list
    }

    if (agendaItems.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.EventNote,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "هیچ رویداد یا یادآوری برای روزهای آینده ثبت نشده است",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(agendaItems) { item ->
                AgendaDaySection(
                    item = item,
                    onReminderClick = onReminderClick
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun AgendaDaySection(
    item: AgendaDayItem,
    onReminderClick: (Reminder) -> Unit
) {
    val date = item.date
    val isFriday = date.dayOfWeek == 7
    val hasHoliday = item.occasions.any { it.isHoliday }
    val isDayOff = isFriday || hasHoliday

    Column(modifier = Modifier.fillMaxWidth()) {
        // Section Date Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val tagText = when {
                    item.isToday -> "امروز"
                    item.isTomorrow -> "فردا"
                    else -> null
                }

                if (tagText != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (item.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = tagText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.isToday) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                }

                Text(
                    text = "${date.dayOfWeekNameFa}، ${date.day.toPersianDigits()} ${date.monthNameFa}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDayOff) FarsiHolidayRed else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Occasions under date
        item.occasions.forEach { occ ->
            FarsiBadge(
                text = occ.titleFa,
                isHoliday = occ.isHoliday,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }

        // Reminders under date
        if (item.reminders.isEmpty()) {
            Text(
                text = "بدون یادآور",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        } else {
            item.reminders.forEach { rem ->
                FarsiCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onReminderClick(rem) }
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
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                        ) {
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
                            contentDescription = "ویرایش",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
