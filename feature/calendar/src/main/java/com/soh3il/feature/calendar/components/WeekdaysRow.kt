package com.soh3il.feature.calendar.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WeekdaysRow() {
    val weekdayLetters = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")
    val fullWeekdayNames = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .semantics { heading() },
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        weekdayLetters.forEachIndexed { index, letter ->
            val isFriday = index == 6
            Text(
                text = letter,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isFriday) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = fullWeekdayNames[index] },
                textAlign = TextAlign.Center
            )
        }
    }
}
