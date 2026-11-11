package com.soh3il.feature.calendar.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soh3il.core.designsystem.utils.toPersianDigits
import com.soh3il.core.jalali.DateConverter
import com.soh3il.core.jalali.JalaliDate

@Composable
fun CalendarHeader(
    currentDate: JalaliDate,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTodayClick: () -> Unit,
    onHeaderTitleClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // [LOCKED RULE - Invariant 3] Button vectors strictly locked:
        // Right side of screen (Start in RTL): Previous Month button (ChevronRight)
        IconButton(
            onClick = onPreviousMonth,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "ماه قبل",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // Middle: Month name & year centered, clickable to open Year/Month picker, plus "امروز" chip
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .then(
                        if (onHeaderTitleClick != null) {
                            Modifier.clickable { onHeaderTitleClick() }
                        } else Modifier
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${currentDate.monthNameFa} ${currentDate.year.toPersianDigits()}",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (onHeaderTitleClick != null) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "انتخاب سریع سال و ماه",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Companion Gregorian & Hijri summary
                val gregConv = DateConverter.fromJalali(currentDate.year, currentDate.month, 15)
                val gregMonthName = DateConverter.GREGORIAN_MONTHS_EN.getOrElse(gregConv.gregorian.monthValue - 1) { "" }
                val hijriMonthName = DateConverter.HIJRI_MONTHS_FA.getOrElse(gregConv.hijriMonth - 1) { "" }
                val companionText = "$gregMonthName ${gregConv.gregorian.year} • $hijriMonthName ${gregConv.hijriYear.toPersianDigits()}"
                Text(
                    text = companionText,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Normal
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            SuggestionChip(
                onClick = onTodayClick,
                modifier = Modifier
                    .defaultMinSize(minHeight = 44.dp)
                    .semantics { contentDescription = "بازگشت به امروز" },
                label = {
                    Text(
                        text = "امروز",
                        fontSize = 11.sp,
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

        // [LOCKED RULE - Invariant 3] Button vectors strictly locked:
        // Left side of screen (End in RTL): Next Month button (ChevronLeft)
        IconButton(
            onClick = onNextMonth,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "ماه بعد",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
