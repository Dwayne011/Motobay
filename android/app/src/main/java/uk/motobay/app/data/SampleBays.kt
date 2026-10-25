package uk.motobay.app.data

/**
 * Made-up bays for offline development (gradle property motobay.sampleData=true).
 * These are NOT real locations and must never ship to riders.
 */
object SampleBays {
    val all: List<Bay> = listOf(
        sample(1, 51.5136, -0.1365, Fee.Free, capacity = 12, covered = false),
        sample(2, 51.5101, -0.1342, Fee.Paid, capacity = 8, maxStay = "4 hours"),
        sample(3, 51.5155, -0.1418, Fee.Unknown, covered = true),
        sample(4, 51.5079, -0.1281, Fee.Free, capacity = 20, openingHours = "24/7"),
        sample(5, 51.5122, -0.1236, Fee.Paid, capacity = 6),
        sample(6, 53.4794, -2.2453, Fee.Free, capacity = 10),
        sample(7, 55.9533, -3.1883, Fee.Paid, capacity = 15),
    )

    private fun sample(
        n: Int, lat: Double, lon: Double, fee: Fee,
        capacity: Int? = null, covered: Boolean? = null, maxStay: String? = null, openingHours: String? = null,
    ) = Bay(
        id = "sample/$n", lat = lat, lon = lon, name = "Sample bay $n", fee = fee, covered = covered,
        capacity = capacity, openingHours = openingHours, maxStay = maxStay, operator = null, street = null,
    )
}
