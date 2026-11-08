package com.soh3il.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soh3il.core.designsystem.components.FarsiCard
import com.soh3il.core.designsystem.utils.toPersianDigits
import com.soh3il.core.jalali.DateConverter
import com.soh3il.core.jalali.JalaliDate
import java.time.LocalDate

@Composable
fun DateConverterDialog(
    onDismiss: () -> Unit
) {
    val todayJ = remember { JalaliDate.now() }
    val todayG = remember { LocalDate.now() }

    var mode by remember { mutableIntStateOf(0) } // 0=From Jalali, 1=From Gregorian

    var jYear by remember { mutableStateOf(todayJ.year.toString()) }
    var jMonth by remember { mutableStateOf(todayJ.month.toString()) }
    var jDay by remember { mutableStateOf(todayJ.day.toString()) }

    var gYear by remember { mutableStateOf(todayG.year.toString()) }
    var gMonth by remember { mutableStateOf(todayG.monthValue.toString()) }
    var gDay by remember { mutableStateOf(todayG.dayOfMonth.toString()) }

    val conversionResult = remember(mode, jYear, jMonth, jDay, gYear, gMonth, gDay) {
        runCatching {
            if (mode == 0) {
                val y = jYear.toIntOrNull() ?: todayJ.year
                val m = (jMonth.toIntOrNull() ?: todayJ.month).coerceIn(1, 12)
                val maxD = JalaliDate.getDaysInMonth(y, m)
                val d = (jDay.toIntOrNull() ?: todayJ.day).coerceIn(1, maxD)
                DateConverter.fromJalali(y, m, d)
            } else {
                val y = gYear.toIntOrNull() ?: todayG.year
                val m = (gMonth.toIntOrNull() ?: todayG.monthValue).coerceIn(1, 12)
                val maxD = runCatching { LocalDate.of(y, m, 1).lengthOfMonth() }.getOrDefault(31)
                val d = (gDay.toIntOrNull() ?: todayG.dayOfMonth).coerceIn(1, maxD)
                DateConverter.fromGregorian(y, m, d)
            }
        }.getOrNull()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("بستن", fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CompareArrows,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    Text(
                        text = "مبدل پیشرفته تاریخ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "بستن")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Source Selector Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = mode == 0,
                        onClick = { mode = 0 },
                        label = { Text("ورودی: شمسی") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = mode == 1,
                        onClick = { mode = 1 },
                        label = { Text("ورودی: میلادی") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Input fields based on mode
                if (mode == 0) {
                    Text(
                        text = "تاریخ خورشیدی را وارد کنید:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = jDay,
                            onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) jDay = it },
                            label = { Text("روز") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = jMonth,
                            onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) jMonth = it },
                            label = { Text("ماه") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = jYear,
                            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) jYear = it },
                            label = { Text("سال") },
                            modifier = Modifier.weight(1.5f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                } else {
                    Text(
                        text = "تاریخ میلادی را وارد کنید:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = gDay,
                            onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) gDay = it },
                            label = { Text("Day") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = gMonth,
                            onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) gMonth = it },
                            label = { Text("Month") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = gYear,
                            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) gYear = it },
                            label = { Text("Year") },
                            modifier = Modifier.weight(1.5f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Results Card
                if (conversionResult != null) {
                    FarsiCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "نتایج معادل‌سازی",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // 1. Jalali
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("خورشیدی:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "${conversionResult.jalali.dayOfWeekNameFa}، ${conversionResult.jalali.day.toPersianDigits()} ${conversionResult.jalali.monthNameFa} ${conversionResult.jalali.year.toPersianDigits()}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            // 2. Gregorian
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("میلادی:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "${conversionResult.dayOfWeekNameEn}, ${conversionResult.gregorian.dayOfMonth} ${conversionResult.gregorian.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${conversionResult.gregorian.year}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))

                            // 3. Hijri
                            if (conversionResult.hijriYear > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("قمری:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        "${conversionResult.hijriDay.toPersianDigits()} / ${conversionResult.hijriMonth.toPersianDigits()} / ${conversionResult.hijriYear.toPersianDigits()} هـ.ق",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Extra astronomical & cultural details
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("حیوان سال خورشیدی:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(conversionResult.yearAnimal, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("وضعیت سال خورشیدی:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(if (conversionResult.isJalaliLeap) "سال کبیسه" else "سال عادی", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("فاصله تا امروز:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(conversionResult.relativeDescriptionFa, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    )
}
