package com.soh3il.core.database.converter

import com.soh3il.core.model.RecurrenceRule
import com.soh3il.core.model.RecurrenceType
import com.soh3il.core.model.ShortMonthPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class DatabaseConvertersTest {
    private val converters = DatabaseConverters()

    @Test fun existingSolarReminderEncodingIsUnchanged() {
        val encoded = "MONTHLY_JALALI|2|31||SNAP_TO_LAST_DAY"
        val rule = converters.toRecurrenceRule(encoded)
        assertEquals(RecurrenceType.MONTHLY_JALALI, rule.type)
        assertEquals(31, rule.dayOfMonth)
        assertEquals(encoded, converters.fromRecurrenceRule(rule))
    }

    @Test fun gregorianCalendarAndSkipPolicySurviveDatabaseRoundTrip() {
        for (type in listOf(RecurrenceType.MONTHLY_GREGORIAN, RecurrenceType.YEARLY_GREGORIAN)) {
            val rule = RecurrenceRule(type = type, interval = 3, shortMonthPolicy = ShortMonthPolicy.SKIP_MONTH)
            assertEquals(rule, converters.toRecurrenceRule(converters.fromRecurrenceRule(rule)))
        }
    }
}
