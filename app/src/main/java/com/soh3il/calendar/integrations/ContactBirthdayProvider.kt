package com.soh3il.calendar.integrations

import android.annotation.SuppressLint
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.soh3il.core.jalali.BirthdayDates
import com.soh3il.core.model.Occasion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ContactBirthdayProvider {
    @SuppressLint("Recycle")
    suspend fun loadForSolarYear(context: Context, solarYear: Int): Result<List<Occasion>> = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return@withContext Result.failure(SecurityException("مجوز دسترسی به مخاطبین صادر نشده است"))
        }
        try {
            val projection = arrayOf(
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.Data.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Event.START_DATE
            )
            val selection = "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.CommonDataKinds.Event.TYPE} = ?"
            val args = arrayOf(
                ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE,
                ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY.toString()
            )
            val result = mutableListOf<Occasion>()
            context.contentResolver.query(ContactsContract.Data.CONTENT_URI, projection, selection, args, null)?.use { cursor ->
                val contactIdIndex = cursor.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID)
                val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.Data.DISPLAY_NAME)
                val dateIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Event.START_DATE)
                while (cursor.moveToNext()) {
                    val contactId = cursor.getString(contactIdIndex)
                    val displayName = cursor.getString(nameIndex).orEmpty().ifBlank { "مخاطب" }
                    BirthdayDates.inSolarYear(cursor.getString(dateIndex).orEmpty(), solarYear).forEach { date ->
                        result += Occasion(
                            id = "contact-birthday-$contactId-${date.format("YYYY-MM-DD")}",
                            jalaliDate = date.format("YYYY-MM-DD"),
                            titleFa = "تولد $displayName",
                            titleEn = "Birthday: $displayName",
                            categories = listOf("contacts", "birthday")
                        )
                    }
                }
            }
            Result.success(result.distinctBy { it.id })
        } catch (error: SecurityException) {
            Result.failure(error)
        }
    }
}
