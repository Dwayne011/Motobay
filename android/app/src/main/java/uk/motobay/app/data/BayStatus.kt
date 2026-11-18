package uk.motobay.app.data

import kotlinx.coroutines.flow.Flow

/** Safety details riders can confirm for a bay. [key] is the Firestore field name. */
enum class BayFeature(val key: String, val label: String) {
    Anchor("anchor", "Ground anchor or rail"),
    Cctv("cctv", "CCTV"),
    Lit("lit", "Lit at night"),
}

/** The latest "space now" or "full" report for a bay. [at] is epoch millis. */
data class SpaceReport(val full: Boolean, val at: Long)

/** How many riders said yes and no to a feature. */
data class Votes(val yes: Int, val no: Int) {
    val total get() = yes + no
}

/**
 * What riders have said about a bay, as public totals: nobody can see which rider
 * said what, or where they were when they said it. Kept by the Cloud Function.
 */
data class BayStatus(val space: SpaceReport?, val features: Map<BayFeature, Votes>)

/** What the signed-in rider has said about one bay, so the buttons show their answers. */
data class MyBayReport(val full: Boolean?, val spaceAt: Long?, val features: Map<BayFeature, Boolean>)

/**
 * Live reports from signed-in riders: "space now" or "full", and safety details.
 * They show straight away (no moderation), one report per rider per bay at a time.
 */
interface BayStatusRepository {
    fun status(bayId: String): Flow<BayStatus?>
    fun mine(bayId: String): Flow<MyBayReport?>
    suspend fun reportSpace(bay: Bay, full: Boolean)

    /** [value] null takes the rider's answer back. */
    suspend fun setFeature(bay: Bay, feature: BayFeature, value: Boolean?)

    /** Every report the signed-in rider made, for account deletion. */
    suspend fun deleteMine()
}

/** Reports older than this aren't shown: a bay that was full this morning may not be now. */
const val SPACE_REPORT_FRESH_MS = 3 * 60 * 60 * 1000L

/** "Space reported 20 min ago" or "Reported full 2 hr ago", or null when there's no fresh report. */
fun SpaceReport.text(nowMillis: Long): String? {
    val age = nowMillis - at
    if (age > SPACE_REPORT_FRESH_MS || age < -60_000) return null
    val minutes = (age / 60_000).coerceAtLeast(0)
    val ago = when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        else -> "${minutes / 60} hr ago"
    }
    return if (full) "Reported full $ago" else "Space reported $ago"
}

/** "3 riders say yes", "1 rider says no", or "Riders disagree (2 yes, 1 no)". */
fun Votes.text(): String = when {
    yes > 0 && no == 0 -> if (yes == 1) "1 rider says yes" else "$yes riders say yes"
    no > 0 && yes == 0 -> if (no == 1) "1 rider says no" else "$no riders say no"
    else -> "Riders disagree ($yes yes, $no no)"
}
