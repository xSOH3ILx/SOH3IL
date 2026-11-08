package com.soh3il.core.database.preferences

import android.content.Context
import android.content.SharedPreferences
import com.soh3il.core.model.AppTheme

object UserPreferences {
    private const val PREFS_NAME = "soh3il_user_preferences"
    private const val KEY_THEME = "pref_theme"
    private const val KEY_AMOLED = "pref_amoled"
    private const val KEY_NOTIFICATION_SOUND_URI = "pref_sound_uri"
    private const val KEY_NOTIFICATION_SOUND_TITLE = "pref_sound_title"
    private const val KEY_PERSISTENT_NOTIFICATION = "pref_persistent_notification"
    private const val KEY_LIVE_ICON = "pref_live_icon"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getTheme(context: Context): AppTheme {
        val name = getPrefs(context).getString(KEY_THEME, AppTheme.SYSTEM.name)
        return try {
            AppTheme.valueOf(name ?: AppTheme.SYSTEM.name)
        } catch (_: Exception) {
            AppTheme.SYSTEM
        }
    }

    fun setTheme(context: Context, theme: AppTheme) {
        getPrefs(context).edit().putString(KEY_THEME, theme.name).apply()
    }

    fun isAmoled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_AMOLED, false)
    }

    fun setAmoled(context: Context, isAmoled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_AMOLED, isAmoled).apply()
    }

    fun getNotificationSoundUri(context: Context): String? {
        return getPrefs(context).getString(KEY_NOTIFICATION_SOUND_URI, null)
    }

    fun getNotificationSoundTitle(context: Context): String {
        return getPrefs(context).getString(KEY_NOTIFICATION_SOUND_TITLE, "پیش‌فرض سیستم") ?: "پیش‌فرض سیستم"
    }

    fun setNotificationSound(context: Context, uri: String?, title: String) {
        getPrefs(context).edit()
            .putString(KEY_NOTIFICATION_SOUND_URI, uri)
            .putString(KEY_NOTIFICATION_SOUND_TITLE, title)
            .apply()
    }

    fun isPersistentNotificationEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_PERSISTENT_NOTIFICATION, true)
    }

    fun setPersistentNotificationEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_PERSISTENT_NOTIFICATION, enabled).apply()
    }

    fun isLiveIconEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_LIVE_ICON, true)
    }

    fun setLiveIconEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_LIVE_ICON, enabled).apply()
    }

    fun isDynamicNotificationIconEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean("pref_dynamic_notif_icon", true)
    }

    fun setDynamicNotificationIconEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean("pref_dynamic_notif_icon", enabled).apply()
    }
}
