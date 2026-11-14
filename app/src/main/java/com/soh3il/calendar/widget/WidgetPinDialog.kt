package com.soh3il.calendar.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.soh3il.calendar.R

@Composable
fun WidgetPinDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val manager = AppWidgetManager.getInstance(context)
    var unsupported by remember { mutableStateOf(!manager.isRequestPinAppWidgetSupported) }
    val variants = listOf(
        CalendarAppWidgetProvider::class.java to R.string.widget_daily_label,
        CompactCalendarWidgetProvider::class.java to R.string.widget_compact_label,
        MonthCalendarWidgetProvider::class.java to R.string.widget_month_label
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ویجت‌های صفحهٔ اصلی") },
        text = {
            Column {
                if (unsupported) {
                    Text("برای افزودن ویجت، روی فضای خالی صفحهٔ اصلی نگه دارید و فهرست ویجت‌ها را باز کنید؛ سپس SOH3IL را پیدا کنید.")
                } else {
                    Text("نوع ویجت را انتخاب کنید و افزودن آن را در پنجرهٔ گوشی تأیید کنید.")
                    variants.forEach { (provider, label) ->
                        OutlinedButton(
                            onClick = {
                                if (manager.requestPinAppWidget(ComponentName(context, provider), null, null)) onDismiss()
                                else unsupported = true
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) { Text(stringResource(label)) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("بستن") } }
    )
}
