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
import com.soh3il.feature.settings.SettingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* handled */ }

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

        setContent {
            val context = LocalContext.current
            var currentTheme by remember { mutableStateOf(UserPreferences.getTheme(context)) }
            var isAmoled by remember { mutableStateOf(UserPreferences.isAmoled(context)) }

            Soh3ilTheme(themePreference = currentTheme, isAmoled = isAmoled) {
                val coroutineScope = rememberCoroutineScope()
                var currentTab by remember { mutableStateOf(0) } // 0=Calendar, 1=Settings
                var showAddSheet by remember { mutableStateOf(false) }

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

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = currentTab == 0,
                                onClick = { currentTab = 0 },
                                icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "تقویم") },
                                label = { Text("تقویم") }
                            )
                            NavigationBarItem(
                                selected = currentTab == 1,
                                onClick = { currentTab = 1 },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "تنظیمات") },
                                label = { Text("تنظیمات") }
                            )
                        }
                    }
                ) { innerPadding ->
                    androidx.compose.foundation.layout.Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentTab) {
                            0 -> CalendarScreen(
                                reminders = reminders,
                                onAddReminderClick = { showAddSheet = true }
                            )
                            1 -> SettingsScreen(
                                currentTheme = currentTheme,
                                onThemeSelected = {
                                    currentTheme = it
                                    UserPreferences.setTheme(context, it)
                                },
                                isAmoled = isAmoled,
                                onAmoledToggled = {
                                    isAmoled = it
                                    UserPreferences.setAmoled(context, it)
                                },
                                currentVersionName = currentVersion
                            )
                        }

                        if (showAddSheet) {
                            AddReminderSheet(
                                onDismiss = { showAddSheet = false },
                                onSaveReminder = { reminder ->
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val id = db.reminderDao().insertReminder(ReminderEntity.fromDomain(reminder))
                                        val savedReminder = reminder.copy(id = id)
                                        AlarmScheduler.scheduleReminder(this@MainActivity, savedReminder)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
