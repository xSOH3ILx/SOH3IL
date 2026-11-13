package com.soh3il.feature.calendar.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soh3il.core.designsystem.components.FarsiBadge
import com.soh3il.core.designsystem.components.FarsiCard
import com.soh3il.core.designsystem.theme.FarsiPrimary
import com.soh3il.core.designsystem.utils.toPersianDigits
import com.soh3il.core.jalali.DateConverter
import com.soh3il.core.jalali.IranianHolidays
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
    onAddReminderClick: ((JalaliDate) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val conv = remember(selectedDate) {
        DateConverter.fromJalali(selectedDate.year, selectedDate.month, selectedDate.day)
    }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, "$label در کلیپ‌بورد کپی شد", Toast.LENGTH_SHORT).show()
    }

    val shamsiFull = "${selectedDate.day.toPersianDigits()} ${selectedDate.monthNameFa} ${selectedDate.year.toPersianDigits()}"
    val miladiMonthName = DateConverter.GREGORIAN_MONTHS_FA.getOrElse(conv.gregorian.monthValue - 1) { "" }
    val miladiFull = "${conv.gregorian.dayOfMonth.toPersianDigits()} $miladiMonthName ${conv.gregorian.year.toPersianDigits()}"
    val hijriMonthName = DateConverter.HIJRI_MONTHS_FA.getOrElse(conv.hijriMonth - 1) { "" }
    val hijriFull = "${conv.hijriDay.toPersianDigits()} $hijriMonthName ${conv.hijriYear.toPersianDigits()}"

    val lunarDataMissing = !IranianHolidays.hasOfficialLunarHolidays(selectedDate.year)

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
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

        if (lunarDataMissing) {
            item {
                Text(
                    text = "تعطیلات قمری سال ${selectedDate.year.toPersianDigits()} هنوز در برنامه ثبت نشده است. قرمز نبودن یک روز به معنی عادی بودن آن نیست.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        // 3-Column Triple-Calendar Tonal Chip Row with one-tap copy
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Shamsi Chip
                TripleDateCard(
                    title = "خورشیدی",
                    value = shamsiFull,
                    onClick = { copyToClipboard("تاریخ خورشیدی", shamsiFull) },
                    modifier = Modifier.weight(1f)
                )

                // Miladi Chip
                TripleDateCard(
                    title = "میلادی",
                    value = miladiFull,
                    onClick = { copyToClipboard("تاریخ میلادی", miladiFull) },
                    modifier = Modifier.weight(1f)
                )

                // Qamari Chip
                TripleDateCard(
                    title = "قمری محاسباتی",
                    value = hijriFull,
                    onClick = { copyToClipboard("تاریخ قمری", hijriFull) },
                    modifier = Modifier.weight(1f)
                )
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
                        val indicatorColor = try {
                            androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(rem.colorHex))
                        } catch (_: Exception) {
                            FarsiPrimary
                        }
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(indicatorColor)
                        )
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(
                                text = rem.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (rem.isEnabled) 1f else 0.55f),
                                textDecoration = if (rem.isEnabled) null else TextDecoration.LineThrough
                            )
                            val timeStr = "\u200E${rem.hour.toPersianDigits().padStart(2, '۰')}:${rem.minute.toPersianDigits().padStart(2, '۰')}"
                            val recurrenceDetail = when (rem.recurrence.type) {
                                RecurrenceType.NONE -> null
                                RecurrenceType.DAILY -> if (rem.recurrence.interval <= 1) "روزانه" else "هر ${rem.recurrence.interval.toPersianDigits()} روز یکبار"
                                RecurrenceType.WEEKLY -> if (rem.recurrence.interval <= 1) "هفتگی" else "هر ${rem.recurrence.interval.toPersianDigits()} هفته یکبار"
                                RecurrenceType.MONTHLY_JALALI -> if (rem.recurrence.interval <= 1) "ماهانه" else "هر ${rem.recurrence.interval.toPersianDigits()} ماه یکبار"
                                RecurrenceType.YEARLY_JALALI -> if (rem.recurrence.interval <= 1) "سالانه" else "هر ${rem.recurrence.interval.toPersianDigits()} سال یکبار"
                            }
                            val doneLabel = if (rem.isEnabled) null else "انجام‌شده"
                            val subtitle = listOfNotNull(
                                "ساعت $timeStr",
                                recurrenceDetail,
                                doneLabel
                            ).joinToString(" • ")
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

@Composable
private fun TripleDateCard(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable { onClick() }
            .heightIn(min = 48.dp)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(3.dp))
            Icon(
                Icons.Default.ContentCopy,
                contentDescription = "کپی",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(11.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
