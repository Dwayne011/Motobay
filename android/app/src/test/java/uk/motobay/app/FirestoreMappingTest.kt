package uk.motobay.app

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.motobay.app.data.Bay
import uk.motobay.app.data.BaySubmission
import uk.motobay.app.data.Fee
import uk.motobay.app.data.ModerationStatus
import uk.motobay.app.data.baySubmissionFrom
import uk.motobay.app.data.baySubmissionToMap
import uk.motobay.app.data.commentFrom
import uk.motobay.app.data.commentToMap
import uk.motobay.app.data.photoFrom
import uk.motobay.app.data.photoToMap
import uk.motobay.app.data.BayRating
import uk.motobay.app.data.CommunityData
import uk.motobay.app.data.RatingSummary
import uk.motobay.app.data.ratingSummary
import uk.motobay.app.data.ratingSummaryFrom
import java.util.Date

class FirestoreMappingTest {
    private val bay = Bay("node/1", 51.5, -0.1, "Bay")
    private val created = Timestamp(Date(1_700_000_000_000))

    @Test fun commentRoundTrip() {
        val map = commentToMap(bay, "Hello", "Sam", "uid1")
        assertEquals("Pending", map["status"])
        val read = commentFrom("c1", map + ("createdAt" to created))!!
        assertEquals("Hello", read.text)
        assertEquals("uid1", read.authorUid)
        assertEquals(ModerationStatus.Pending, read.status)
        assertEquals(1_700_000_000_000, read.createdAt)
    }

    @Test fun photoRoundTrip() {
        val map = photoToMap(bay, "https://firebasestorage.googleapis.com/x", "photos/uid1/p1.jpg", "Nice", "Sam", "uid1")
        val read = photoFrom("p1", map + ("createdAt" to created) + ("status" to "Approved"))!!
        assertEquals("https://firebasestorage.googleapis.com/x", read.url)
        assertEquals(ModerationStatus.Approved, read.status)
    }

    @Test fun baySubmissionLeavesOutEmptyOptionalFieldsAndRoundTrips() {
        val s = BaySubmission("", null, 51.6, -0.2, "New", Fee.Paid, null, true, "", "Sam", 0, ModerationStatus.Pending)
        val map = baySubmissionToMap(s, "uid1", ModerationStatus.Pending)
        assertFalse("editOf" in map)
        assertFalse("capacity" in map)
        assertEquals("Paid", map["fee"])
        val read = baySubmissionFrom("b1", map + ("createdAt" to created))!!
        assertEquals(Fee.Paid, read.fee)
        assertNull(read.capacity)
        assertEquals(true, read.covered)
        assertNull(read.editOf)
    }

    @Test fun adminAutoApprovedBayIsWrittenAsApproved() {
        val s = BaySubmission("", "node/1", 51.6, -0.2, "", Fee.Free, 4, null, "", "Dwayne", 0, ModerationStatus.Pending)
        val map = baySubmissionToMap(s, "admin1", ModerationStatus.Approved)
        assertEquals("Approved", map["status"])
        assertEquals("node/1", map["editOf"])
        assertEquals(4, map["capacity"])
    }

    @Test fun malformedDocumentsAreSkippedNotCrashed() {
        assertNull(commentFrom("c", mapOf("text" to "no bay")))
        assertNull(commentFrom("c", mapOf("bayId" to "node/1", "text" to "x", "status" to "Bogus")))
        assertNull(baySubmissionFrom("b", mapOf("lat" to 1.0, "status" to "Pending")))
        assertTrue(photoFrom("p", mapOf("bayId" to "node/1", "url" to "u", "status" to "Rejected"))!!.status == ModerationStatus.Rejected)
    }

    @Test fun ratingSummaryReadsTheFunctionsTotals() {
        assertEquals("node/1" to RatingSummary(4.5, 2), ratingSummaryFrom("node_1", mapOf("bayId" to "node/1", "count" to 2L, "total" to 9L)))
        assertNull(ratingSummaryFrom("x", mapOf("bayId" to "node/1", "count" to 0L, "total" to 0L)))
        assertNull(ratingSummaryFrom("x", mapOf("count" to 1L, "total" to 5L)))
    }

    @Test fun publicTotalsWinOverTheRidersOwnRatings() {
        val mine = BayRating("node/1", 5, "me", 0)
        // Totals from the Cloud Function include everyone's ratings.
        assertEquals(RatingSummary(3.0, 4), CommunityData(ratings = listOf(mine), summaries = mapOf("node/1" to RatingSummary(3.0, 4))).ratingSummary("node/1"))
        // Before the function has counted the bay, the rider still sees their own rating.
        assertEquals(RatingSummary(5.0, 1), CommunityData(ratings = listOf(mine)).ratingSummary("node/1"))
        assertNull(CommunityData().ratingSummary("node/1"))
    }
}
