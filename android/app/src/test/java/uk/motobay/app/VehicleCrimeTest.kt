package uk.motobay.app

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import uk.motobay.app.data.BayCrime
import uk.motobay.app.data.VehicleCrimeData
import uk.motobay.app.ui.bay.comparison

class VehicleCrimeTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test fun parsesTheFileTheWorkflowWrites() {
        val data = json.decodeFromString(
            VehicleCrimeData.serializer(),
            """{"generated":"x","attribution":"OGL","months":["2026-09","2026-08","2026-07"],"radiusMetres":200,"typical":3,
               "bays":{"node/1":7,"node/2":0},"noData":["node/9"],"points":[[51.5,-0.12,2],[51.51,-0.13,1]]}""",
        )
        assertEquals(BayCrime.Count(7, 3, 200, "Jul–Sep 2026"), data.forBay("node/1"))
        assertEquals(BayCrime.Count(0, 3, 200, "Jul–Sep 2026"), data.forBay("node/2"))
        assertEquals(BayCrime.NoData, data.forBay("node/9"))
        assertNull(data.forBay("node/unknown")) // a bay added since the file was built: say nothing
        assertEquals(2, data.points.size)
    }

    @Test fun periodSpansANewYear() {
        assertEquals("Nov 2026 – Jan 2027", VehicleCrimeData(months = listOf("2027-01", "2026-12", "2026-11")).period())
        assertEquals("Sep 2026", VehicleCrimeData(months = listOf("2026-09")).period())
        assertEquals("", VehicleCrimeData().period())
    }

    @Test fun comparisonWithATypicalBay() {
        assertEquals("None were reported this close.", comparison(0, 3))
        assertEquals("This is below most bays.", comparison(1, 3))
        assertEquals("This is about average.", comparison(3, 3))
        assertEquals("This is above most bays.", comparison(5, 3))
        assertEquals("This is well above most bays.", comparison(12, 3))
        // With a typical of 0, one report is above, not "well above".
        assertEquals("This is above most bays.", comparison(1, 0))
    }
}
