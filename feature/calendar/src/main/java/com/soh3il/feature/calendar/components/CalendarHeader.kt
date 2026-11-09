package com.soh3il.feature.calendar.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soh3il.core.designsystem.utils.toPersianDigits
import com.soh3il.core.jalali.JalaliDate

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
        // [LOCKED RULE] DO NOT CHANGE: Button directions strictly locked per user requirement:
        // Right side of screen (Start in RTL): Previous Month button (ChevronRight)
        IconButton(onClick = onPreviousMonth) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "ماه قبل",
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

            SuggestionChip(
                onClick = onTodayClick,
                label = {
                    Text(
                        text = "امروز",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                shape = RoundedCornerShape(8.dp),
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                border = null
            )
        }

        // [LOCKED RULE] DO NOT CHANGE: Button directions strictly locked per user requirement:
        // Left side of screen (End in RTL): Next Month button (ChevronLeft)
        IconButton(onClick = onNextMonth) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "ماه بعد",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
