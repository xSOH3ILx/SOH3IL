package com.soh3il.feature.settings.sections

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.soh3il.core.database.preferences.UserPreferences
import com.soh3il.core.designsystem.components.FarsiCard
import com.soh3il.core.model.AppTheme
import com.soh3il.feature.settings.components.SettingsSectionHeader

@Composable
fun AppearanceSection(
    context: Context,
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    isAmoled: Boolean,
    onAmoledToggled: (Boolean) -> Unit,
    isLiveIconEnabled: Boolean,
    onLiveIconToggled: (Boolean) -> Unit
) {
    SettingsSectionHeader(
        title = "پوسته و ظاهر",
        subtitle = "شخصی‌سازی رنگ‌بندی، حالت شب و صرفه‌جویی باتری",
        icon = Icons.Default.ColorLens
    )
    Spacer(modifier = Modifier.height(6.dp))

    FarsiCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            val themeOptions = listOf(
                AppTheme.SYSTEM to "هماهنگ با سیستم (خودکار)",
                AppTheme.LIGHT to "پوسته روشن",
                AppTheme.DARK to "پوسته تاریک"
            )

            themeOptions.forEach { (theme, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .selectable(
                            selected = (currentTheme == theme),
                            onClick = { onThemeSelected(theme) },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = (currentTheme == theme),
                        onClick = null
                    )
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = if (currentTheme == theme) FontWeight.SemiBold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // AMOLED Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onAmoledToggled(!isAmoled) }
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "مشکی خالص برای نمایشگرهای AMOLED",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (currentTheme == AppTheme.LIGHT)
                            "در حالت روشن غیرفعال است؛ در زمان تاریک بودن سیستم یا برنامه اعمال می‌شود."
                        else
                            "پس‌زمینه مشکی خالص (#000000) برای خاموش شدن پیکسل‌ها و کاهش مصرف باتری.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = isAmoled,
                    onCheckedChange = onAmoledToggled
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Live Launcher Icon Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        val newVal = !isLiveIconEnabled
                        onLiveIconToggled(newVal)
                    }
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "آیکون زنده تقویم در صفحه اصلی (Launcher Live Icon)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "نمایش روز جاری شمسی به عنوان آیکون برنامه در صفحه برنامه‌های گوشی شما.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = isLiveIconEnabled,
                    onCheckedChange = onLiveIconToggled
                )
            }
        }
    }
}
