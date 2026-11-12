package com.soh3il.core.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Exported only for system broadcasts. App-owned actions stay on the non-exported
 * [MidnightReceiver] so other packages cannot trigger them.
 */
class SystemEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action !in SYSTEM_ACTIONS) return
        MidnightReceiver.enqueueFromReceiver(this, context, action)
    }

    companion object {
        val SYSTEM_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            MidnightReceiver.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            MidnightReceiver.ACTION_QUICKBOOT_POWERON,
            MidnightReceiver.ACTION_HTC_QUICKBOOT_POWERON
        )
    }
}
