package com.soh3il.feature.calendar.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.soh3il.core.jalali.JalaliDate
import com.soh3il.core.jalali.visibleOn
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

    val emptySlots = startingDayOfWeek - 1
    val totalCells = emptySlots + daysInMonth
    val rowCount = (totalCells + 6) / 7

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        for (row in 0 until rowCount) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (column in 0 until 7) {
                    val index = row * 7 + column
                    Box(modifier = Modifier.weight(1f)) {
                        if (index in emptySlots until totalCells) {
                            val dayNumber = (index - emptySlots) + 1
                            val cellDate = JalaliDate(year, month, dayNumber)
                            val dateStr = cellDate.format("YYYY-MM-DD")
                            val isFriday = cellDate.dayOfWeek == 7
                            val isOfficialHoliday = occasions.any { it.jalaliDate == dateStr && it.isHoliday }
                            val eventsCount = reminders.count { it.visibleOn(cellDate) }
                            DayCell(
                                dayNumber = dayNumber,
                                isSelected = cellDate == selectedDate,
                                isToday = cellDate == today,
                                isHoliday = isFriday || isOfficialHoliday,
                                isFriday = isFriday,
                                hasReminder = eventsCount > 0,
                                monthName = cellDate.monthNameFa,
                                eventsCount = eventsCount,
                                onClick = { onDateSelected(cellDate) }
                            )
                        } else {
                            Box(modifier = Modifier.aspectRatio(1f))
                        }
                    }
                }
            }
        }
    }
}
