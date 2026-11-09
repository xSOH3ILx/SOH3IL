package com.soh3il.feature.settings.sections

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soh3il.core.designsystem.components.FarsiCard
import com.soh3il.core.designsystem.utils.toPersianDigits
import com.soh3il.core.updater.UpdateChecker
import com.soh3il.core.updater.UpdateInfo
import com.soh3il.feature.settings.components.SettingsSectionHeader
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun AboutUpdateSection(
    context: Context,
    currentVersionName: String,
    onShowInstallPermissionDialog: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var updateCheckMessage by remember { mutableStateOf<String?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var downloadedApkFile by remember { mutableStateOf<File?>(null) }
    var downloadError by remember { mutableStateOf<String?>(null) }

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
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "گاه‌شمار و یادآور سهیل (SOH3IL)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "نسخه ${currentVersionName.toPersianDigits()}",
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
                            "نسخه جدید ${result.latestVersionName.toPersianDigits()} در گیت‌هاب موجود است!"
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
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
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
                            text = "تغییرات نسخه ${updateInfo!!.latestVersionName.toPersianDigits()}:",
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
                                text = "در حال دریافت فایل نصبی: ${(downloadProgress * 100).toInt().toPersianDigits()}%",
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
                                    text = "برای نصب نسخه جدید، مجوز «نصب برنامه‌های ناشناخته» نیاز است. لطفاً روی دکمه زیر زده و اجازه را فعال کنید:",
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
                                        onShowInstallPermissionDialog()
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
