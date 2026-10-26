package uk.motobay.app

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.motobay.app.data.Bay
import uk.motobay.app.data.BayDataset
import uk.motobay.app.data.Fee
import uk.motobay.app.data.PlaceSearch
import uk.motobay.app.data.PlaceSuggestion

class DataParsingTest {
    /** Same shape as data/uk-bays.json written by scripts/fetch_uk_bays.py; empty fields are left out. */
    @Test fun decodesBayDataFile() {
        val body = """
            {"generated":"2026-10-26T03:17:00Z","attribution":"© OpenStreetMap contributors, ODbL 1.0","count":2,"bays":[
              {"id":"node/101","lat":51.51,"lon":-0.13,"name":"Motorcycle parking","fee":"free","covered":false,"capacity":12},
              {"id":"way/202","lat":51.52,"lon":-0.14,"name":"Soho Bike Bay","fee":"paid","maxStay":"4 hours"}
            ]}
        """.trimIndent()
        val data = Json { ignoreUnknownKeys = true }.decodeFromString(BayDataset.serializer(), body)
        assertEquals(2, data.bays.size)
        assertEquals(Fee.Free, data.bays[0].fee)
        assertEquals(12, data.bays[0].capacity)
        assertEquals(false, data.bays[0].covered)
        assertEquals(Fee.Paid, data.bays[1].fee)
        assertNull(data.bays[1].capacity)
        assertEquals("4 hours", data.bays[1].maxStay)
    }

    /** The real file published by the weekly job (data/uk-bays.json at the repo root). */
    @Test fun decodesPublishedDataFile() {
        val file = java.io.File("../../data/uk-bays.json")
        if (!file.exists()) return
        val data = Json { ignoreUnknownKeys = true }.decodeFromString(BayDataset.serializer(), file.readText())
        assertEquals(data.count, data.bays.size)
        assertTrue(data.bays.size > 1000)
        assertTrue(data.bays.all { it.lat in 49.0..61.5 && it.lon in -9.0..2.5 })
    }

    /** A real Photon response for "camden", recorded on 2026-10-25. */
    @Test fun parsesPhotonSuggestions() {
        val body = javaClass.classLoader!!.getResource("photon-camden.json").readText()
        val results = PlaceSearch().parsePhoton(body)
        assertTrue(results.isNotEmpty())
        val first = results.first()
        assertEquals("Camden Town", first.title)
        assertEquals(PlaceSuggestion.Kind.Town, first.kind)
        assertNotNull(first.point)
        assertTrue(first.subtitle.contains("Greater London"))
        assertTrue(results.any { it.title == "Camden Lock Market" && it.bbox != null })
        // House numbers aren't glued onto named places.
        assertTrue(results.none { it.title.first().isDigit() })
    }
}

class BayTitleTest {
    private fun bay(name: String, street: String? = null, area: String? = null, city: String? = null, postcode: String? = null) =
        Bay("node/1", 51.5, -0.1, name, street = street, area = area, city = city, postcode = postcode)

    @Test fun genericNameFallsBackToTheStreet() {
        val b = bay("Motorcycle parking", street = "Wentworth Street", area = "Spitalfields", city = "London", postcode = "E1")
        assertEquals("Wentworth Street", b.title)
        assertEquals("Spitalfields, London E1", b.locality)
    }

    @Test fun realNameKeepsTheStreetInTheLocality() {
        val b = bay("Soho Bike Bay", street = "Wardour Street", area = "Soho", city = "London", postcode = "W1")
        assertEquals("Soho Bike Bay", b.title)
        assertEquals("Wardour Street, Soho, London W1", b.locality)
    }

    @Test fun townIsNotRepeatedWhenItIsTheArea() {
        val b = bay("Solo M/C Bay", street = "Deansgate", area = "Manchester", city = "Manchester", postcode = "M3")
        assertEquals("Deansgate", b.title)
        assertEquals("Manchester M3", b.locality)
    }

    @Test fun nothingKnownGivesNoLocality() {
        val b = bay("Motorcycle parking")
        assertEquals("Motorcycle parking", b.title)
        assertNull(b.locality)
    }
}
