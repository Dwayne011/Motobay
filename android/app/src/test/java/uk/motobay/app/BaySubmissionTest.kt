package uk.motobay.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import uk.motobay.app.data.Bay
import uk.motobay.app.data.BaySubmission
import uk.motobay.app.data.Fee
import uk.motobay.app.data.ModerationStatus
import uk.motobay.app.data.applySubmissions

class BaySubmissionTest {
    private val base = mapOf("node/1" to Bay("node/1", 51.5, -0.1, "Motorcycle parking"))

    private fun submission(editOf: String?, fee: Fee, status: ModerationStatus, at: Long = 1) = BaySubmission(
        id = "s$at", editOf = editOf, lat = 51.6, lon = -0.2, name = "", fee = fee, capacity = 4,
        covered = null, notes = "", author = "Rider", createdAt = at, status = status,
    )

    @Test fun approvedCorrectionUpdatesBay() {
        val result = applySubmissions(base, listOf(submission("node/1", Fee.Free, ModerationStatus.Approved)))
        val bay = result.getValue("node/1")
        assertEquals(Fee.Free, bay.fee)
        assertEquals(4, bay.capacity)
        assertEquals("Motorcycle parking", bay.name) // blank name keeps the original
        assertEquals(51.5, bay.lat, 0.0) // corrections don't move bays
    }

    @Test fun pendingAndRejectedAreIgnored() {
        val result = applySubmissions(base, listOf(
            submission("node/1", Fee.Free, ModerationStatus.Pending),
            submission(null, Fee.Paid, ModerationStatus.Rejected, at = 2),
        ))
        assertEquals(base, result)
    }

    @Test fun approvedNewBayIsAdded() {
        val result = applySubmissions(base, listOf(submission(null, Fee.Paid, ModerationStatus.Approved)))
        val added = result.getValue("rider/s1")
        assertEquals(Fee.Paid, added.fee)
        assertEquals("Rider", added.addedBy)
        assertEquals(2, result.size)
    }

    @Test fun newestCorrectionWins() {
        val result = applySubmissions(base, listOf(
            submission("node/1", Fee.Paid, ModerationStatus.Approved, at = 2),
            submission("node/1", Fee.Free, ModerationStatus.Approved, at = 1),
        ))
        assertEquals(Fee.Paid, result.getValue("node/1").fee)
        assertNull(result["rider/s1"])
    }
}
