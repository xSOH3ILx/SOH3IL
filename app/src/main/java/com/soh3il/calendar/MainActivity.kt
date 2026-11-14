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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.soh3il.core.database.Soh3ilDatabase
import com.soh3il.core.database.preferences.UserPreferences
import com.soh3il.core.designsystem.theme.Soh3ilTheme
import com.soh3il.core.designsystem.utils.toPersianDigits
import com.soh3il.feature.calendar.CalendarScreen
import com.soh3il.feature.reminder.AddReminderSheet
import android.content.Intent
import com.soh3il.feature.settings.SettingsScreen
import com.soh3il.calendar.integrations.CalendarProviderSync
import com.soh3il.calendar.integrations.rememberContactBirthdays
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private var integrationRefreshVersion by mutableStateOf(0)

    private val requestContactsPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                UserPreferences.setContactBirthdaysEnabled(this, true)
                integrationRefreshVersion++
            }
        }

    private val requestCalendarPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            integrationRefreshVersion++
        }

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                android.widget.Toast.makeText(
                    this,
                    "بدون مجوز اعلان، یادآورها نمایش داده نمی‌شوند و انجام‌شده حساب نمی‌شوند.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }

    private var openAddReminderTrigger by mutableStateOf(false)
    private var openConverterTrigger by mutableStateOf(false)
    private var openTodayTrigger by mutableStateOf(false)

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        if (
            intent.action == com.soh3il.core.scheduler.PersistentNotificationHelper.ACTION_OPEN_ADD_REMINDER ||
            intent.getBooleanExtra(com.soh3il.core.scheduler.PersistentNotificationHelper.EXTRA_OPEN_ADD_REMINDER, false)
        ) {
            openAddReminderTrigger = true
        }
        when (intent.getStringExtra("extra_action")) {
            "ADD_REMINDER" -> openAddReminderTrigger = true
            "OPEN_TODAY" -> openTodayTrigger = true
            "OPEN_CONVERTER" -> openConverterTrigger = true
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
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
        if (savedInstanceState == null) handleIncomingIntent(intent)

        setContent {
            val context = LocalContext.current
            var currentTheme by remember { mutableStateOf(UserPreferences.getTheme(context)) }
            var isAmoled by remember { mutableStateOf(UserPreferences.isAmoled(context)) }

            Soh3ilTheme(themePreference = currentTheme, isAmoled = isAmoled) {
                val coroutineScope = rememberCoroutineScope()
                var currentTab by rememberSaveable { mutableStateOf(0) } // 0=Calendar, 1=Settings
                var showAddSheet by rememberSaveable { mutableStateOf(false) }
                var showWidgetPicker by rememberSaveable { mutableStateOf(false) }
                var editingReminderId by rememberSaveable { mutableStateOf<Long?>(null) }
                var selectedCalendarJdn by rememberSaveable { mutableStateOf(com.soh3il.core.jalali.JalaliDate.now().toJdn()) }
                val selectedCalendarDate = com.soh3il.core.jalali.JalaliDate.fromJdn(selectedCalendarJdn)
                val tabState = rememberSaveableStateHolder()

                val remindersState by db.reminderDao().getAllReminders().collectAsState(initial = null)
                val reminders = remindersState.orEmpty().map { it.toDomain() }
                val editingReminder = reminders.firstOrNull { it.id == editingReminderId }
                var birthdaySolarYear by rememberSaveable { mutableStateOf(com.soh3il.core.jalali.JalaliDate.now().year) }
                val contactsPermissionGranted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_CONTACTS
                ) == PackageManager.PERMISSION_GRANTED
                val contactBirthdaysEnabled = UserPreferences.isContactBirthdaysEnabled(context)
                val contactBirthdays = rememberContactBirthdays(
                    context, birthdaySolarYear, contactBirthdaysEnabled,
                    contactsPermissionGranted, integrationRefreshVersion
                )

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

                // Check if opened from notification or app shortcut
                androidx.compose.runtime.LaunchedEffect(openAddReminderTrigger, openTodayTrigger, openConverterTrigger) {
                    if (openAddReminderTrigger) {
                        currentTab = 0
                        editingReminderId = null
                        selectedCalendarJdn = com.soh3il.core.jalali.JalaliDate.now().toJdn()
                        showAddSheet = true
                        openAddReminderTrigger = false
                    } else if (openTodayTrigger) {
                        currentTab = 0
                        selectedCalendarJdn = com.soh3il.core.jalali.JalaliDate.now().toJdn()
                        openTodayTrigger = false
                    } else if (openConverterTrigger) {
                        currentTab = 1
                        openConverterTrigger = false
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
                        tabState.SaveableStateProvider(currentTab) {
                            when (currentTab) {
                                0 -> CalendarScreen(
                                    onDisplayedYearChanged = { birthdaySolarYear = it },
                                    reminders = reminders,
                                    additionalOccasions = contactBirthdays,
                                    onAddReminderClick = { date ->
                                        selectedCalendarJdn = date.toJdn()
                                        editingReminderId = null
                                        showAddSheet = true
                                    },
                                    onReminderClick = {
                                        editingReminderId = it.id
                                        showAddSheet = true
                                    }
                                )
                                1 -> SettingsScreen(
                                    onWidgetsClick = { showWidgetPicker = true },
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
                                        android.widget.Toast.makeText(
                                            this@MainActivity,
                                            "${reminders.size.toPersianDigits()} یادآور برای صدور آماده شد",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    onImportIcsUri = { uri ->
                                        coroutineScope.launch(Dispatchers.IO) {
                                            val result = ReminderMutations.importIcs(context, db, uri)
                                            withContext(Dispatchers.Main) {
                                                val message = result.fold(
                                                    onSuccess = { (total, added) ->
                                                        if (total > added) "${added.toPersianDigits()} یادآور جدید وارد شد (${(total - added).toPersianDigits()} مورد تکراری نادیده گرفته شد)"
                                                        else "${added.toPersianDigits()} یادآور وارد شد"
                                                    },
                                                    onFailure = { error ->
                                                        if (error is IllegalArgumentException) error.message ?: "فایل تقویم معتبر نیست"
                                                        else "فایل تقویم خوانده نشد؛ دسترسی به فایل را بررسی کنید."
                                                    }
                                                )
                                                android.widget.Toast.makeText(this@MainActivity, message, android.widget.Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    onSyncCalendarClick = {
                                        val hasCalendarPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
                                            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED
                                        if (!hasCalendarPermission) {
                                            requestCalendarPermissions.launch(
                                                arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
                                            )
                                        } else {
                                            coroutineScope.launch(Dispatchers.IO) {
                                                val result = CalendarProviderSync.syncReminders(context, reminders)
                                                withContext(Dispatchers.Main) {
                                                    val message = result.fold(
                                                        onSuccess = { count -> "${count.toPersianDigits()} یادآور با تقویم دستگاه همگام شد" },
                                                        onFailure = { it.message ?: "همگام‌سازی تقویم انجام نشد" }
                                                    )
                                                    android.widget.Toast.makeText(this@MainActivity, message, android.widget.Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    },
                                    contactBirthdaysEnabled = contactBirthdaysEnabled,
                                    contactsPermissionGranted = contactsPermissionGranted,
                                    onContactBirthdaysToggled = { enabled ->
                                        if (enabled && !contactsPermissionGranted) {
                                            requestContactsPermission.launch(Manifest.permission.READ_CONTACTS)
                                        } else {
                                            UserPreferences.setContactBirthdaysEnabled(context, enabled)
                                            integrationRefreshVersion++
                                        }
                                    }
                                )
                            }
                        }

                        if (showWidgetPicker) {
                            com.soh3il.calendar.widget.WidgetPinDialog { showWidgetPicker = false }
                        }

                        if (showAddSheet && (editingReminderId == null || editingReminder != null)) {
                            val initialDate = editingReminder?.let {
                                com.soh3il.core.jalali.JalaliDate(it.jalaliYear, it.jalaliMonth, it.jalaliDay)
                            } ?: selectedCalendarDate

                            AddReminderSheet(
                                initialDate = initialDate,
                                existingReminder = editingReminder,
                                onDismiss = {
                                    showAddSheet = false
                                    editingReminderId = null
                                },
                                onSaveReminder = { reminder ->
                                    coroutineScope.launch(Dispatchers.IO) { ReminderMutations.save(context, db, reminder) }
                                },
                                onDeleteReminder = { reminder ->
                                    coroutineScope.launch(Dispatchers.IO) { ReminderMutations.delete(context, db, reminder) }
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
        integrationRefreshVersion++
        com.soh3il.core.scheduler.LiveIconHelper.updateLiveIcon(this)
        com.soh3il.core.scheduler.MidnightScheduler.scheduleMidnightAlarm(this)
        com.soh3il.calendar.widget.CalendarAppWidgetProvider.requestUpdate(this)
        if (com.soh3il.core.database.preferences.UserPreferences.isPersistentNotificationEnabled(this)) {
            com.soh3il.core.scheduler.PersistentNotificationHelper.updateNotification(this)
        }
    }
}
