package uk.motobay.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import uk.motobay.app.data.Bay
import uk.motobay.app.data.Fee
import uk.motobay.app.data.ParkingPlan
import uk.motobay.app.data.Payment
import uk.motobay.app.data.SpaceReport
import uk.motobay.app.data.Votes
import uk.motobay.app.data.parkedHere
import uk.motobay.app.data.payment
import uk.motobay.app.data.plan
import uk.motobay.app.data.summary
import uk.motobay.app.data.text
import java.time.LocalDateTime

class ParkingTest {
    private val paid = Bay("council/islington/a-1", 51.55, -0.09, "Motorcycle parking", Fee.Paid, charged = "1-5 08:30-18:30", street = "Aberdeen Road")
    private val tue = { h: Int, m: Int -> LocalDateTime.of(2026, 11, 10, h, m) } // a Tuesday

    @Test fun `parked before charging starts gets a reminder 15 minutes before`() {
        val plan = paid.parkedHere(tue(7, 0)).plan(tue(7, 0))
        assertEquals(ParkingPlan.FreeUntil(tue(8, 30), tue(8, 15)), plan)
        assertEquals("Free until 8.30am. We'll remind you 15 minutes before charging starts.", plan.summary(tue(7, 0)))
    }

    @Test fun `parked less than 15 minutes before, the reminder comes at once`() {
        assertEquals(ParkingPlan.FreeUntil(tue(8, 30), tue(8, 20)), paid.parkedHere(tue(8, 20)).plan(tue(8, 20)))
    }

    @Test fun `parked in the evening, the reminder is for the next morning`() {
        val plan = paid.parkedHere(tue(19, 0)).plan(tue(19, 0))
        assertEquals(ParkingPlan.FreeUntil(tue(8, 30).plusDays(1), tue(8, 15).plusDays(1)), plan)
        assertEquals("Free until tomorrow 8.30am. We'll remind you 15 minutes before charging starts.", plan.summary(tue(19, 0)))
    }

    @Test fun `parked on a Friday evening, the reminder is for Monday`() {
        val fri = LocalDateTime.of(2026, 11, 13, 19, 0)
        val plan = paid.parkedHere(fri).plan(fri) as ParkingPlan.FreeUntil
        assertEquals(LocalDateTime.of(2026, 11, 16, 8, 30), plan.startsAt)
        assertEquals("Free until Mon 8.30am. We'll remind you 15 minutes before charging starts.", plan.summary(fri))
    }

    @Test fun `parked while charged says so, with no reminder`() {
        val plan = paid.parkedHere(tue(10, 30)).plan(tue(10, 30))
        assertEquals(ParkingPlan.ChargedNow(tue(18, 30)), plan)
        assertEquals("Charged now, free from 6.30pm. Pay now if you haven't.", plan.summary(tue(10, 30)))
    }

    @Test fun `free bays and bays with unknown hours get no reminder`() {
        assertEquals(ParkingPlan.Free, paid.copy(fee = Fee.Free).parkedHere(tue(7, 0)).plan(tue(7, 0)))
        assertEquals(ParkingPlan.Unknown, paid.copy(fee = Fee.Unknown).parkedHere(tue(7, 0)).plan(tue(7, 0)))
        assertEquals(ParkingPlan.Unknown, paid.copy(charged = null).parkedHere(tue(7, 0)).plan(tue(7, 0)))
        assertEquals(ParkingPlan.AlwaysCharged, paid.copy(charged = "always").parkedHere(tue(7, 0)).plan(tue(7, 0)))
    }

    @Test fun `the RingGo location code comes from the council's rules`() {
        val islington = paid.copy(rules = "Paid Mon–Fri 8.30am–6.30pm; free outside those hours. Pay by RingGo, location 54137. Controlled zone Q.")
        assertEquals(Payment("RingGo", "54137"), islington.payment)
        assertNull(paid.copy(rules = "Paid at all times.").payment)
        assertNull(paid.payment)
    }

    @Test fun `space reports say how long ago, and old ones are hidden`() {
        val now = 10_000_000_000L
        assertEquals("Space reported just now", SpaceReport(false, now - 20_000).text(now))
        assertEquals("Reported full 20 min ago", SpaceReport(true, now - 20 * 60_000).text(now))
        assertEquals("Space reported 2 hr ago", SpaceReport(false, now - 130 * 60_000).text(now))
        assertNull(SpaceReport(true, now - 4 * 60 * 60_000).text(now))
    }

    @Test fun `safety votes read naturally`() {
        assertEquals("1 rider says yes", Votes(1, 0).text())
        assertEquals("3 riders say no", Votes(0, 3).text())
        assertEquals("Riders disagree (2 yes, 1 no)", Votes(2, 1).text())
    }
}
