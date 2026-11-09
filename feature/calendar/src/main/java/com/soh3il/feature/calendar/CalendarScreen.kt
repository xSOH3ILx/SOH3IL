package com.soh3il.feature.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soh3il.core.jalali.IranianHolidays
import com.soh3il.core.jalali.JalaliDate
import com.soh3il.core.jalali.matchesDate
import com.soh3il.core.model.Occasion
import com.soh3il.core.model.Reminder
import com.soh3il.feature.calendar.components.CalendarHeader
import com.soh3il.feature.calendar.components.MonthGridView
import com.soh3il.feature.calendar.components.SelectedDayDetails
import com.soh3il.feature.calendar.components.WeekdaysRow

@OptIn(ExperimentalMaterial3Api::class)
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
    var viewMode by remember { mutableIntStateOf(0) } // 0=Month Grid, 1=Agenda View

    val monthOccasions = remember(displayedDate.year, displayedDate.month, occasions) {
        if (occasions.isNotEmpty()) occasions else IranianHolidays.getOccasionsForMonth(displayedDate.year, displayedDate.month)
    }

    val selectedDayOccasions = remember(selectedDate, occasions) {
        val list = if (occasions.isNotEmpty()) occasions else IranianHolidays.getOccasionsForMonth(selectedDate.year, selectedDate.month)
        list.filter { it.jalaliDate == selectedDate.format("YYYY-MM-DD") }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddReminderClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "افزودن یادآور")
            }
        },
        // [LOCKED RULE] DO NOT CHANGE: FAB position strictly locked to FabPosition.Start (Right side in RTL) per user requirement
        floatingActionButtonPosition = FabPosition.Start
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // View Mode Selector (Material 3 SingleChoiceSegmentedButtonRow with centered text, without checkmark)
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                SegmentedButton(
                    selected = viewMode == 0,
                    onClick = { viewMode = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = {} // [LOCKED RULE] Checkmark removed per user requirement
                ) {
                    Text(
                        text = "تقویم ماهانه",
                        fontSize = 13.sp,
                        fontWeight = if (viewMode == 0) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                SegmentedButton(
                    selected = viewMode == 1,
                    onClick = { viewMode = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = {} // [LOCKED RULE] Checkmark removed per user requirement
                ) {
                    Text(
                        text = "دستور کار (رویدادها)",
                        fontSize = 13.sp,
                        fontWeight = if (viewMode == 1) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (viewMode == 0) {
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(displayedDate) {
                            detectHorizontalDragGestures(
                                onDragStart = { totalDragDistance = 0f },
                                onDragEnd = {
                                    val threshold = 40.dp.toPx()
                                    // [LOCKED RULE] DO NOT CHANGE: Swipe gesture mapping strictly locked per user requirement:
                                    // Positive drag (>0) -> plusMonths(1)
                                    // Negative drag (<0) -> plusMonths(-1)
                                    if (totalDragDistance > threshold) {
                                        displayedDate = displayedDate.plusMonths(1)
                                    } else if (totalDragDistance < -threshold) {
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
                        occasions = monthOccasions,
                        onDateSelected = { selectedDate = it }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Selected Day Info & Occasions & Reminders
                SelectedDayDetails(
                    selectedDate = selectedDate,
                    occasions = selectedDayOccasions,
                    reminders = reminders.filter { it.matchesDate(selectedDate) },
                    onReminderClick = onReminderClick
                )
            } else {
                // Agenda / Schedule View
                AgendaView(
                    today = today,
                    reminders = reminders,
                    onReminderClick = onReminderClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
