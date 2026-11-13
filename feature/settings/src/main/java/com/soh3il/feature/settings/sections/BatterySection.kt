package com.soh3il.feature.settings.sections

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soh3il.core.designsystem.components.FarsiBadge
import com.soh3il.core.designsystem.components.FarsiCard
import com.soh3il.feature.settings.components.SettingsSectionHeader

fun openBatteryOptimization(context: Context) {
    try {
        // Let the user choose the app from the system list instead of requesting
        // a direct Doze exemption from inside the app.
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            context.startActivity(fallback)
        } catch (e2: Exception) {
            openAppDetailsSettings(context)
        }
    }
}

fun openAppDetailsSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}

@Composable
fun BatterySection(
    context: Context,
    isIgnoringBattery: Boolean,
    onShowAutoStartDialog: () -> Unit
) {
    SettingsSectionHeader(
        title = "سیستم و پایداری زنگ‌ها",
        subtitle = "جلوگیری از بسته شدن برنامه در پس‌زمینه برای دقت ۱۰۰٪ یادآورها",
        icon = Icons.Default.BatteryAlert
    )
    Spacer(modifier = Modifier.height(6.dp))

    FarsiCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { openBatteryOptimization(context) }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "وضعیت بهینه‌سازی باتری سیستم:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (isIgnoringBattery) {
                    FarsiBadge(text = "مجاز و دقیق", isHoliday = false)
                } else {
                    FarsiBadge(text = "نیازمند اقدام", isHoliday = true)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "سیستم‌عامل اندروید برای کاهش مصرف باتری ممکن است برنامه‌ها را در پس‌زمینه متوقف کند. برای اینکه یادآورهای SOH3IL دقیقاً سر موعد زنگ بزنند، حالت باتری برنامه را روی بدون محدودیت (Unrestricted) قرار دهید.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { openBatteryOptimization(context) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("تنظیم باتری (Unrestricted)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onShowAutoStartDialog,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("شروع خودکار و پس‌زمینه", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
