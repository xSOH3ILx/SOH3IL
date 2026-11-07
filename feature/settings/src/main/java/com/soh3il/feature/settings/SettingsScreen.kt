package com.soh3il.feature.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun SettingsScreen(
    currentTheme: AppTheme = AppTheme.SYSTEM,
    onThemeSelected: (AppTheme) -> Unit = {},
    isAmoled: Boolean = false,
    onAmoledToggled: (Boolean) -> Unit = {},
    currentVersionName: String = "1.0.0"
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

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

        Spacer(modifier = Modifier.height(20.dp))

        // 1. Theme Selection & AMOLED
        FarsiCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ColorLens,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "پوسته و ظاهر برنامه",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

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

                Spacer(modifier = Modifier.height(12.dp))

                // AMOLED Switch (active when theme is dark or system dark)
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

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Battery & Alarms Guidance (DontKillMyApp warning & Direct Click Action)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.BatteryAlert,
                            contentDescription = null,
                            tint = if (isIgnoringBattery) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "بهینه‌سازی باتری و دقت آلارم‌ها",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    if (isIgnoringBattery) {
                        FarsiBadge(text = "مجاز و دقیق", isHoliday = false)
                    } else {
                        FarsiBadge(text = "نیازمند اقدام", isHoliday = true)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "سیستم‌عامل اندروید برای کاهش مصرف باتری ممکن است برنامه‌ها را در پس‌زمینه متوقف کند. برای اینکه یادآورهای SOH3IL بدون تاخیر و دقیقاً سر موعد زنگ بزنند، حالت باتری برنامه را روی بدون محدودیت (Unrestricted) قرار دهید.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { openBatteryOptimization(context) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("تنظیم باتری", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { showAutoStartDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("شروع خودکار و پایداری", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. In-App Direct Update
        FarsiCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.SystemUpdate,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "بروزرسانی مستقیم برنامه",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Text(
                        text = "نسخه فعلی: $currentVersionName",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                                "نسخه جدید ${result.latestVersionName} موجود است!"
                            } else {
                                "شما از آخرین نسخه سُهیل استفاده می‌کنید."
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
}
