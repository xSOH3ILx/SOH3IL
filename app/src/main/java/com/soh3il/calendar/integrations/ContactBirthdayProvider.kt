package com.soh3il.calendar.integrations

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.soh3il.core.jalali.DateConverter
import com.soh3il.core.model.Occasion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ContactBirthdayProvider {
    suspend fun loadForSolarYear(context: Context, solarYear: Int): List<Occasion> = withContext(Dispatchers.IO) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return@withContext emptyList()
        }

        val gregorianYear = DateConverter.fromJalali(solarYear, 1, 1).gregorian.year
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
        context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            projection,
            selection,
            args,
            null
        )?.use { cursor ->
            val contactIdIndex = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
            val nameIndex = cursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME)
            val dateIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Event.START_DATE)
            while (cursor.moveToNext()) {
                val contactId = cursor.getString(contactIdIndex)
                val displayName = cursor.getString(nameIndex).orEmpty().ifBlank { "مخاطب" }
                val rawDate = cursor.getString(dateIndex).orEmpty()
                val match = Regex("(?:\\d{4}-)?(\\d{2})-(\\d{2})").find(rawDate) ?: continue
                val month = match.groupValues[1].toIntOrNull() ?: continue
                val day = match.groupValues[2].toIntOrNull() ?: continue
                val solarDate = runCatching {
                    DateConverter.fromGregorian(gregorianYear, month, day).jalali
                }.getOrNull() ?: continue
                result += Occasion(
                    id = "contact-birthday-$contactId-$month-$day",
                    jalaliDate = solarDate.format("YYYY-MM-DD"),
                    titleFa = "تولد $displayName",
                    titleEn = "Birthday: $displayName",
                    categories = listOf("contacts", "birthday")
                )
            }
        }
        result.distinctBy { it.id }
    }
}
