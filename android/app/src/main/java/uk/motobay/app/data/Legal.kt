package uk.motobay.app.data

/** Legal text, links and attributions. Change the URLs here, nowhere else. */
object Legal {
    /** Replace with the hosted UK GDPR privacy policy before release. */
    const val PRIVACY_POLICY_URL = "https://dwayne011.github.io/Motobay/privacy.html"

    /** Google Play requires a web page where account deletion can be requested. */
    const val DELETE_ACCOUNT_URL = "https://dwayne011.github.io/Motobay/delete-account.html"

    /** Shown once on first launch, and always available under Settings > Legal. */
    const val DISCLAIMER = "Parking bay data is for guidance only. UK councils suspend bays and change permit rules " +
        "without notice. Always check physical street signage and bay markings before leaving your motorcycle. " +
        "This app accepts no liability for Penalty Charge Notices (PCNs), fines, or vehicle removal."

    /** Shown with the disclaimer and before the first post. */
    const val UGC_TERMS = "By posting photos or comments you agree not to upload abusive, illegal or misleading content. " +
        "Everything posted is checked before it is published and can be reported or removed."

    /** Why something is being reported; stored on the report document. */
    val REPORT_REASONS = listOf(
        "spam" to "Spam or advertising",
        "abusive" to "Abusive or offensive",
        "misleading" to "Wrong or misleading",
        "privacy" to "Shows a face or number plate",
        "other" to "Something else",
    )

    data class Attribution(val title: String, val text: String, val url: String)

    /** Every data source the Android app reads from, with the wording each licence asks for. */
    val ATTRIBUTIONS = listOf(
        Attribution(
            "Bay locations",
            "© OpenStreetMap contributors. Bay data comes from OpenStreetMap and is available under the Open Database Licence (ODbL).",
            "https://www.openstreetmap.org/copyright",
        ),
        Attribution(
            "Council bay details",
            "Prices, spaces and charging hours for some bays come from Camden, Islington, Hillingdon, Bromley, Enfield and Barnet councils and the City of London Corporation. " +
                "Bay positions and descriptions for Edinburgh, Leeds and Nottingham come from their councils' open data. " +
                "Contains public sector information licensed under the Open Government Licence v3.0. " +
                "Copyright City of Edinburgh Council and Nottingham City Council, contains Ordnance Survey data \u00a9 Crown copyright and database right 2026.",
            "https://www.nationalarchives.gov.uk/doc/open-government-licence/version/3/",
        ),
        Attribution(
            "Vehicle crime",
            "Vehicle crime near each bay comes from police-recorded crime published at data.police.uk (England, Wales and Northern Ireland). " +
                "Contains public sector information licensed under the Open Government Licence v3.0.",
            "https://data.police.uk/about/",
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
