package com.soh3il.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soh3il.core.designsystem.components.FarsiBadge
import com.soh3il.core.designsystem.components.FarsiCard
import com.soh3il.core.designsystem.theme.FarsiHolidayRed
import com.soh3il.core.designsystem.theme.FarsiPrimary
import com.soh3il.core.designsystem.utils.toPersianDigits
import com.soh3il.core.jalali.JalaliDate
import com.soh3il.core.model.Occasion
import com.soh3il.core.model.Reminder

@Composable
fun CalendarScreen(
    reminders: List<Reminder> = emptyList(),
    occasions: List<Occasion> = emptyList(),
    onAddReminderClick: () -> Unit = {},
    onReminderClick: (Reminder) -> Unit = {}
) {
    val today = remember { JalaliDate.now() }
    var displayedDate by remember { mutableStateOf(today) }
    var selectedDate by remember { mutableStateOf(today) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddReminderClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "افزودن یادآور")
            }
        },
        floatingActionButtonPosition = FabPosition.Start
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1. Month Header (Centered Month, Left=Previous, Right=Next)
            CalendarHeader(
                currentDate = displayedDate,
                onPreviousMonth = { displayedDate = displayedDate.plusMonths(-1) },
                onNextMonth = { displayedDate = displayedDate.plusMonths(1) },
                onTodayClick = {
                    displayedDate = today
                    selectedDate = today
                }
            )

            // 2. Weekdays bar
            WeekdaysRow()

            // 3. Days Grid with Horizontal Swipe gesture
            // Swipe Left-to-Right: Next month | Swipe Right-to-Left: Previous month
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(displayedDate) {
                        detectHorizontalDragGestures(
                            onDragStart = { totalDragDistance = 0f },
                            onDragEnd = {
                                val threshold = 40.dp.toPx()
                                if (totalDragDistance > threshold) {
                                    // Swiped Left to Right -> Next month
                                    displayedDate = displayedDate.plusMonths(1)
                                } else if (totalDragDistance < -threshold) {
                                    // Swiped Right to Left -> Previous month
                                    displayedDate = displayedDate.plusMonths(-1)
                                }
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                totalDragDistance += dragAmount
                            }
                        )
                    }
            ) {
                MonthGridView(
                    year = displayedDate.year,
                    month = displayedDate.month,
                    selectedDate = selectedDate,
                    today = today,
                    reminders = reminders,
                    occasions = occasions,
                    onDateSelected = { selectedDate = it }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Selected Day Info & Occasions & Reminders
            SelectedDayDetails(
                selectedDate = selectedDate,
                occasions = occasions.filter { it.jalaliDate == selectedDate.format("YYYY-MM-DD") },
                reminders = reminders.filter {
                    it.jalaliYear == selectedDate.year &&
                            it.jalaliMonth == selectedDate.month &&
                            it.jalaliDay == selectedDate.day
                },
                onReminderClick = onReminderClick
            )
        }
    }
}

@Composable
fun CalendarHeader(
    currentDate: JalaliDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Right side of screen (Start in RTL): Next Month button (arrow points right)
        IconButton(onClick = onNextMonth) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "ماه بعد",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // Middle: Month name & year centered, plus "امروز" chip
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "${currentDate.monthNameFa} ${currentDate.year.toPersianDigits()}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onTodayClick() },
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "امروز",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Left side of screen (End in RTL): Previous Month button (arrow points left)
        IconButton(onClick = onPreviousMonth) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "ماه قبل",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun WeekdaysRow() {
    val weekdays = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        weekdays.forEachIndexed { index, name ->
            val isFriday = index == 6
            Text(
                text = name,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isFriday) FarsiHolidayRed else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun MonthGridView(
    year: Int,
    month: Int,
    selectedDate: JalaliDate,
    today: JalaliDate,
    reminders: List<Reminder>,
    occasions: List<Occasion>,
    onDateSelected: (JalaliDate) -> Unit
) {
    val firstDayOfMonth = JalaliDate(year, month, 1)
    val startingDayOfWeek = firstDayOfMonth.dayOfWeek // 1=Shanbeh to 7=Jomeh
    val daysInMonth = JalaliDate.getDaysInMonth(year, month)

    // Leading empty slots
    val emptySlots = startingDayOfWeek - 1
    val totalCells = emptySlots + daysInMonth

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        items(totalCells) { index ->
            if (index >= emptySlots) {
                val dayNumber = (index - emptySlots) + 1
                val cellDate = JalaliDate(year, month, dayNumber)
                val isSelected = cellDate == selectedDate
                val isToday = cellDate == today
                val dateStr = cellDate.format("YYYY-MM-DD")
                val isHoliday = cellDate.dayOfWeek == 7 || occasions.any { it.jalaliDate == dateStr && it.isHoliday }
                val hasReminder = reminders.any {
                    it.jalaliYear == year && it.jalaliMonth == month && it.jalaliDay == dayNumber && it.isEnabled
                }

                DayCell(
                    dayNumber = dayNumber,
                    isSelected = isSelected,
                    isToday = isToday,
                    isHoliday = isHoliday,
                    hasReminder = hasReminder,
                    onClick = { onDateSelected(cellDate) }
                )
            } else {
                Box(modifier = Modifier.aspectRatio(1f))
            }
        }
    }
}

@Composable
fun DayCell(
    dayNumber: Int,
    isSelected: Boolean,
    isToday: Boolean,
    isHoliday: Boolean,
    hasReminder: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(3.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.surfaceVariant
                    else -> Color.Transparent
                }
            )
            .border(
                width = if (isToday && !isSelected) 1.dp else 0.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = dayNumber.toPersianDigits(),
                fontSize = 14.sp,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isHoliday -> FarsiHolidayRed
                    else -> MaterialTheme.colorScheme.onBackground
                }
            )
            if (hasReminder) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.primary
                        )
                )
            }
        }
    }
}

@Composable
fun SelectedDayDetails(
    selectedDate: JalaliDate,
    occasions: List<Occasion>,
    reminders: List<Reminder>,
    onReminderClick: (Reminder) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        item {
            Text(
                text = "${selectedDate.dayOfWeekNameFa}، ${selectedDate.day.toPersianDigits()} ${selectedDate.monthNameFa}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            )
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
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                text = rem.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val timeStr = "\u200E${rem.hour.toPersianDigits().padStart(2, '۰')}:${rem.minute.toPersianDigits().padStart(2, '۰')}"
                            Text(
                                text = "ساعت $timeStr",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
