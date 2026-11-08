package com.soh3il.feature.settings

import android.app.Activity
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soh3il.core.database.preferences.UserPreferences
import com.soh3il.core.model.AppTheme
import com.soh3il.feature.settings.dialogs.AutoStartDialog
import com.soh3il.feature.settings.dialogs.InstallPermissionDialog
import com.soh3il.feature.settings.sections.AboutUpdateSection
import com.soh3il.feature.settings.sections.AppearanceSection
import com.soh3il.feature.settings.sections.BatterySection
import com.soh3il.feature.settings.sections.NotificationsSection
import com.soh3il.feature.settings.sections.ToolsSection
import com.soh3il.feature.settings.util.AutoStartHelper

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

    // Dialog state
    var showDateConverterDialog by remember { mutableStateOf(false) }
    var showAutoStartDialog by remember { mutableStateOf(false) }
    var showInstallPermissionDialog by remember { mutableStateOf(false) }
    val deviceGuide = remember { AutoStartHelper.getDeviceGuide() }

    // User preferences state
    var isPersistentNotificationEnabled by remember {
        mutableStateOf(UserPreferences.isPersistentNotificationEnabled(context))
    }
    var isLiveIconEnabled by remember {
        mutableStateOf(UserPreferences.isLiveIconEnabled(context))
    }
    var isDynamicNotifIconEnabled by remember {
        mutableStateOf(UserPreferences.isDynamicNotificationIconEnabled(context))
    }
    var selectedSoundTitle by remember {
        mutableStateOf(UserPreferences.getNotificationSoundTitle(context))
    }

    val importIcsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onImportIcsUri?.invoke(uri)
        }
    }

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

        // 1. پوسته و ظاهر (Appearance & Theme)
        AppearanceSection(
            context = context,
            currentTheme = currentTheme,
            onThemeSelected = onThemeSelected,
            isAmoled = isAmoled,
            onAmoledToggled = onAmoledToggled,
            isLiveIconEnabled = isLiveIconEnabled,
            onLiveIconToggled = { newVal ->
                isLiveIconEnabled = newVal
                UserPreferences.setLiveIconEnabled(context, newVal)
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 2. اعلان‌ها و نوار وضعیت (Notifications & Status Bar)
        NotificationsSection(
            context = context,
            isPersistentNotificationEnabled = isPersistentNotificationEnabled,
            onPersistentNotificationToggled = { enabled ->
                isPersistentNotificationEnabled = enabled
                UserPreferences.setPersistentNotificationEnabled(context, enabled)
                onPersistentNotificationToggled?.invoke(enabled)
            },
            isDynamicNotifIconEnabled = isDynamicNotifIconEnabled,
            onDynamicNotifIconToggled = { newVal ->
                isDynamicNotifIconEnabled = newVal
                UserPreferences.setDynamicNotificationIconEnabled(context, newVal)
                onPersistentNotificationToggled?.invoke(isPersistentNotificationEnabled)
            },
            selectedSoundTitle = selectedSoundTitle,
            ringtonePickerLauncher = ringtonePickerLauncher
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 3. ابزارها و گاه‌شماری (Tools & Calendar)
        ToolsSection(
            onOpenDateConverter = { showDateConverterDialog = true },
            onExportIcsClick = onExportIcsClick,
            importIcsLauncher = importIcsLauncher
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 4. پایداری سیستم و بهینه‌سازی باتری (System & Battery Optimization)
        BatterySection(
            context = context,
            isIgnoringBattery = isIgnoringBattery,
            onShowAutoStartDialog = { showAutoStartDialog = true }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 5. درباره برنامه و بروزرسانی (About & Direct Update)
        AboutUpdateSection(
            context = context,
            currentVersionName = currentVersionName,
            onShowInstallPermissionDialog = { showInstallPermissionDialog = true }
        )
    }

    // Dialogs
    if (showAutoStartDialog) {
        AutoStartDialog(
            context = context,
            deviceGuide = deviceGuide,
            onDismiss = { showAutoStartDialog = false }
        )
    }

    if (showInstallPermissionDialog) {
        InstallPermissionDialog(
            context = context,
            onDismiss = { showInstallPermissionDialog = false }
        )
    }

    if (showDateConverterDialog) {
        DateConverterDialog(onDismiss = { showDateConverterDialog = false })
    }
}
