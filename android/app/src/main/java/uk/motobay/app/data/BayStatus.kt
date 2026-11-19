package uk.motobay.app.data

import kotlinx.coroutines.flow.Flow

/** Safety details riders can confirm for a bay. [key] is the Firestore field name. */
enum class BayFeature(val key: String, val label: String) {
    Anchor("anchor", "Ground anchor or rail"),
    Cctv("cctv", "CCTV"),
    Lit("lit", "Lit at night"),
}

/**
 * The latest "space now", "full" or "suspended" report for a bay. [at] is epoch millis.
 * A suspended bay (coned off for works, say) is reported as [suspended], with [full] true.
 */
data class SpaceReport(val full: Boolean, val at: Long, val suspended: Boolean = false)

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
data class MyBayReport(val full: Boolean?, val spaceAt: Long?, val features: Map<BayFeature, Boolean>, val suspended: Boolean = false)

/**
 * Live reports from signed-in riders: "space now" or "full", and safety details.
 * They show straight away (no moderation), one report per rider per bay at a time.
 */
interface BayStatusRepository {
    fun status(bayId: String): Flow<BayStatus?>
    fun mine(bayId: String): Flow<MyBayReport?>
    suspend fun reportSpace(bay: Bay, full: Boolean)

    /** The bay is suspended (coned off or signed out of use). */
    suspend fun reportSuspended(bay: Bay)

    /** [value] null takes the rider's answer back. */
    suspend fun setFeature(bay: Bay, feature: BayFeature, value: Boolean?)

    /** Every report the signed-in rider made, for account deletion. */
    suspend fun deleteMine()
}

/** Reports older than this aren't shown: a bay that was full this morning may not be now. */
const val SPACE_REPORT_FRESH_MS = 3 * 60 * 60 * 1000L

/** Suspensions usually last days, so a suspended report stays up longer. */
const val SUSPENDED_REPORT_FRESH_MS = 48 * 60 * 60 * 1000L

/** How long this report stays visible. */
val SpaceReport.freshFor: Long get() = if (suspended) SUSPENDED_REPORT_FRESH_MS else SPACE_REPORT_FRESH_MS

/** "Space reported 20 min ago", "Reported full 2 hr ago" or "Reported suspended 1 day ago", or null when there's no fresh report. */
fun SpaceReport.text(nowMillis: Long): String? {
    val age = nowMillis - at
    if (age > freshFor || age < -60_000) return null
    val minutes = (age / 60_000).coerceAtLeast(0)
    val ago = when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 24 * 60 -> "${minutes / 60} hr ago"
        else -> "${minutes / (24 * 60)} day ago"
    }
    return when {
        suspended -> "Reported suspended $ago"
        full -> "Reported full $ago"
        else -> "Space reported $ago"
    }
}

/** "3 riders say yes", "1 rider says no", or "Riders disagree (2 yes, 1 no)". */
fun Votes.text(): String = when {
    yes > 0 && no == 0 -> if (yes == 1) "1 rider says yes" else "$yes riders say yes"
    no > 0 && yes == 0 -> if (no == 1) "1 rider says no" else "$no riders say no"
    else -> "Riders disagree ($yes yes, $no no)"
}
