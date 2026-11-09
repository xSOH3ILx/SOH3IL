package com.soh3il.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soh3il.core.designsystem.theme.FarsiEventGray
import com.soh3il.core.designsystem.theme.FarsiEventGrayContainer
import com.soh3il.core.designsystem.theme.FarsiHolidayRed
import com.soh3il.core.designsystem.theme.FarsiHolidayRedContainer

@Composable
fun FarsiBadge(
    text: String,
    isHoliday: Boolean = false,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isHoliday) FarsiHolidayRedContainer else FarsiEventGrayContainer
    val textColor = if (isHoliday) FarsiHolidayRed else FarsiEventGray
    val dotColor = if (isHoliday) FarsiHolidayRed else FarsiEventGray

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@Composable
fun FarsiCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val borderModifier = Modifier.border(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp)
    )

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.then(borderModifier),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            content()
        }
    } else {
        Card(
            modifier = modifier.then(borderModifier),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            content()
        }
    }
}
