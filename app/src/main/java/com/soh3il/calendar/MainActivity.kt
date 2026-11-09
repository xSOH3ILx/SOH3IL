package com.soh3il.calendar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.soh3il.core.database.Soh3ilDatabase
import com.soh3il.core.database.entity.ReminderEntity
import com.soh3il.core.database.preferences.UserPreferences
import com.soh3il.core.designsystem.theme.Soh3ilTheme
import com.soh3il.core.model.AppTheme
import com.soh3il.core.scheduler.AlarmScheduler
import com.soh3il.feature.calendar.CalendarScreen
import com.soh3il.feature.reminder.AddReminderSheet
import android.content.Intent
import com.soh3il.feature.settings.SettingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* handled */ }

    private var openAddReminderTrigger by mutableStateOf(false)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(com.soh3il.core.scheduler.PersistentNotificationHelper.EXTRA_OPEN_ADD_REMINDER, false)) {
            openAddReminderTrigger = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val db = Soh3ilDatabase.getInstance(this)

        if (intent.getBooleanExtra(com.soh3il.core.scheduler.PersistentNotificationHelper.EXTRA_OPEN_ADD_REMINDER, false)) {
            openAddReminderTrigger = true
        }

        setContent {
            val context = LocalContext.current
            var currentTheme by remember { mutableStateOf(UserPreferences.getTheme(context)) }
            var isAmoled by remember { mutableStateOf(UserPreferences.isAmoled(context)) }

            Soh3ilTheme(themePreference = currentTheme, isAmoled = isAmoled) {
                val coroutineScope = rememberCoroutineScope()
                var currentTab by remember { mutableStateOf(0) } // 0=Calendar, 1=Settings
                var showAddSheet by remember { mutableStateOf(false) }
                var editingReminder by remember { mutableStateOf<com.soh3il.core.model.Reminder?>(null) }

                val remindersState by db.reminderDao().getAllReminders().collectAsState(initial = emptyList())
                val reminders = remindersState.map { it.toDomain() }

                val currentVersion = remember {
                    runCatching {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0)).versionName
                        } else {
                            @Suppress("DEPRECATION")
                            packageManager.getPackageInfo(packageName, 0).versionName
                        }
                    }.getOrNull() ?: "1.0.0"
                }

                // Update persistent notification, Home Screen Widget, and Live Launcher Icon
                androidx.compose.runtime.LaunchedEffect(reminders) {
                    com.soh3il.core.scheduler.PersistentNotificationHelper.updateNotification(context, reminders)
                    com.soh3il.calendar.widget.CalendarAppWidgetProvider.updateAllWidgets(context)
                    com.soh3il.core.scheduler.LiveIconHelper.updateLiveIcon(context)
                    com.soh3il.core.scheduler.MidnightScheduler.scheduleMidnightAlarm(context)
                }

                // Check if opened from notification "+ یادآور جدید" action
                androidx.compose.runtime.LaunchedEffect(openAddReminderTrigger) {
                    if (openAddReminderTrigger) {
                        editingReminder = null
                        showAddSheet = true
                        openAddReminderTrigger = false
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = currentTab == 0,
                                onClick = { currentTab = 0 },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == 0) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth,
                                        contentDescription = "تقویم"
                                    )
                                },
                                label = { Text("تقویم") }
                            )
                            NavigationBarItem(
                                selected = currentTab == 1,
                                onClick = { currentTab = 1 },
                                icon = {
                                    Icon(
                                        imageVector = if (currentTab == 1) Icons.Filled.Settings else Icons.Outlined.Settings,
                                        contentDescription = "تنظیمات"
                                    )
                                },
                                label = { Text("تنظیمات") }
                            )
                        }
                    }
                ) { innerPadding ->
                    androidx.compose.foundation.layout.Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentTab) {
                            0 -> CalendarScreen(
                                reminders = reminders,
                                onAddReminderClick = {
                                    editingReminder = null
                                    showAddSheet = true
                                },
                                onReminderClick = {
                                    editingReminder = it
                                    showAddSheet = true
                                }
                            )
                            1 -> SettingsScreen(
                                currentTheme = currentTheme,
                                onThemeSelected = {
                                    currentTheme = it
                                    UserPreferences.setTheme(context, it)
                                    com.soh3il.calendar.widget.CalendarAppWidgetProvider.requestUpdate(this@MainActivity)
                                },
                                isAmoled = isAmoled,
                                onAmoledToggled = {
                                    isAmoled = it
                                    UserPreferences.setAmoled(context, it)
                                    com.soh3il.calendar.widget.CalendarAppWidgetProvider.requestUpdate(this@MainActivity)
                                },
                                currentVersionName = currentVersion,
                                onPersistentNotificationToggled = { enabled ->
                                    if (enabled) {
                                        com.soh3il.core.scheduler.PersistentNotificationHelper.updateNotification(this@MainActivity, reminders)
                                    } else {
                                        com.soh3il.core.scheduler.PersistentNotificationHelper.cancelNotification(this@MainActivity)
                                    }
                                    com.soh3il.calendar.widget.CalendarAppWidgetProvider.requestUpdate(this@MainActivity)
                                },
                                onExportIcsClick = {
                                    com.soh3il.core.scheduler.IcsHelper.shareIcsFile(this@MainActivity, reminders)
                                },
                                onImportIcsUri = { uri ->
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val imported = com.soh3il.core.scheduler.IcsHelper.importFromIcsUri(this@MainActivity, uri)
                                        imported.forEach { rem ->
                                            val id = db.reminderDao().insertReminder(ReminderEntity.fromDomain(rem))
                                            val saved = rem.copy(id = id)
                                            AlarmScheduler.scheduleReminder(this@MainActivity, saved)
                                        }
                                        com.soh3il.calendar.widget.CalendarAppWidgetProvider.requestUpdate(this@MainActivity)
                                        if (UserPreferences.isPersistentNotificationEnabled(this@MainActivity)) {
                                            com.soh3il.core.scheduler.PersistentNotificationHelper.updateNotification(this@MainActivity)
                                        }
                                    }
                                }
                            )
                        }

                        if (showAddSheet) {
                            val initialDate = editingReminder?.let {
                                com.soh3il.core.jalali.JalaliDate(it.jalaliYear, it.jalaliMonth, it.jalaliDay)
                            } ?: com.soh3il.core.jalali.JalaliDate.now()

                            AddReminderSheet(
                                initialDate = initialDate,
                                existingReminder = editingReminder,
                                onDismiss = {
                                    showAddSheet = false
                                    editingReminder = null
                                },
                                onSaveReminder = { reminder ->
                                    coroutineScope.launch(Dispatchers.IO) {
                                        if (reminder.id != 0L) {
                                            db.reminderDao().updateReminder(ReminderEntity.fromDomain(reminder))
                                            AlarmScheduler.cancelReminder(this@MainActivity, reminder)
                                            AlarmScheduler.scheduleReminder(this@MainActivity, reminder)
                                        } else {
                                            val id = db.reminderDao().insertReminder(ReminderEntity.fromDomain(reminder))
                                            val savedReminder = reminder.copy(id = id)
                                            AlarmScheduler.scheduleReminder(this@MainActivity, savedReminder)
                                        }
                                        com.soh3il.calendar.widget.CalendarAppWidgetProvider.requestUpdate(this@MainActivity)
                                        if (com.soh3il.core.database.preferences.UserPreferences.isPersistentNotificationEnabled(this@MainActivity)) {
                                            com.soh3il.core.scheduler.PersistentNotificationHelper.updateNotification(this@MainActivity)
                                        }
                                    }
                                },
                                onDeleteReminder = { reminder ->
                                    coroutineScope.launch(Dispatchers.IO) {
                                        db.reminderDao().deleteReminderById(reminder.id)
                                        AlarmScheduler.cancelReminder(this@MainActivity, reminder)
                                        com.soh3il.calendar.widget.CalendarAppWidgetProvider.requestUpdate(this@MainActivity)
                                        if (com.soh3il.core.database.preferences.UserPreferences.isPersistentNotificationEnabled(this@MainActivity)) {
                                            com.soh3il.core.scheduler.PersistentNotificationHelper.updateNotification(this@MainActivity)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        com.soh3il.core.scheduler.LiveIconHelper.updateLiveIcon(this)
        com.soh3il.core.scheduler.MidnightScheduler.scheduleMidnightAlarm(this)
        com.soh3il.calendar.widget.CalendarAppWidgetProvider.requestUpdate(this)
        if (com.soh3il.core.database.preferences.UserPreferences.isPersistentNotificationEnabled(this)) {
            com.soh3il.core.scheduler.PersistentNotificationHelper.updateNotification(this)
        }
    }
}
