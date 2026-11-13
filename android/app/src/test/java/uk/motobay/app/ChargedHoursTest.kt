package uk.motobay.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.motobay.app.data.Bay
import uk.motobay.app.data.ChargedHours
import uk.motobay.app.data.Fee
import uk.motobay.app.data.chargeStatus
import uk.motobay.app.data.isFreeAt
import java.time.LocalDateTime

/** 9 November 2026 is a Monday. */
class ChargedHoursTest {
    private fun at(day: Int, h: Int, m: Int = 0) = LocalDateTime.of(2026, 11, 8 + day, h, m)
    private val weekdaysAndSaturdayMorning = ChargedHours.parse("1-5 08:30-18:30;6 08:30-13:30")!!
    private fun paid(charged: String?) = Bay("council/x/1", 51.5, -0.1, "Motorcycle parking", fee = Fee.Paid, charged = charged)

    @Test fun chargedInsideTheWindowsOnly() {
        assertTrue(weekdaysAndSaturdayMorning.chargedAt(at(1, 8, 30)))
        assertTrue(weekdaysAndSaturdayMorning.chargedAt(at(5, 18, 29)))
        assertFalse(weekdaysAndSaturdayMorning.chargedAt(at(1, 8, 29)))
        assertFalse(weekdaysAndSaturdayMorning.chargedAt(at(5, 18, 30)))
        assertTrue(weekdaysAndSaturdayMorning.chargedAt(at(6, 13, 29)))
        assertFalse(weekdaysAndSaturdayMorning.chargedAt(at(6, 13, 30)))
        assertFalse(weekdaysAndSaturdayMorning.chargedAt(at(7, 12)))
    }

    @Test fun nextChangeIsWhenTheAnswerFlips() {
        assertEquals(at(2, 18, 30), weekdaysAndSaturdayMorning.nextChange(at(2, 10)))
        assertEquals(at(3, 8, 30), weekdaysAndSaturdayMorning.nextChange(at(2, 20)))
        // Saturday afternoon: free until Monday morning.
        assertEquals(at(8, 8, 30), weekdaysAndSaturdayMorning.nextChange(at(6, 15)))
    }

    @Test fun allDayMondayToSaturdayRunsThroughMidnight() {
        val hours = ChargedHours.parse("1-6 00:00-24:00")!!
        assertTrue(hours.chargedAt(at(3, 23, 59)))
        assertFalse(hours.chargedAt(at(7, 0)))
        // Tuesday midnight isn't a change; the first is Sunday 00:00.
        assertEquals(at(7, 0), hours.nextChange(at(1, 12)))
        assertEquals(at(8, 0), hours.nextChange(at(7, 12)))
    }

    @Test fun alwaysChargedNeverChanges() {
        val hours = ChargedHours.parse("always")!!
        assertTrue(hours.chargedAt(at(7, 3)))
        assertNull(hours.nextChange(at(7, 3)))
    }

    @Test fun malformedOrMissingHoursAreUnknown() {
        for (bad in listOf(null, "", "weekdays", "8 09:00-10:00", "1-5 18:00-09:00", "1-5 09:00-25:00", "5-1 09:00-10:00")) {
            assertNull("$bad", ChargedHours.parse(bad))
        }
    }

    @Test fun freeNowCountsPaidBaysOutsideTheirHours() {
        val bay = paid("1-5 08:30-18:30")
        assertFalse(bay.isFreeAt(at(2, 10)))
        assertTrue(bay.isFreeAt(at(2, 19)))
        assertTrue(bay.isFreeAt(at(6, 10)))
        // A paid bay with unknown hours, and a bay of unknown price, are never claimed to be free.
        assertFalse(paid(null).isFreeAt(at(7, 3)))
        assertFalse(bay.copy(fee = Fee.Unknown).isFreeAt(at(7, 3)))
        assertTrue(bay.copy(fee = Fee.Free, charged = null).isFreeAt(at(2, 10)))
    }

    @Test fun statusSaysWhatIsTrueNowAndWhenItChanges() {
        val bay = paid("1-5 08:30-18:30;6 08:30-13:30")
        assertEquals("Charged now \u00b7 free from 6.30pm", bay.chargeStatus(at(2, 10)))
        assertEquals("Free right now \u00b7 charged from tomorrow 8.30am", bay.chargeStatus(at(2, 20)))
        assertEquals("Free right now \u00b7 charged from Mon 8.30am", bay.chargeStatus(at(6, 15)))
        assertEquals("Free right now \u00b7 charged from 8.30am", bay.chargeStatus(at(3, 7)))
        assertEquals("Charged now \u00b7 charged at all times", paid("always").chargeStatus(at(2, 10)))
        assertEquals("Charged now \u00b7 free from noon", paid("1-5 10:00-12:00").chargeStatus(at(2, 11)))
        assertNull(paid(null).chargeStatus(at(2, 10)))
        assertNull(bay.copy(fee = Fee.Free).chargeStatus(at(2, 10)))
    }
}
