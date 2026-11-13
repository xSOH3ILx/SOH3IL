package com.soh3il.feature.settings

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import com.soh3il.core.scheduler.NotificationHelper
import android.os.Build
import android.os.PowerManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.soh3il.core.database.Soh3ilDatabase
import com.soh3il.core.database.preferences.UserPreferences
import com.soh3il.core.model.AppTheme
import com.soh3il.core.scheduler.LiveIconHelper
import com.soh3il.core.scheduler.PersistentNotificationHelper
import com.soh3il.feature.settings.dialogs.AutoStartDialog
import com.soh3il.feature.settings.sections.AboutUpdateSection
import com.soh3il.feature.settings.sections.AppearanceSection
import com.soh3il.feature.settings.sections.BatterySection
import com.soh3il.feature.settings.sections.NotificationsSection
import com.soh3il.feature.settings.sections.IntegrationsSection
import com.soh3il.feature.settings.sections.ToolsSection
import com.soh3il.feature.settings.util.AutoStartHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    currentTheme: AppTheme = AppTheme.SYSTEM,
    onThemeSelected: (AppTheme) -> Unit = {},
    isAmoled: Boolean = false,
    onAmoledToggled: (Boolean) -> Unit = {},
    currentVersionName: String = "1.0.0",
    onPersistentNotificationToggled: ((Boolean) -> Unit)? = null,
    onExportIcsClick: (() -> Unit)? = null,
    onImportIcsUri: ((Uri) -> Unit)? = null,
    onSyncCalendarClick: (() -> Unit)? = null,
    contactBirthdaysEnabled: Boolean = false,
    contactsPermissionGranted: Boolean = false,
    onContactBirthdaysToggled: ((Boolean) -> Unit)? = null,
    onBackClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Dialog state
    var showDateConverterDialog by remember { mutableStateOf(false) }
    var showAutoStartDialog by remember { mutableStateOf(false) }
    val deviceGuide = remember { AutoStartHelper.getDeviceGuide() }

    // User preferences state
    var isPersistentNotificationEnabled by remember {
        mutableStateOf(UserPreferences.isPersistentNotificationEnabled(context))
    }
    var isLockscreenCalendarEnabled by remember {
        mutableStateOf(UserPreferences.isLockscreenCalendarEnabled(context))
    }
    var isGregorianDateEnabled by remember {
        mutableStateOf(UserPreferences.isPersistentGregorianEnabled(context))
    }
    var isHijriDateEnabled by remember {
        mutableStateOf(UserPreferences.isPersistentHijriEnabled(context))
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

    fun syncAllWidgetsAndNotifications() {
        // 1. Broadcast all SOH3IL widget variants without coupling this feature to :app
        val manager = AppWidgetManager.getInstance(context)
        listOf(
            "com.soh3il.calendar.widget.CalendarAppWidgetProvider",
            "com.soh3il.calendar.widget.CompactCalendarWidgetProvider",
            "com.soh3il.calendar.widget.MonthCalendarWidgetProvider"
        ).forEach { providerName ->
            val component = ComponentName(context.packageName, providerName)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                context.sendBroadcast(
                    Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                        this.component = component
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                    }
                )
            }
        }

        // 2. Refresh Persistent Notification
        scope.launch(Dispatchers.IO) {
            try {
                if (UserPreferences.isPersistentNotificationEnabled(context)) {
                    val db = Soh3ilDatabase.getInstance(context)
                    val active = db.reminderDao().getAllActiveRemindersSync().map { it.toDomain() }
                    PersistentNotificationHelper.updateNotification(context, active)
                }
            } catch (_: Exception) {}
        }

        // 3. Update Live Icon
        try {
            LiveIconHelper.updateLiveIcon(context)
        } catch (_: Exception) {}
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
            NotificationHelper.updateNotificationChannelSound(context, uri)
            selectedSoundTitle = title
            syncAllWidgetsAndNotifications()
        }
    }

    val powerManager = remember(context) { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    val isIgnoringBattery = remember(context) {
        powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "تنظیمات برنامه",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "بازگشت",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. پوسته و ظاهر (Appearance & Theme)
        AppearanceSection(
            context = context,
            currentTheme = currentTheme,
            onThemeSelected = { newTheme ->
                onThemeSelected(newTheme)
                syncAllWidgetsAndNotifications()
            },
            isAmoled = isAmoled,
            onAmoledToggled = { newAmoled ->
                onAmoledToggled(newAmoled)
                syncAllWidgetsAndNotifications()
            },
            isLiveIconEnabled = isLiveIconEnabled,
            onLiveIconToggled = { newVal ->
                isLiveIconEnabled = newVal
                UserPreferences.setLiveIconEnabled(context, newVal)
                LiveIconHelper.updateLiveIcon(context)
                syncAllWidgetsAndNotifications()
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
                syncAllWidgetsAndNotifications()
            },
            isLockscreenCalendarEnabled = isLockscreenCalendarEnabled,
            onLockscreenCalendarToggled = { enabled ->
                isLockscreenCalendarEnabled = enabled
                UserPreferences.setLockscreenCalendarEnabled(context, enabled)
                syncAllWidgetsAndNotifications()
            },
            isGregorianDateEnabled = isGregorianDateEnabled,
            onGregorianDateToggled = { enabled ->
                isGregorianDateEnabled = enabled
                UserPreferences.setPersistentGregorianEnabled(context, enabled)
                syncAllWidgetsAndNotifications()
            },
            isHijriDateEnabled = isHijriDateEnabled,
            onHijriDateToggled = { enabled ->
                isHijriDateEnabled = enabled
                UserPreferences.setPersistentHijriEnabled(context, enabled)
                syncAllWidgetsAndNotifications()
            },
            isDynamicNotifIconEnabled = isDynamicNotifIconEnabled,
            onDynamicNotifIconToggled = { newVal ->
                isDynamicNotifIconEnabled = newVal
                UserPreferences.setDynamicNotificationIconEnabled(context, newVal)
                onPersistentNotificationToggled?.invoke(isPersistentNotificationEnabled)
                syncAllWidgetsAndNotifications()
            },
            selectedSoundTitle = selectedSoundTitle,
            ringtonePickerLauncher = ringtonePickerLauncher
        )

        Spacer(modifier = Modifier.height(20.dp))

        IntegrationsSection(
            contactBirthdaysEnabled = contactBirthdaysEnabled,
            contactsPermissionGranted = contactsPermissionGranted,
            onContactBirthdaysToggled = { enabled -> onContactBirthdaysToggled?.invoke(enabled) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 3. ابزارها و گاه‌شماری (Tools & Calendar)
        ToolsSection(
            onOpenDateConverter = { showDateConverterDialog = true },
            onExportIcsClick = onExportIcsClick,
            importIcsLauncher = importIcsLauncher,
            onSyncCalendarClick = onSyncCalendarClick
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 4. پایداری سیستم و بهینه‌سازی باتری (System & Battery Optimization)
        BatterySection(
            context = context,
            isIgnoringBattery = isIgnoringBattery,
            onShowAutoStartDialog = { showAutoStartDialog = true }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 5. درباره برنامه و به‌روزرسانی (About & Release Metadata)
        AboutUpdateSection(
            context = context,
            currentVersionName = currentVersionName
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

    if (showDateConverterDialog) {
        DateConverterDialog(onDismiss = { showDateConverterDialog = false })
    }
}
