package uk.motobay.app.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Every bay the app shows: OpenStreetMap bays, with approved rider corrections
 * applied, plus approved bays that riders added.
 */
fun combinedBays(
    osm: StateFlow<Map<String, Bay>>,
    community: StateFlow<CommunityData>,
    scope: CoroutineScope,
): StateFlow<Map<String, Bay>> =
    combine(osm, community) { base, data -> applySubmissions(base, data.bays) }
        .stateIn(scope, SharingStarted.Eagerly, emptyMap())

/** Applies approved submissions, oldest first, so the newest correction wins. */
fun applySubmissions(base: Map<String, Bay>, submissions: List<BaySubmission>): Map<String, Bay> {
    val approved = submissions.filter { it.status == ModerationStatus.Approved }.sortedBy { it.createdAt }
    if (approved.isEmpty()) return base
    val result = base.toMutableMap()
    for (s in approved) {
        if (s.editOf != null) {
            val bay = result[s.editOf] ?: continue
            result[bay.id] = bay.copy(
                name = s.name.ifBlank { bay.name },
                fee = if (s.fee == Fee.Unknown) bay.fee else s.fee,
                capacity = s.capacity ?: bay.capacity,
                covered = s.covered ?: bay.covered,
            )
        } else {
            val id = "rider/${s.id}"
            result[id] = Bay(
                id = id, lat = s.lat, lon = s.lon,
                name = s.name.ifBlank { "Motorcycle parking" },
                fee = s.fee, capacity = s.capacity, covered = s.covered,
                addedBy = s.author,
            )
        }
    }
    return result
}
