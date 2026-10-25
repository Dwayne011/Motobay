package uk.motobay.app

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import uk.motobay.app.data.Fee
import uk.motobay.app.data.OsmApi

/** Parses a response in Overpass's `out center tags` format. */
class OsmParsingTest {
    private val response = """
        {"version":0.6,"elements":[
          {"type":"node","id":101,"lat":51.51,"lon":-0.13,
           "tags":{"amenity":"motorcycle_parking","fee":"no","capacity":"12","covered":"no"}},
          {"type":"way","id":202,"center":{"lat":51.52,"lon":-0.14},
           "tags":{"amenity":"motorcycle_parking","fee":"yes","name":"Soho Bike Bay","maxstay":"4 hours","covered":"roof"}},
          {"type":"node","id":303,"lat":51.53,"lon":-0.15,
           "tags":{"amenity":"parking","motorcycle":"designated","addr:street":"Brewer Street","capacity:motorcycle":"6"}},
          {"type":"node","id":404,"tags":{"amenity":"motorcycle_parking"}}
        ]}
    """.trimIndent()

    private val bays = Json.parseToJsonElement(response).jsonObject["elements"]!!.jsonArray
        .map { OsmApi.parseElement(it.jsonObject) }

    @Test fun freeNode() {
        val b = bays[0]!!
        assertEquals("node/101", b.id)
        assertEquals(Fee.Free, b.fee)
        assertEquals(12, b.capacity)
        assertEquals(false, b.covered)
        assertEquals("Motorcycle parking", b.name)
    }

    @Test fun paidWayUsesCentre() {
        val b = bays[1]!!
        assertEquals("way/202", b.id)
        assertEquals(51.52, b.lat, 0.0)
        assertEquals(Fee.Paid, b.fee)
        assertEquals("Soho Bike Bay", b.name)
        assertEquals("4 hours", b.maxStay)
        assertEquals(true, b.covered)
    }

    @Test fun carParkWithMotorcycleSpaces() {
        val b = bays[2]!!
        assertEquals(Fee.Unknown, b.fee)
        assertEquals(6, b.capacity)
        assertEquals("Brewer Street motorcycle bay", b.name)
    }

    @Test fun elementWithoutCoordinatesIsSkipped() {
        assertNull(bays[3])
    }
}
