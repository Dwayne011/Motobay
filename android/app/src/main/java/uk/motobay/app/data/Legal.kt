package uk.motobay.app.data

/** Legal text, links and attributions. Change the URLs here, nowhere else. */
object Legal {
    /** Replace with the hosted UK GDPR privacy policy before release. */
    const val PRIVACY_POLICY_URL = "https://example.com/motobays/privacy"

    /** Shown once on first launch, and always available under Settings > Legal. */
    const val DISCLAIMER = "Parking bay data is for guidance only. UK councils suspend bays and change permit rules " +
        "without notice. Always check physical street signage and bay markings before leaving your motorcycle. " +
        "This app accepts no liability for Penalty Charge Notices (PCNs), fines, or vehicle removal."

    data class Attribution(val title: String, val text: String, val url: String)

    /** Every data source the Android app reads from, with the wording each licence asks for. */
    val ATTRIBUTIONS = listOf(
        Attribution(
            "Bay locations",
            "© OpenStreetMap contributors. Bay data comes from OpenStreetMap and is available under the Open Database Licence (ODbL).",
            "https://www.openstreetmap.org/copyright",
        ),
        Attribution(
            "Map tiles",
            "Map tiles by OpenFreeMap, rendered from OpenStreetMap data. © OpenStreetMap contributors.",
            "https://openfreemap.org",
        ),
        Attribution(
            "Place search",
            "Search suggestions by Photon (komoot), built on OpenStreetMap data. © OpenStreetMap contributors.",
            "https://photon.komoot.io",
        ),
        Attribution(
            "Postcode lookup",
            "Postcode search by postcodes.io. Contains OS data © Crown copyright and database right; contains Royal Mail data " +
                "© Royal Mail copyright and database right; contains National Statistics data © Crown copyright and database right. " +
                "Contains public sector information licensed under the Open Government Licence v3.0.",
            "https://postcodes.io/about",
        ),
        Attribution(
            "Accounts and photos",
            "Sign-in, photo storage and the review queue run on Google Firebase. Your photos and comments are stored there until you delete them.",
            "https://firebase.google.com/support/privacy",
        ),
    )
}
