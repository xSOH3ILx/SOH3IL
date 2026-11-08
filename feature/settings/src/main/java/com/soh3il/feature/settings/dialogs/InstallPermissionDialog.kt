package com.soh3il.feature.settings.dialogs

import android.content.Context
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soh3il.core.updater.UpdateChecker

@Composable
fun InstallPermissionDialog(
    context: Context,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
                    onDismiss()
                }
            ) {
                Text("رفتن به تنظیمات مجوز")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}
