package com.soh3il.feature.calendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soh3il.core.designsystem.theme.FarsiHolidayRed
import com.soh3il.core.designsystem.utils.toPersianDigits

@Composable
fun DayCell(
    dayNumber: Int,
    isSelected: Boolean,
    isToday: Boolean,
    isHoliday: Boolean,
    hasReminder: Boolean,
    monthName: String = "",
    eventsCount: Int = 0,
    onClick: () -> Unit
) {
    val cellBackground = when {
        // Selected holidays stay red, never blue. #B91C1C keeps white text above 4.5:1.
        isSelected && isHoliday -> FarsiHolidayRed
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.surfaceVariant
        isHoliday -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
        else -> Color.Transparent
    }

    val accessibilityDesc = buildString {
        append(dayNumber.toPersianDigits())
        if (monthName.isNotBlank()) append(" $monthName")
        if (isToday) append("، امروز")
        if (isHoliday) append("، تعطیل رسمی")
        if (eventsCount > 0) {
            append("، دارای ${eventsCount.toPersianDigits()} یادآور")
        } else if (hasReminder) {
            append("، دارای یادآور")
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .heightIn(min = 48.dp)
            .padding(3.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(cellBackground)
            .border(
                width = if (isToday && !isSelected) 1.5.dp else 0.dp,
                color = if (isHoliday) FarsiHolidayRed else MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(12.dp)
            )
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityDesc
                role = Role.Button
                selected = isSelected
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = dayNumber.toPersianDigits(),
                fontSize = 14.sp,
                lineHeight = 16.sp,
                maxLines = 1,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> Color.White // [LOCKED RULE] White text for selected cells
                    isHoliday -> MaterialTheme.colorScheme.error
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
                            if (isSelected) Color.White
                            else if (isHoliday) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        )
                )
            }
        }
    }
}
