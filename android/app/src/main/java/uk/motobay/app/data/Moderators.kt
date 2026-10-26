package uk.motobay.app.data

/**
 * Who can approve and reject submissions. This only decides what the app
 * shows; the real check is in firestore.rules and storage.rules, which must
 * list the same addresses (a unit test compares them).
 *
 * Google may report a googlemail.com account as gmail.com, so both spellings
 * of the admin address are listed.
 */
object Moderators {
    val EMAILS = setOf(
        "dwayne.edwards89@googlemail.com",
        "dwayne.edwards89@gmail.com",
    )

    fun isModerator(email: String?, emailVerified: Boolean, viaGoogle: Boolean): Boolean =
        emailVerified && viaGoogle && email?.lowercase() in EMAILS
}
