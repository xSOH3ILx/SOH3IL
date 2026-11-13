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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

@Composable
fun AboutUpdateSection(
    context: Context,
    currentVersionName: String
) {
    val coroutineScope = rememberCoroutineScope()
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var updateCheckMessage by remember { mutableStateOf<String?>(null) }

    SettingsSectionHeader(
        title = "درباره برنامه و به‌روزرسانی",
        subtitle = "اطلاعات نسخه و صفحه انتشار رسمی SOH3IL",
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
                    text = "گاه‌شمار و یادآور SOH3IL",
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
                text = "طراحی و توسعه‌یافته با کاتلین خالص و Jetpack Compose برای استفاده شخصی و خانوادگی؛ آزاد و متن‌باز برای عموم علاقه‌مندان.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            Button(
                onClick = {
                    coroutineScope.launch {
                        isCheckingUpdate = true
                        updateCheckMessage = null
                        val result = UpdateChecker.checkForUpdates(currentVersionName)
                        updateInfo = result
                        isCheckingUpdate = false
                        updateCheckMessage = if (result.hasUpdate) {
                            "نسخه جدید ${result.latestVersionName.toPersianDigits()} در صفحه انتشار رسمی موجود است."
                        } else {
                            "شما از آخرین نسخه SOH3IL استفاده می‌کنید."
                        }
                    }
                },
                enabled = !isCheckingUpdate,
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
                    Text("در حال بررسی نسخه...", fontSize = 13.sp)
                } else {
                    Text("بررسی نسخه جدید", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            updateCheckMessage?.let { message ->
                Text(
                    text = message,
                    fontSize = 13.sp,
                    color = if (updateInfo?.hasUpdate == true) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            updateInfo?.takeIf { it.hasUpdate }?.let { info ->
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
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "تغییرات نسخه ${info.latestVersionName.toPersianDigits()}:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                    Text(
                        text = info.changelog,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { UpdateChecker.openReleasePage(context, info.releaseHtmlUrl) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مشاهده صفحه انتشار رسمی", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
