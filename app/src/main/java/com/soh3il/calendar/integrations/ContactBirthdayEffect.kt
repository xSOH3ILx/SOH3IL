package com.soh3il.calendar.integrations

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.soh3il.core.model.Occasion

@Composable
fun rememberContactBirthdays(
    context: Context,
    solarYear: Int,
    enabled: Boolean,
    permissionGranted: Boolean,
    refreshVersion: Int
): List<Occasion> {
    var birthdays by remember { mutableStateOf<List<Occasion>>(emptyList()) }
    LaunchedEffect(solarYear, enabled, permissionGranted, refreshVersion) {
        if (!enabled || !permissionGranted) {
            birthdays = emptyList()
        } else {
            ContactBirthdayProvider.loadForSolarYear(context, solarYear).fold(
                onSuccess = { birthdays = it },
                onFailure = {
                    birthdays = emptyList()
                    Toast.makeText(context, "دسترسی به مخاطبین قطع شده است؛ مجوز آن را در تنظیمات گوشی بررسی کنید.", Toast.LENGTH_LONG).show()
                }
            )
        }
    }
    return birthdays
}
