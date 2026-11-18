package uk.motobay.app.data

import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** Where the rider last parked, kept on the phone so the charge alert survives a restart. */
@Serializable
data class Parked(
    val bayId: String,
    val title: String,
    val lat: Double,
    val lon: Double,
    val fee: Fee,
    /** The bay's charged hours when the rider parked, so the alert doesn't depend on the bay file. */
    val charged: String? = null,
    val parkedAt: Long,
)

/** What the app can tell a rider who has just parked, and when to alert them. */
sealed interface ParkingPlan {
    /** A free bay: nothing to watch for. */
    data object Free : ParkingPlan

    /** Charging hours aren't known, so there's no alert. */
    data object Unknown : ParkingPlan

    /** Charged at all times: pay now. */
    data object AlwaysCharged : ParkingPlan

    /** Charged now; free from [freeFrom] (null if that's more than a week away). */
    data class ChargedNow(val freeFrom: LocalDateTime?) : ParkingPlan

    /**
     * Free now; charging starts at [startsAt]. [alertAt] is when to notify: [LEAD_MINUTES]
     * before, or right away if that has already passed.
     */
    data class FreeUntil(val startsAt: LocalDateTime, val alertAt: LocalDateTime) : ParkingPlan

    /** Free now and charging doesn't start in the coming week. */
    data object FreeForAWeek : ParkingPlan

    companion object {
        const val LEAD_MINUTES = 15L
    }
}

fun Parked.plan(now: LocalDateTime): ParkingPlan = when (fee) {
    Fee.Free -> ParkingPlan.Free
    Fee.Unknown -> ParkingPlan.Unknown
    Fee.Paid -> {
        val hours = ChargedHours.parse(charged)
        when {
            hours == null -> ParkingPlan.Unknown
            charged == "always" -> ParkingPlan.AlwaysCharged
            hours.chargedAt(now) -> ParkingPlan.ChargedNow(hours.nextChange(now))
            else -> hours.nextChange(now)?.let { start ->
                val alert = start.minusMinutes(ParkingPlan.LEAD_MINUTES)
                ParkingPlan.FreeUntil(start, if (alert.isAfter(now)) alert else now)
            } ?: ParkingPlan.FreeForAWeek
        }
    }
}

fun Bay.parkedHere(at: LocalDateTime): Parked =
    Parked(id, title, lat, lon, fee, charged, at.atZone(UK).toInstant().toEpochMilli())

/** Every bay is in the UK, so its times are UK times whatever the phone's clock is set to. */
val UK: ZoneId = ZoneId.of("Europe/London")

fun LocalDateTime.epochMillis(): Long = ZonedDateTime.of(this, UK).toInstant().toEpochMilli()

/** "8.30am", "noon", "tomorrow 8.30am" or "Mon 8.30am", relative to [now]. */
fun clockText(now: LocalDateTime, at: LocalDateTime): String {
    val h = at.hour
    val clock = when {
        h == 0 && at.minute == 0 -> "midnight"
        h == 12 && at.minute == 0 -> "noon"
        else -> "${if (h % 12 == 0) 12 else h % 12}" + (if (at.minute == 0) "" else ".%02d".format(at.minute)) + if (h < 12) "am" else "pm"
    }
    val days = java.time.temporal.ChronoUnit.DAYS.between(now.toLocalDate(), at.toLocalDate())
    return when (days) {
        0L -> clock
        1L -> "tomorrow $clock"
        else -> at.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.UK) + " $clock"
    }
}

/** One line for the "Parked here" card. */
fun ParkingPlan.summary(now: LocalDateTime): String = when (this) {
    ParkingPlan.Free -> "Free bay, so no alert needed."
    ParkingPlan.Unknown -> "This bay's charging hours aren't known, so there's no alert. Check the sign."
    ParkingPlan.AlwaysCharged -> "Charged at all times. Pay now if you haven't."
    is ParkingPlan.ChargedNow -> "Charged now" + (freeFrom?.let { ", free from ${clockText(now, it)}" } ?: "") + ". Pay now if you haven't."
    is ParkingPlan.FreeUntil -> "Free until ${clockText(now, startsAt)}. We'll remind you ${ParkingPlan.LEAD_MINUTES} minutes before charging starts."
    ParkingPlan.FreeForAWeek -> "Free for at least the next week."
}
