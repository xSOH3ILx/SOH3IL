package com.soh3il.calendar.integrations

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import com.soh3il.calendar.ReminderMutations
import com.soh3il.core.database.Soh3ilDatabase
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

@SdkSuppress(minSdkVersion = 29)
class ProviderAccessTest {
    @Test fun unreadableIcsReturnsFailureWithoutMutatingReminders() = runBlocking {
        val context = revokedContext("fixture.ics")
        val database = Soh3ilDatabase.getInstance(ApplicationProvider.getApplicationContext())
        val before = database.reminderDao().getAllRemindersSync()
        val result = ReminderMutations.importIcs(context, database, Uri.parse("content://fixture.ics/unreadable"))
        assertTrue(result.exceptionOrNull() is IOException)
        org.junit.Assert.assertEquals(before, database.reminderDao().getAllRemindersSync())
    }

    private fun revokedContext(authority: String = "com.android.calendar"): Context {
        val app = ApplicationProvider.getApplicationContext<Context>()
        val provider = object : ContentProvider() {
            override fun onCreate() = true
            override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor =
                throw SecurityException("fixture permission revoked between check and query")
            override fun getType(uri: Uri): String? = null
            override fun insert(uri: Uri, values: ContentValues?): Uri? = error("unexpected insert")
            override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = error("unexpected delete")
            override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = error("unexpected update")
        }
        provider.attachInfo(app, ProviderInfo().apply { this.authority = authority; exported = true })
        val resolver = ContentResolver.wrap(provider)
        return object : ContextWrapper(app) {
            override fun getContentResolver() = resolver
            override fun checkPermission(permission: String, pid: Int, uid: Int) = PackageManager.PERMISSION_GRANTED
        }
    }

    @Test fun contactsRevocationReturnsFailureInsteadOfThrowing() = runBlocking {
        val result = ContactBirthdayProvider.loadForSolarYear(revokedContext("com.android.contacts"), 1405)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }

    @Test fun calendarRevocationReturnsFailureInsteadOfThrowing() = runBlocking {
        val result = CalendarProviderSync.syncReminders(revokedContext(), emptyList())
        assertTrue(result.exceptionOrNull() is SecurityException)
    }
}
