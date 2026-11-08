package com.soh3il.core.jalali

import com.soh3il.core.model.RecurrenceRule
import com.soh3il.core.model.RecurrenceType
import com.soh3il.core.model.Reminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class IcsParserTest {

    @Test
    fun testExportAndImportIcsRoundTrip() {
        val reminders = listOf(
            Reminder(
                id = 1,
                title = "جلسه کاری",
                description = "بررسی عملکرد ماهانه",
                jalaliYear = 1404,
                jalaliMonth = 1,
                jalaliDay = 15,
                hour = 10,
                minute = 30,
                recurrence = RecurrenceRule(type = RecurrenceType.MONTHLY_JALALI, interval = 1),
                isEnabled = true
            ),
            Reminder(
                id = 2,
                title = "پرداخت شهریه",
                description = "",
                jalaliYear = 1404,
                jalaliMonth = 2,
                jalaliDay = 1,
                hour = 8,
                minute = 0,
                recurrence = RecurrenceRule(type = RecurrenceType.NONE),
                isEnabled = true
            )
        )

        val icsText = IcsParser.exportToIcsString(reminders)
        assertTrue(icsText.contains("BEGIN:VCALENDAR"))
        assertTrue(icsText.contains("SUMMARY:جلسه کاری"))
        assertTrue(icsText.contains("RRULE:FREQ=MONTHLY;INTERVAL=1"))
        assertTrue(icsText.contains("SUMMARY:پرداخت شهریه"))
        assertTrue(icsText.contains("END:VCALENDAR"))

        // Now import
        val imported = IcsParser.importFromIcsStream(ByteArrayInputStream(icsText.toByteArray(Charsets.UTF_8)))
        assertEquals(2, imported.size)
        assertEquals("جلسه کاری", imported[0].title)
        assertEquals(10, imported[0].hour)
        assertEquals(30, imported[0].minute)
        assertEquals(RecurrenceType.MONTHLY_JALALI, imported[0].recurrence.type)
        assertEquals("پرداخت شهریه", imported[1].title)
    }
}
