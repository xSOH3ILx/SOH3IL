package com.soh3il.core.jalali

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.time.LocalDate

class IcsRegressionTest {
    @Test fun invalidNativeGregorianAnchorIsRejected() {
        for ((start, rule) in listOf("20260331" to "MONTHLY_GREGORIAN;DAY=32", "20260228" to "YEARLY_GREGORIAN;DAY=29;INTERVAL=4")) {
            val text = "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\nSUMMARY:fixture\r\nDTSTART:${start}T090000\r\nX-SOH3IL-RECURRENCE:$rule;POLICY=SKIP_MONTH\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n"
            assertTrue(rule, IcsParser.importFromIcsStream(ByteArrayInputStream(text.toByteArray())).isEmpty())
        }
    }

    private fun importEvent(start: String, rule: String) = IcsParser.importFromIcsStream(
        ByteArrayInputStream("BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\nSUMMARY:fixture\r\nDTSTART:$start\r\nRRULE:$rule\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n".toByteArray())
    ).single()

    private fun nextDate(start: String, rule: String): LocalDate {
        val reminder = importEvent(start, rule)
        val base = JalaliDate(reminder.jalaliYear, reminder.jalaliMonth, reminder.jalaliDay)
        val trigger = JalaliRecurrenceEngine.calculateNextTrigger(reminder, base.toEpochMillis(10, 0, 0))!!
        val next = JalaliDate.fromEpochMillis(trigger)
        assertTrue(JalaliRecurrenceEngine.matchesDate(reminder, next))
        return DateConverter.fromJalali(next.year, next.month, next.day).gregorian
    }

    @Test fun monthlyImportKeepsGregorianCalendarAndInterval() {
        assertEquals(LocalDate.of(2026, 5, 15), nextDate("20260315T090000", "FREQ=MONTHLY;INTERVAL=2"))
        assertEquals("MONTHLY_GREGORIAN", importEvent("20260315T090000", "FREQ=MONTHLY").recurrence.type.name)
    }

    @Test fun monthlyImportSkipsNonexistentGregorianDayWithoutMovingAnchor() {
        assertEquals(LocalDate.of(2026, 5, 31), nextDate("20260331T090000", "FREQ=MONTHLY"))
        val reminder = importEvent("20260331T090000", "FREQ=MONTHLY")
        val april30 = DateConverter.fromGregorian(2026, 4, 30).jalali
        assertFalse(JalaliRecurrenceEngine.matchesDate(reminder, april30))
        val roundTrip = IcsParser.importFromIcsStream(ByteArrayInputStream(IcsParser.exportToIcsString(listOf(reminder)).toByteArray())).single()
        assertEquals(reminder.recurrence, roundTrip.recurrence)
    }

    @Test fun yearlyImportKeepsLeapDayOnGregorianCalendar() {
        assertEquals(LocalDate.of(2028, 2, 29), nextDate("20240229T090000", "FREQ=YEARLY"))
    }

    @Test fun invalidGregorianAnchorFailsBeforeScheduling() {
        val valid = importEvent("20260331T090000", "FREQ=MONTHLY")
        val invalid = valid.copy(recurrence = valid.recurrence.copy(dayOfMonth = 32))
        assertThrows(IllegalArgumentException::class.java) {
            JalaliRecurrenceEngine.calculateNextTrigger(invalid, JalaliDate(1405, 1, 11).toEpochMillis(10, 0, 0))
        }
    }

    @Test(timeout = 2_000) fun impossibleGregorianRuleFailsWithoutAnEndlessSearch() {
        val valid = importEvent("20260228T090000", "FREQ=YEARLY;INTERVAL=4")
        val invalid = valid.copy(recurrence = valid.recurrence.copy(dayOfMonth = 29))
        assertThrows(IllegalArgumentException::class.java) {
            JalaliRecurrenceEngine.calculateNextTrigger(invalid, JalaliDate(1405, 1, 1).toEpochMillis(10, 0, 0))
        }
    }

    @Test fun oversizedSingleLineStopsAtByteLimitAndReportsFailure() {
        var bytesRead = 0L
        val stream = object : InputStream() {
            override fun read(): Int = if (bytesRead++ < IcsParser.MAX_STREAM_BYTES + 1_048_576) 'A'.code else -1
        }
        assertThrows(IllegalArgumentException::class.java) { IcsParser.importFromIcsStream(stream) }
        assertTrue("The source must be stopped before the full oversized line is read", bytesRead <= IcsParser.MAX_STREAM_BYTES + 1)
    }

    @Test fun allDayImportPreservesTheDate() {
        val text = "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\nSUMMARY:fixture\r\nDTSTART;VALUE=DATE:20260321\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n"
        val reminder = IcsParser.importFromIcsStream(ByteArrayInputStream(text.toByteArray())).single()
        assertEquals(JalaliDate(1405, 1, 1), JalaliDate(reminder.jalaliYear, reminder.jalaliMonth, reminder.jalaliDay))
    }
}
