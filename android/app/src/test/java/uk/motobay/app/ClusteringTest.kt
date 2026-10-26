package uk.motobay.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.motobay.app.data.BBox
import uk.motobay.app.data.Bay
import uk.motobay.app.ui.explore.CLUSTER_MAX_ZOOM
import uk.motobay.app.ui.explore.clusterBays
import uk.motobay.app.ui.explore.expansionZoom

class ClusteringTest {
    private val london = BBox(51.4, -0.3, 51.6, 0.1)
    private fun bay(i: Int, lat: Double, lon: Double) = Bay("node/$i", lat, lon, "Bay $i")

    // Two bays 30 m apart and one 3.3 km away (about 35 dp apart at zoom 9, 70 dp at zoom 10).
    private val bays = listOf(bay(1, 51.5000, -0.1000), bay(2, 51.5002, -0.1002), bay(3, 51.5270, -0.0800))

    @Test fun farApartAtRegionalZoomTheyAllCluster() {
        val nodes = clusterBays(bays, zoom = 9, box = london)
        assertEquals(1, nodes.size)
        assertEquals(3, nodes[0].count)
        assertTrue(nodes[0].isCluster)
    }

    @Test fun atStreetZoomThePairStaysTogetherAndTheOtherIsAlone() {
        val nodes = clusterBays(bays, zoom = 14, box = london)
        assertEquals(2, nodes.size)
        assertEquals(setOf(2, 1), nodes.map { it.count }.toSet())
    }

    @Test fun atMaxZoomEveryBayIsItsOwnPin() {
        val nodes = clusterBays(bays, zoom = CLUSTER_MAX_ZOOM, box = london)
        assertEquals(3, nodes.size)
        assertTrue(nodes.none { it.isCluster })
    }

    @Test fun baysOutsideTheBoxAreIgnored() {
        val nodes = clusterBays(bays, zoom = 14, box = BBox(51.52, -0.09, 51.53, -0.07))
        assertEquals(listOf("node/3"), nodes.map { it.id })
    }

    @Test fun clustersAreStableAcrossCalls() {
        assertEquals(clusterBays(bays, 12, london).map { it.id }, clusterBays(bays, 12, london).map { it.id })
    }

    @Test fun expansionZoomIsWhereTheClusterSplits() {
        val z = expansionZoom(bays, fromZoom = 9, box = london)
        assertTrue(z in 10..CLUSTER_MAX_ZOOM)
        assertTrue(clusterBays(bays, z, london).size > 1)
        assertEquals(1, clusterBays(bays, z - 1, london).size)
    }
}
