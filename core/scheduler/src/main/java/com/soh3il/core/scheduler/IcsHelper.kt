package com.soh3il.core.scheduler

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.soh3il.core.jalali.IcsParser
import com.soh3il.core.model.Reminder
import java.io.File
import java.io.InputStream

object IcsHelper {

    fun exportToIcsString(reminders: List<Reminder>): String =
        IcsParser.exportToIcsString(reminders)

    fun importFromIcsStream(inputStream: InputStream): List<Reminder> =
        IcsParser.importFromIcsStream(inputStream)

    fun shareIcsFile(context: Context, reminders: List<Reminder>) {
        val icsData = exportToIcsString(reminders)
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, "SOH3IL_Calendar_Export.ics")
        file.writeText(icsData, Charsets.UTF_8)

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/calendar"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, "اشتراک فایل تقویم"))
    }
}
