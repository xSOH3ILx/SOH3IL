package com.soh3il.feature.settings.sections

import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soh3il.core.database.preferences.UserPreferences
import com.soh3il.core.designsystem.components.FarsiCard
import com.soh3il.feature.settings.components.SettingsSectionHeader

@Composable
fun NotificationsSection(
    context: Context,
    isPersistentNotificationEnabled: Boolean,
    onPersistentNotificationToggled: (Boolean) -> Unit,
    isDynamicNotifIconEnabled: Boolean,
    onDynamicNotifIconToggled: (Boolean) -> Unit,
    selectedSoundTitle: String,
    ringtonePickerLauncher: ActivityResultLauncher<Intent>
) {
    var isPlayingPreview by remember { mutableStateOf(false) }
    var currentRingtone by remember { mutableStateOf<Ringtone?>(null) }

    SettingsSectionHeader(
        title = "اعلان‌ها و نوار وضعیت",
        subtitle = "نوار تاریخ دائمی، صفحه قفل، AOD و صدای زنگ هشدار",
        icon = Icons.Default.NotificationsActive
    )
    Spacer(modifier = Modifier.height(6.dp))

    FarsiCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Persistent Status Bar Notification Switch Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .semantics(mergeDescendants = true) {}
                    .clickable(role = Role.Switch) {
                        onPersistentNotificationToggled(!isPersistentNotificationEnabled)
                    }
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "نوار اعلان دائمی تاریخ",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "نمایش عدد بزرگ روز شمسی در نوار وضعیت، صفحه قفل و Always On Display (AOD) با مناسبت‌ها و آیکون زنده.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )
                }

                Switch(
                    checked = isPersistentNotificationEnabled,
                    onCheckedChange = null
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dynamic Status Bar Icon Switch Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .semantics(mergeDescendants = true) {}
                    .clickable(role = Role.Switch) {
                        onDynamicNotifIconToggled(!isDynamicNotifIconEnabled)
                    }
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "نمایش رقم زنده روز در نوار وضعیت (Status Bar)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "نمایش رقم فارسی روز (مانند ۱۸) در بالای صفحه گوشی بجای آیکون پیش‌فرض برنامه.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = isDynamicNotifIconEnabled,
                    onCheckedChange = null
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Notification Sound Setting Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "صدای زنگ اعلان یادآورها:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = selectedSoundTitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            try {
                                if (isPlayingPreview) {
                                    currentRingtone?.stop()
                                    isPlayingPreview = false
                                } else {
                                    val currentUriString = UserPreferences.getNotificationSoundUri(context)
                                    val uri = currentUriString?.let { Uri.parse(it) }
                                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                                    val ringtone = RingtoneManager.getRingtone(context, uri)
                                    currentRingtone = ringtone
                                    ringtone?.play()
                                    isPlayingPreview = true
                                }
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlayingPreview) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "پخش نمونه صدا",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val currentUriString = UserPreferences.getNotificationSoundUri(context)
                            val existingUri = currentUriString?.let { Uri.parse(it) }
                                ?: Settings.System.DEFAULT_NOTIFICATION_URI
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION or RingtoneManager.TYPE_ALARM)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "انتخاب صدای اعلان یادآور SOH3IL")
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
                            }
                            ringtonePickerLauncher.launch(intent)
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("تغییر صدا", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
