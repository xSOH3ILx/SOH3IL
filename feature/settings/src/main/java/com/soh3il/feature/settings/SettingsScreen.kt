package com.soh3il.feature.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soh3il.core.database.preferences.UserPreferences
import com.soh3il.core.designsystem.components.FarsiBadge
import com.soh3il.core.designsystem.components.FarsiCard
import com.soh3il.core.model.AppTheme
import com.soh3il.core.updater.UpdateChecker
import com.soh3il.core.updater.UpdateInfo
import com.soh3il.feature.settings.util.AutoStartHelper
import kotlinx.coroutines.launch
import java.io.File

private fun openBatteryOptimization(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } else {
            openAppDetailsSettings(context)
        }
    } catch (e: Exception) {
        try {
            val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            context.startActivity(fallback)
        } catch (e2: Exception) {
            openAppDetailsSettings(context)
        }
    }
}

private fun openAppDetailsSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    subtitle: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsScreen(
    currentTheme: AppTheme = AppTheme.SYSTEM,
    onThemeSelected: (AppTheme) -> Unit = {},
    isAmoled: Boolean = false,
    onAmoledToggled: (Boolean) -> Unit = {},
    currentVersionName: String = "1.0.0",
    onPersistentNotificationToggled: ((Boolean) -> Unit)? = null,
    onExportIcsClick: (() -> Unit)? = null,
    onImportIcsUri: ((Uri) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Dialog state
    var showDateConverterDialog by remember { mutableStateOf(false) }
    var isPersistentNotificationEnabled by remember {
        mutableStateOf(UserPreferences.isPersistentNotificationEnabled(context))
    }

    val importIcsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportIcsUri?.invoke(uri)
        }
    }

    // Update state
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var updateCheckMessage by remember { mutableStateOf<String?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadedApkFile by remember { mutableStateOf<File?>(null) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var showInstallPermissionDialog by remember { mutableStateOf(false) }

    // Auto-start state
    var showAutoStartDialog by remember { mutableStateOf(false) }
    val deviceGuide = remember { AutoStartHelper.getDeviceGuide() }

    // Notification Sound state
    var selectedSoundTitle by remember { mutableStateOf(UserPreferences.getNotificationSoundTitle(context)) }
    var isPlayingPreview by remember { mutableStateOf(false) }
    var currentRingtone by remember { mutableStateOf<Ringtone?>(null) }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }

            val title = if (uri != null) {
                try {
                    val ringtone = RingtoneManager.getRingtone(context, uri)
                    ringtone?.getTitle(context) ?: "صدای انتخابی"
                } catch (_: Exception) {
                    "صدای انتخابی"
                }
            } else {
                "بی‌صدا"
            }

            UserPreferences.setNotificationSound(context, uri?.toString(), title)
            selectedSoundTitle = title
        }
    }

    val powerManager = remember(context) { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    val isIgnoringBattery = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
        } else true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "تنظیمات برنامه",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ==========================================
        // 1. پوسته و ظاهر (Appearance & Theme)
        // ==========================================
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
                            .clickable { onThemeSelected(theme) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (currentTheme == theme),
                            onClick = { onThemeSelected(theme) }
                        )
                        Text(
                            text = label,
                            fontSize = 14.sp,
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
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ==========================================
        // 2. اعلان‌ها و نوار وضعیت (Notifications & Status Bar)
        // ==========================================
        SettingsSectionHeader(
            title = "اعلان‌ها و نوار وضعیت",
            subtitle = "نوار تاریخ دائمی، صفحه قفل، AOD و صدای زنگ هشدار",
            icon = Icons.Default.NotificationsActive
        )
        Spacer(modifier = Modifier.height(6.dp))

        FarsiCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Persistent Status Bar Notification Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                        onCheckedChange = { enabled ->
                            isPersistentNotificationEnabled = enabled
                            UserPreferences.setPersistentNotificationEnabled(context, enabled)
                            onPersistentNotificationToggled?.invoke(enabled)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Notification Sound Setting
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
                            }
                        ) {
                            Icon(
                                imageVector = if (isPlayingPreview) Icons.Default.Stop else Icons.Default.VolumeUp,
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
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "انتخاب صدای اعلان یادآور سهیل")
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

        Spacer(modifier = Modifier.height(20.dp))

        // ==========================================
        // 3. ابزارها و گاه‌شماری (Tools & Calendar)
        // ==========================================
        SettingsSectionHeader(
            title = "ابزارها و گاه‌شماری",
            subtitle = "مبدل سه‌گانه تاریخ و پشتیبان‌گیری استاندارد جهانی تقویم",
            icon = Icons.Default.DateRange
        )
        Spacer(modifier = Modifier.height(6.dp))

        FarsiCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Date Converter Tool
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "مبدل پیشرفته تاریخ",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "تبدیل بلادرنگ تاریخ‌های خورشیدی، میلادی و قمری، تحلیل کبیسه و حیوان سال.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp
                        )
                    }

                    Button(
                        onClick = { showDateConverterDialog = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("باز کردن", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Universal Calendar Backup & Transfer (ICS)
                Text(
                    text = "پشتیبان‌گیری و انتقال تقویم (فرمت جهانی iCalendar / ICS):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "سازگار با تقویم گوگل، اوت‌لوک و نرم‌افزارهای استاندارد تقویم.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onExportIcsClick?.invoke() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("صدور ICS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { importIcsLauncher.launch("text/calendar") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ورود ICS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ==========================================
        // 4. پایداری سیستم و بهینه‌سازی باتری (System & Battery Optimization)
        // ==========================================
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
                    Text(
                        text = "وضعیت بهینه‌سازی باتری سیستم:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

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
                        onClick = { showAutoStartDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("شروع خودکار و پس‌زمینه", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ==========================================
        // 5. درباره برنامه و بروزرسانی (About & Direct Update)
        // ==========================================
        SettingsSectionHeader(
            title = "درباره برنامه و بروزرسانی",
            subtitle = "اطلاعات نسخه، سازنده و دریافت مستقیم جدیدترین نگارش",
            icon = Icons.Default.SystemUpdate
        )
        Spacer(modifier = Modifier.height(6.dp))

        FarsiCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "گاه‌شمار و یادآور سهیل (SOH3IL)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "نسخه $currentVersionName",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "طراحی و توسعه‌یافته با کاتلین خالص و Jetpack Compose برای استفاده شخصی و خانوادگی سهیل؛ آزاد و متن‌باز برای عموم علاقه‌مندان.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                // Check Update Button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isCheckingUpdate = true
                            updateCheckMessage = null
                            downloadError = null
                            val result = UpdateChecker.checkForUpdates(currentVersionName)
                            isCheckingUpdate = false
                            updateInfo = result
                            updateCheckMessage = if (result.hasUpdate) {
                                "نسخه جدید ${result.latestVersionName} در گیت‌هاب موجود است!"
                            } else {
                                "شما از آخرین نسخه سهیل استفاده می‌کنید."
                            }
                        }
                    },
                    enabled = !isCheckingUpdate && !isDownloading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isCheckingUpdate) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("در حال بررسی گیت‌هاب...", fontSize = 13.sp)
                    } else {
                        Text("بررسی نسخه جدید در گیت‌هاب", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                updateCheckMessage?.let { msg ->
                    Text(
                        text = msg,
                        fontSize = 13.sp,
                        color = if (updateInfo?.hasUpdate == true) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // When Update is Available
                if (updateInfo?.hasUpdate == true) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "تغییرات نسخه ${updateInfo!!.latestVersionName}:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }

                        Text(
                            text = updateInfo!!.changelog,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        if (updateInfo!!.matchedApkName.isNotEmpty()) {
                            Text(
                                text = "فایل سازگار با پردازنده: ${updateInfo!!.matchedApkName}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Download Progress
                        if (isDownloading) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                LinearProgressIndicator(
                                    progress = { downloadProgress },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "در حال دریافت فایل نصبی: ${(downloadProgress * 100).toInt()}%",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        } else if (downloadedApkFile != null && downloadedApkFile!!.exists()) {
                            // Downloaded and Ready to Install
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "فایل نصبی آماده نصب است",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(start = 6.dp)
                                    )
                                }

                                val canInstall = UpdateChecker.canInstallUnknownPackages(context)

                                if (!canInstall) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "⚠️ برای نصب نسخه جدید، مجوز «نصب برنامه‌های ناشناخته» نیاز است. لطفاً روی دکمه زیر زده و تیک اجازه را فعال کنید:",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.error,
                                        lineHeight = 18.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { UpdateChecker.openInstallPermissionSettings(context) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("فعال‌سازی مجوز نصب در تنظیمات", fontSize = 12.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        if (!UpdateChecker.canInstallUnknownPackages(context)) {
                                            showInstallPermissionDialog = true
                                            UpdateChecker.openInstallPermissionSettings(context)
                                        } else {
                                            UpdateChecker.launchApkInstall(context, downloadedApkFile!!)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("نصب نسخه جدید", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            // Start Download Button
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isDownloading = true
                                        downloadProgress = 0f
                                        downloadError = null
                                        val result = UpdateChecker.downloadApk(
                                            context = context,
                                            downloadUrl = updateInfo!!.downloadUrl,
                                            versionName = updateInfo!!.latestVersionName,
                                            onProgress = { p, _, _ ->
                                                downloadProgress = p
                                            }
                                        )
                                        isDownloading = false
                                        result.onSuccess { file ->
                                            downloadedApkFile = file
                                            if (UpdateChecker.canInstallUnknownPackages(context)) {
                                                UpdateChecker.launchApkInstall(context, file)
                                            }
                                        }.onFailure { err ->
                                            downloadError = err.message ?: "خطا در دانلود فایل"
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("دانلود و نصب مستقیم", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        downloadError?.let { err ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = err,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        // Browser Download Fallback
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                UpdateChecker.openBrowserDownload(context, updateInfo!!.releaseHtmlUrl)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("یا دانلود فایل APK از طریق مرورگر", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Auto-Start Guidance Dialog
    if (showAutoStartDialog) {
        AlertDialog(
            onDismissRequest = { showAutoStartDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "راهنمای شروع خودکار",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "دستگاه شما: ${deviceGuide.brandName}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "برای اینکه زنگ‌ها و اعلان‌های تقویم در تمام ساعات دقیق کار کنند، مراحل زیر را انجام دهید:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    deviceGuide.instructions.forEach { instruction ->
                        Text(
                            text = "• $instruction",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        AutoStartHelper.openAutoStartScreen(context)
                        showAutoStartDialog = false
                    }
                ) {
                    Text("باز کردن صفحه شروع خودکار گوشی")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAutoStartDialog = false }) {
                    Text("بستن")
                }
            }
        )
    }

    // Install Permission Dialog
    if (showInstallPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showInstallPermissionDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("مجوز نصب برنامه‌های ناشناخته", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "سیستم‌عامل اندروید برای نصب نسخه جدید از داخل برنامه، نیاز به تایید مجوز دارد.\n\nلطفاً در صفحه‌ای که باز می‌شود گزینه «اجازه از این منبع (Allow from this source)» را روشن کنید و سپس به برنامه بازگردید.",
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        UpdateChecker.openInstallPermissionSettings(context)
                        showInstallPermissionDialog = false
                    }
                ) {
                    Text("رفتن به تنظیمات مجوز")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInstallPermissionDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }

    // Date Converter Dialog
    if (showDateConverterDialog) {
        DateConverterDialog(onDismiss = { showDateConverterDialog = false })
    }
}
