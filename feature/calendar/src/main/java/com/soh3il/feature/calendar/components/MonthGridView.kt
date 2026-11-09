package com.soh3il.feature.calendar.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.soh3il.core.jalali.JalaliDate
import com.soh3il.core.jalali.matchesDate
import com.soh3il.core.model.Occasion
import com.soh3il.core.model.Reminder

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
                val hasReminder = reminders.any { it.matchesDate(cellDate) }

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
