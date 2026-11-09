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

    @Test
    fun testLineUnfoldingAndEscapedCharacters() {
        val icsWithFolding = """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//Test//FA
            BEGIN:VEVENT
            UID:test_fold_1@test.app
            DTSTART:20250321T090000
            SUMMARY:یک متن بسیار طولانی که بر اساس استاندارد RFC 5545 به چند
              خط شکسته شده و تا خورده است
            DESCRIPTION:خط اول توضیحات\nخط دوم توضیحات با کاراکترهای خاص\, و \; و اسلش \\
            END:VEVENT
            END:VCALENDAR
        """.trimIndent()

        val imported = IcsParser.importFromIcsStream(ByteArrayInputStream(icsWithFolding.toByteArray(Charsets.UTF_8)))
        assertEquals(1, imported.size)
        assertEquals(
            "یک متن بسیار طولانی که بر اساس استاندارد RFC 5545 به چند خط شکسته شده و تا خورده است",
            imported[0].title
        )
        assertEquals(
            "خط اول توضیحات\nخط دوم توضیحات با کاراکترهای خاص, و ; و اسلش \\",
            imported[0].description
        )
    }

    @Test
    fun testMaxEventsCap() {
        val sb = StringBuilder()
        sb.append("BEGIN:VCALENDAR\r\n")
        for (i in 1..20) {
            sb.append("BEGIN:VEVENT\r\n")
            sb.append("DTSTART:20250321T090000\r\n")
            sb.append("SUMMARY:رویداد $i\r\n")
            sb.append("END:VEVENT\r\n")
        }
        sb.append("END:VCALENDAR\r\n")

        val imported = IcsParser.importFromIcsStream(
            ByteArrayInputStream(sb.toString().toByteArray(Charsets.UTF_8)),
            maxEvents = 5
        )
        assertEquals(5, imported.size)
    }
}
