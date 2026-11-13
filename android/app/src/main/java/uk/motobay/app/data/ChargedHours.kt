package uk.motobay.app.data

import java.time.LocalDateTime

/**
 * When a paid bay is charged, parsed from the data file's `charged` field:
 * "always", or day ranges (1 = Monday) with 24-hour times separated by ";",
 * e.g. "1-5 08:30-18:30;6 08:30-13:30". Outside those windows the bay is free.
 * Match-day and bank-holiday differences are not in the data.
 */
class ChargedHours private constructor(private val always: Boolean, private val windows: List<Window>) {
    /** [from] and [to] are minutes since midnight; [to] may be 1440 for the end of the day. */
    private data class Window(val days: IntRange, val from: Int, val to: Int)

    fun chargedAt(t: LocalDateTime): Boolean {
        if (always) return true
        val day = t.dayOfWeek.value
        val minute = t.hour * 60 + t.minute
        return windows.any { day in it.days && minute >= it.from && minute < it.to }
    }

    /** The next moment the bay switches between charged and free, or null if it never does. */
    fun nextChange(t: LocalDateTime): LocalDateTime? {
        if (always || windows.isEmpty()) return null
        val now = chargedAt(t)
        val midnight = t.toLocalDate().atStartOfDay()
        return (0L..7L).asSequence()
            .flatMap { d -> windows.asSequence().flatMap { w -> sequenceOf(w.from, w.to).map { midnight.plusDays(d).plusMinutes(it.toLong()) } } }
            .filter { it.isAfter(t) }
            .sorted()
            .firstOrNull { chargedAt(it) != now }
    }

    companion object {
        private val WINDOW = Regex("""([1-7])(?:-([1-7]))? (\d{2}):(\d{2})-(\d{2}):(\d{2})""")

        /** Null when there is nothing to parse or it isn't in the expected form. */
        fun parse(spec: String?): ChargedHours? {
            if (spec.isNullOrBlank()) return null
            if (spec == "always") return ChargedHours(true, emptyList())
            val windows = spec.split(';').map { part ->
                val m = WINDOW.matchEntire(part.trim()) ?: return null
                val (d1, d2, h1, m1, h2, m2) = m.destructured
                val from = h1.toInt() * 60 + m1.toInt()
                val to = h2.toInt() * 60 + m2.toInt()
                val last = d2.ifEmpty { d1 }.toInt()
                if (from >= to || to > 1440 || last < d1.toInt()) return null
                Window(d1.toInt()..last, from, to)
            }
            return ChargedHours(false, windows)
        }
    }
}

/** Free to park at [t]: a free bay, or a paid one outside its charged hours. Unknown when the hours aren't known. */
fun Bay.isFreeAt(t: LocalDateTime): Boolean = when (fee) {
    Fee.Free -> true
    Fee.Paid -> ChargedHours.parse(charged)?.let { !it.chargedAt(t) } ?: false
    Fee.Unknown -> false
}

/**
 * "Free right now · charged from Mon 8.30am" or "Charged now · free from 6.30pm"
 * for a paid bay whose charged hours are known; null otherwise.
 */
fun Bay.chargeStatus(t: LocalDateTime): String? {
    if (fee != Fee.Paid) return null
    val hours = ChargedHours.parse(charged) ?: return null
    val charged = hours.chargedAt(t)
    val change = hours.nextChange(t)?.let { whenText(t, it) }
    return when {
        charged -> "Charged now" + (change?.let { " · free from $it" } ?: " · charged at all times")
        else -> "Free right now" + (change?.let { " · charged from $it" } ?: "")
    }
}

private fun whenText(now: LocalDateTime, at: LocalDateTime): String {
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
