package uk.motobay.app.ui.explore

import uk.motobay.app.data.BBox
import uk.motobay.app.data.Bay
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

/**
 * A map marker: one bay, or a cluster of nearby bays at the current zoom.
 * Doing the clustering here (rather than letting MapLibre do it per tile)
 * means we know which cluster each bay came from, so pins can fly out of
 * their cluster when zooming in and gather back into it when zooming out.
 */
data class MapNode(
    val id: String,
    val lat: Double,
    val lon: Double,
    /** Bay ids inside this node; one entry for a plain bay. */
    val members: List<String>,
    /** The bay itself when this node is a single bay. */
    val bay: Bay?,
) {
    val isCluster: Boolean get() = bay == null
    val count: Int get() = members.size
}

/** Web Mercator pixel coordinates at a zoom, the units marker spacing is judged in. */
private class WorldPx(val x: Double, val y: Double)

private fun project(lat: Double, lon: Double, zoom: Int): WorldPx {
    // MapLibre renders the world 512 dp wide at zoom 0, so these are screen dp.
    val size = 512.0 * 2.0.pow(zoom)
    val x = (lon + 180) / 360 * size
    val latRad = lat.coerceIn(-85.05, 85.05) * PI / 180
    val y = (1 - ln(tan(latRad) + 1 / Math.cos(latRad)) / PI) / 2 * size
    return WorldPx(x, y)
}

/** Two markers closer than this many screen pixels merge into a cluster. */
const val CLUSTER_RADIUS_PX = 56

/** Below this zoom everything clusters; at or above it every bay is its own pin. */
const val CLUSTER_MAX_ZOOM = 16

private class Cluster(val seed: WorldPx) {
    val bays = mutableListOf<Bay>()
}

/**
 * Groups [bays] whose projected positions at [zoom] lie within
 * [CLUSTER_RADIUS_PX] of a cluster's first bay. Only bays inside [box] are
 * considered, so the work stays small for the whole UK. Bays are visited in
 * id order, so the same inputs always give the same clusters.
 */
fun clusterBays(bays: Collection<Bay>, zoom: Int, box: BBox): List<MapNode> {
    val visible = bays.filter { it in box }.sortedBy { it.id }
    if (zoom >= CLUSTER_MAX_ZOOM) {
        return visible.map { MapNode(it.id, it.lat, it.lon, listOf(it.id), it) }
    }
    // Grid of CLUSTER_RADIUS_PX cells: a bay can only join a cluster seeded in
    // its own or a neighbouring cell.
    val cells = HashMap<Long, MutableList<Cluster>>()
    val clusters = ArrayList<Cluster>()
    val r2 = CLUSTER_RADIUS_PX.toDouble() * CLUSTER_RADIUS_PX
    fun key(cx: Long, cy: Long) = (cx shl 32) or (cy and 0xffffffffL)
    for (bay in visible) {
        val p = project(bay.lat, bay.lon, zoom)
        val cx = Math.floor(p.x / CLUSTER_RADIUS_PX).toLong()
        val cy = Math.floor(p.y / CLUSTER_RADIUS_PX).toLong()
        var best: Cluster? = null
        var bestD = Double.MAX_VALUE
        for (dx in -1L..1L) for (dy in -1L..1L) {
            for (c in cells[key(cx + dx, cy + dy)] ?: continue) {
                val ddx = c.seed.x - p.x
                val ddy = c.seed.y - p.y
                val d = ddx * ddx + ddy * ddy
                if (d <= r2 && d < bestD) { best = c; bestD = d }
            }
        }
        val target = best ?: Cluster(p).also {
            clusters.add(it)
            cells.getOrPut(key(cx, cy)) { mutableListOf() }.add(it)
        }
        target.bays.add(bay)
    }
    return clusters.map { c ->
        val group = c.bays
        if (group.size == 1) {
            val b = group[0]
            MapNode(b.id, b.lat, b.lon, listOf(b.id), b)
        } else {
            val ids = group.map { it.id }
            MapNode(
                id = "cluster/${ids.first()}/${ids.size}",
                lat = group.sumOf { it.lat } / group.size,
                lon = group.sumOf { it.lon } / group.size,
                members = ids,
                bay = null,
            )
        }
    }
}

/** The zoom at which a cluster first splits into more than one node (for tap-to-expand). */
fun expansionZoom(members: Collection<Bay>, fromZoom: Int, box: BBox): Int {
    for (z in (fromZoom + 1)..CLUSTER_MAX_ZOOM) {
        if (clusterBays(members, z, box).size > 1) return z
    }
    return CLUSTER_MAX_ZOOM
}
