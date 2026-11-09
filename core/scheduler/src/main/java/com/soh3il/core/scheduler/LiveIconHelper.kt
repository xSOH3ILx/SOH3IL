package com.soh3il.core.scheduler

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.soh3il.core.database.preferences.UserPreferences
import com.soh3il.core.jalali.JalaliDate

object LiveIconHelper {

    private const val MAIN_ACTIVITY_NAME = "com.soh3il.calendar.MainActivity"
    private const val ALIAS_NAME_PREFIX = "com.soh3il.calendar.Day"

    /**
     * Updates the launcher app icon dynamically based on user preferences and current day.
     * Uses Android's native activity-alias mechanism to display the Jalali day number.
     */
    fun updateLiveIcon(context: Context) {
        try {
            val pm = context.packageManager
            val isEnabled = UserPreferences.isLiveIconEnabled(context)
            val today = JalaliDate.now()
            val currentDay = today.day.coerceIn(1, 31)

            val mainComponent = ComponentName(context.packageName, MAIN_ACTIVITY_NAME)
            val targetDayComponent = ComponentName(context.packageName, "$ALIAS_NAME_PREFIX$currentDay")
            val flags = PackageManager.DONT_KILL_APP

            // Quick check: If the desired state is already active, exit immediately to avoid launcher re-indexing
            val targetState = pm.getComponentEnabledSetting(targetDayComponent)
            val mainState = pm.getComponentEnabledSetting(mainComponent)

            if (isEnabled) {
                if (targetState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED &&
                    mainState == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                ) {
                    return
                }
            } else {
                if ((mainState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                     mainState == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) &&
                    targetState != PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                ) {
                    return
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val actions = mutableListOf<PackageManager.ComponentEnabledSetting>()

                if (isEnabled) {
                    actions.add(
                        PackageManager.ComponentEnabledSetting(
                            mainComponent,
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            flags
                        )
                    )
                    for (day in 1..31) {
                        val state = if (day == currentDay) {
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        } else {
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                        }
                        actions.add(
                            PackageManager.ComponentEnabledSetting(
                                ComponentName(context.packageName, "$ALIAS_NAME_PREFIX$day"),
                                state,
                                flags
                            )
                        )
                    }
                } else {
                    actions.add(
                        PackageManager.ComponentEnabledSetting(
                            mainComponent,
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                            flags
                        )
                    )
                    for (day in 1..31) {
                        actions.add(
                            PackageManager.ComponentEnabledSetting(
                                ComponentName(context.packageName, "$ALIAS_NAME_PREFIX$day"),
                                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                flags
                            )
                        )
                    }
                }

                pm.setComponentEnabledSettings(actions)
            } else {
                if (isEnabled) {
                    // Enable the target alias FIRST so a launcher activity always exists
                    pm.setComponentEnabledSetting(
                        targetDayComponent,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        flags
                    )
                    pm.setComponentEnabledSetting(
                        mainComponent,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        flags
                    )
                    for (day in 1..31) {
                        if (day != currentDay) {
                            pm.setComponentEnabledSetting(
                                ComponentName(context.packageName, "$ALIAS_NAME_PREFIX$day"),
                                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                flags
                            )
                        }
                    }
                } else {
                    // Enable MainActivity first
                    pm.setComponentEnabledSetting(
                        mainComponent,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        flags
                    )
                    for (day in 1..31) {
                        pm.setComponentEnabledSetting(
                            ComponentName(context.packageName, "$ALIAS_NAME_PREFIX$day"),
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                            flags
                        )
                    }
                }
            }
        } catch (_: Exception) {
            // Fail gracefully if launcher or package manager has transient issues
        }
    }
}
