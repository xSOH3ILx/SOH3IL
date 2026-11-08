package com.soh3il.core.jalali

import com.soh3il.core.model.RecurrenceRule
import com.soh3il.core.model.RecurrenceType
import com.soh3il.core.model.Reminder
import java.io.InputStream
import java.time.LocalDate

object IcsParser {

    /**
     * صدور رویدادها در قالب فایل استاندارد جهانی iCalendar (RFC 5545)
     */
    fun exportToIcsString(reminders: List<Reminder>): String {
        val sb = StringBuilder()
        sb.append("BEGIN:VCALENDAR\r\n")
        sb.append("VERSION:2.0\r\n")
        sb.append("PRODID:-//SOH3IL//Persian Calendar//FA\r\n")
        sb.append("CALSCALE:GREGORIAN\r\n")
        sb.append("METHOD:PUBLISH\r\n")

        val nowStr = "20261108T000000Z"

        reminders.forEach { rem ->
            // Convert Jalali date of reminder to Gregorian
            val gDate = runCatching {
                DateConverter.fromJalali(rem.jalaliYear, rem.jalaliMonth, rem.jalaliDay).gregorian
            }.getOrDefault(LocalDate.now())

            val dtStart = String.format(
                "%04d%02d%02dT%02d%02d00",
                gDate.year, gDate.monthValue, gDate.dayOfMonth,
                rem.hour, rem.minute
            )

            sb.append("BEGIN:VEVENT\r\n")
            sb.append("UID:soh3il_${rem.id}_${System.currentTimeMillis()}@soh3il.app\r\n")
            sb.append("DTSTAMP:$nowStr\r\n")
            sb.append("DTSTART:$dtStart\r\n")
            sb.append("SUMMARY:${escapeIcs(rem.title)}\r\n")
            if (rem.description.isNotBlank()) {
                sb.append("DESCRIPTION:${escapeIcs(rem.description)}\r\n")
            }

            // Recurrence Rule
            when (rem.recurrence.type) {
                RecurrenceType.DAILY -> sb.append("RRULE:FREQ=DAILY;INTERVAL=${rem.recurrence.interval}\r\n")
                RecurrenceType.WEEKLY -> sb.append("RRULE:FREQ=WEEKLY;INTERVAL=${rem.recurrence.interval}\r\n")
                RecurrenceType.MONTHLY_JALALI -> sb.append("RRULE:FREQ=MONTHLY;INTERVAL=${rem.recurrence.interval}\r\n")
                RecurrenceType.YEARLY_JALALI -> sb.append("RRULE:FREQ=YEARLY;INTERVAL=${rem.recurrence.interval}\r\n")
                RecurrenceType.NONE -> { /* No RRULE */ }
            }

            sb.append("END:VEVENT\r\n")
        }

        sb.append("END:VCALENDAR\r\n")
        return sb.toString()
    }

    /**
     * خواندن رویدادها از فایل ICS ورودی
     */
    fun importFromIcsStream(inputStream: InputStream): List<Reminder> {
        val reminders = mutableListOf<Reminder>()
        val lines = inputStream.bufferedReader(Charsets.UTF_8).readLines()

        var inEvent = false
        var currentSummary = ""
        var currentDescription = ""
        var currentDtStart = ""
        var currentRrule = ""

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line == "BEGIN:VEVENT") {
                inEvent = true
                currentSummary = ""
                currentDescription = ""
                currentDtStart = ""
                currentRrule = ""
            } else if (line == "END:VEVENT") {
                if (inEvent && currentSummary.isNotBlank() && currentDtStart.length >= 8) {
                    val parsed = parseEventToReminder(currentSummary, currentDescription, currentDtStart, currentRrule)
                    if (parsed != null) reminders.add(parsed)
                }
                inEvent = false
            } else if (inEvent) {
                when {
                    line.startsWith("SUMMARY:") -> currentSummary = unescapeIcs(line.removePrefix("SUMMARY:"))
                    line.startsWith("DESCRIPTION:") -> currentDescription = unescapeIcs(line.removePrefix("DESCRIPTION:"))
                    line.startsWith("DTSTART:") -> currentDtStart = line.removePrefix("DTSTART:")
                    line.startsWith("DTSTART;") -> currentDtStart = line.substringAfter(":")
                    line.startsWith("RRULE:") -> currentRrule = line.removePrefix("RRULE:")
                }
            }
        }

        return reminders
    }

    private fun parseEventToReminder(
        summary: String,
        description: String,
        dtStart: String,
        rrule: String
    ): Reminder? {
        return runCatching {
            val year = dtStart.substring(0, 4).toInt()
            val month = dtStart.substring(4, 6).toInt()
            val day = dtStart.substring(6, 8).toInt()

            var hour = 9
            var minute = 0
            if (dtStart.contains("T") && dtStart.length >= 13) {
                val timePart = dtStart.substringAfter("T")
                hour = timePart.substring(0, 2).toIntOrNull() ?: 9
                minute = timePart.substring(2, 4).toIntOrNull() ?: 0
            }

            // Convert Gregorian date to Jalali
            val jResult = DateConverter.fromGregorian(year, month, day)

            // Parse RRULE
            var recType = RecurrenceType.NONE
            var recInterval = 1
            if (rrule.contains("FREQ=DAILY")) {
                recType = RecurrenceType.DAILY
            } else if (rrule.contains("FREQ=WEEKLY")) {
                recType = RecurrenceType.WEEKLY
            } else if (rrule.contains("FREQ=MONTHLY")) {
                recType = RecurrenceType.MONTHLY_JALALI
            } else if (rrule.contains("FREQ=YEARLY")) {
                recType = RecurrenceType.YEARLY_JALALI
            }

            if (rrule.contains("INTERVAL=")) {
                val intervalStr = rrule.substringAfter("INTERVAL=").substringBefore(";")
                recInterval = intervalStr.toIntOrNull() ?: 1
            }

            Reminder(
                title = summary,
                description = description,
                jalaliYear = jResult.jalali.year,
                jalaliMonth = jResult.jalali.month,
                jalaliDay = jResult.jalali.day,
                hour = hour,
                minute = minute,
                recurrence = RecurrenceRule(
                    type = recType,
                    interval = recInterval
                ),
                isEnabled = true
            )
        }.getOrNull()
    }

    private fun escapeIcs(text: String): String {
        return text.replace("\\", "\\\\")
            .replace(",", "\\,")
            .replace(";", "\\;")
            .replace("\n", "\\n")
    }

    private fun unescapeIcs(text: String): String {
        return text.replace("\\n", "\n")
            .replace("\\;", ";")
            .replace("\\,", ",")
            .replace("\\\\", "\\")
    }
}
