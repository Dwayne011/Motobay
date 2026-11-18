package uk.motobay.app

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import uk.motobay.app.data.AuthRepository
import uk.motobay.app.data.Bay
import uk.motobay.app.data.BayFeature
import uk.motobay.app.data.BayStatus
import uk.motobay.app.data.BayStatusRepository
import uk.motobay.app.data.MyBayReport
import uk.motobay.app.data.SpaceReport
import uk.motobay.app.data.Votes
import uk.motobay.app.data.epochMillis
import java.time.LocalDateTime

/** In-memory stand-in for the reports and the Cloud Function's totals. */
class FakeBayStatusRepository(private val auth: AuthRepository, private val now: () -> LocalDateTime) : BayStatusRepository {
    private data class Space(val bayId: String, val uid: String, val full: Boolean, val at: Long)
    private data class Features(val bayId: String, val uid: String, val values: Map<BayFeature, Boolean>)

    private val spaces = MutableStateFlow(listOf<Space>())
    private val features = MutableStateFlow(listOf<Features>())

    private fun uid() = auth.user.value?.uid ?: throw IllegalStateException("Sign in to report")

    override fun status(bayId: String): Flow<BayStatus?> = combine(spaces, features) { s, f ->
        val latest = s.filter { it.bayId == bayId }.maxByOrNull { it.at }?.let { SpaceReport(it.full, it.at) }
        val votes = BayFeature.entries.mapNotNull { feature ->
            val answers = f.filter { it.bayId == bayId }.mapNotNull { it.values[feature] }
            Votes(answers.count { it }, answers.count { !it }).takeIf { it.total > 0 }?.let { feature to it }
        }.toMap()
        if (latest == null && votes.isEmpty()) null else BayStatus(latest, votes)
    }

    override fun mine(bayId: String): Flow<MyBayReport?> {
        val me = auth.user.value?.uid ?: return MutableStateFlow(null)
        return combine(spaces, features) { s, f ->
            val space = s.firstOrNull { it.bayId == bayId && it.uid == me }
            MyBayReport(space?.full, space?.at, f.firstOrNull { it.bayId == bayId && it.uid == me }?.values.orEmpty())
        }
    }

    override suspend fun reportSpace(bay: Bay, full: Boolean) {
        val me = uid()
        spaces.update { l -> l.filterNot { it.bayId == bay.id && it.uid == me } + Space(bay.id, me, full, now().epochMillis()) }
    }

    override suspend fun setFeature(bay: Bay, feature: BayFeature, value: Boolean?) {
        val me = uid()
        features.update { l ->
            val old = l.firstOrNull { it.bayId == bay.id && it.uid == me }?.values.orEmpty()
            val new = if (value == null) old - feature else old + (feature to value)
            l.filterNot { it.bayId == bay.id && it.uid == me } + Features(bay.id, me, new)
        }
    }

    /** Another rider's report, for tests. */
    fun seedSpace(bayId: String, uid: String, full: Boolean, at: LocalDateTime) =
        spaces.update { it + Space(bayId, uid, full, at.epochMillis()) }

    fun seedFeature(bayId: String, uid: String, feature: BayFeature, value: Boolean) =
        features.update { it + Features(bayId, uid, mapOf(feature to value)) }

    override suspend fun deleteMine() {
        val me = uid()
        spaces.update { l -> l.filterNot { it.uid == me } }
        features.update { l -> l.filterNot { it.uid == me } }
    }
}
