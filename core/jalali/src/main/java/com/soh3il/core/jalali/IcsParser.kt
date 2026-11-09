package com.soh3il.core.jalali

import com.soh3il.core.model.RecurrenceRule
import com.soh3il.core.model.RecurrenceType
import com.soh3il.core.model.Reminder
import java.io.InputStream
import java.time.LocalDate

object IcsParser {

    const val MAX_EVENTS = 5_000
    const val MAX_STREAM_BYTES = 10 * 1024 * 1024L // 10MB limit

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
     * خواندن ایمن رویدادها از فایل ICS ورودی مطابق با RFC 5545 (با پشتیبانی از Line Unfolding،
     * مهار سرریز حافظه با سقف ۱۰ مگابایت و حداکثر ۵,۰۰۰ رویداد، و پاک‌سازی کاراکترهای کنترلی).
     */
    fun importFromIcsStream(inputStream: InputStream, maxEvents: Int = MAX_EVENTS): List<Reminder> {
        val reminders = mutableListOf<Reminder>()
        var bytesRead = 0L

        val reader = inputStream.bufferedReader(Charsets.UTF_8)
        val unfoldedLines = mutableListOf<String>()
        var currentBuilder: StringBuilder? = null

        for (rawLine in reader.lineSequence()) {
            bytesRead += rawLine.length + 1
            if (bytesRead > MAX_STREAM_BYTES) {
                break
            }

            // RFC 5545 Section 3.1: Line unfolding
            if (rawLine.startsWith(" ") || rawLine.startsWith("\t")) {
                currentBuilder?.append(rawLine.substring(1))
            } else {
                currentBuilder?.let { unfoldedLines.add(it.toString()) }
                currentBuilder = StringBuilder(rawLine)
            }
        }
        currentBuilder?.let { unfoldedLines.add(it.toString()) }

        var inEvent = false
        var currentSummary = ""
        var currentDescription = ""
        var currentDtStart = ""
        var currentRrule = ""

        for (line in unfoldedLines) {
            val trimmed = line.trim()
            if (trimmed.equals("BEGIN:VEVENT", ignoreCase = true)) {
                inEvent = true
                currentSummary = ""
                currentDescription = ""
                currentDtStart = ""
                currentRrule = ""
            } else if (trimmed.equals("END:VEVENT", ignoreCase = true)) {
                if (inEvent && currentSummary.isNotBlank() && currentDtStart.length >= 8) {
                    val sanitizedSummary = sanitizeText(currentSummary)
                    val sanitizedDescription = sanitizeText(currentDescription)
                    val parsed = parseEventToReminder(sanitizedSummary, sanitizedDescription, currentDtStart, currentRrule)
                    if (parsed != null) {
                        reminders.add(parsed)
                        if (reminders.size >= maxEvents) {
                            break
                        }
                    }
                }
                inEvent = false
            } else if (inEvent) {
                when {
                    trimmed.startsWith("SUMMARY:", ignoreCase = true) ->
                        currentSummary = unescapeIcs(trimmed.substring(8))
                    trimmed.startsWith("DESCRIPTION:", ignoreCase = true) ->
                        currentDescription = unescapeIcs(trimmed.substring(12))
                    trimmed.startsWith("DTSTART:", ignoreCase = true) ->
                        currentDtStart = trimmed.substring(8)
                    trimmed.startsWith("DTSTART;", ignoreCase = true) ->
                        currentDtStart = trimmed.substringAfter(":")
                    trimmed.startsWith("RRULE:", ignoreCase = true) ->
                        currentRrule = trimmed.substring(6)
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

            val jResult = DateConverter.fromGregorian(year, month, day)

            var recType = RecurrenceType.NONE
            var recInterval = 1
            if (rrule.contains("FREQ=DAILY", ignoreCase = true)) {
                recType = RecurrenceType.DAILY
            } else if (rrule.contains("FREQ=WEEKLY", ignoreCase = true)) {
                recType = RecurrenceType.WEEKLY
            } else if (rrule.contains("FREQ=MONTHLY", ignoreCase = true)) {
                recType = RecurrenceType.MONTHLY_JALALI
            } else if (rrule.contains("FREQ=YEARLY", ignoreCase = true)) {
                recType = RecurrenceType.YEARLY_JALALI
            }

            if (rrule.contains("INTERVAL=", ignoreCase = true)) {
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

    fun escapeIcs(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace("\n", "\\n")
    }

    fun unescapeIcs(text: String): String {
        return text
            .replace("\\n", "\n", ignoreCase = true)
            .replace("\\;", ";")
            .replace("\\,", ",")
            .replace("\\\\", "\\")
    }

    fun sanitizeText(text: String): String {
        return text.filter { ch ->
            ch == '\n' || ch == '\t' || (!ch.isISOControl() && ch != '\uFEFF')
        }
    }
}
