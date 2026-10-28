package uk.motobay.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.motobay.app.data.Moderators
import java.io.File

class ModeratorsTest {
    @Test fun onlyAVerifiedGoogleAccountWithTheAdminAddressIsAModerator() {
        val admin = "dwayne.edwards89@googlemail.com"
        assertTrue(Moderators.isModerator(admin, emailVerified = true, viaGoogle = true))
        assertTrue(Moderators.isModerator("Dwayne.Edwards89@GoogleMail.com", emailVerified = true, viaGoogle = true))
        assertTrue(Moderators.isModerator("dwayne.edwards89@gmail.com", emailVerified = true, viaGoogle = true))
        assertFalse(Moderators.isModerator(admin, emailVerified = false, viaGoogle = true))
        assertFalse(Moderators.isModerator(admin, emailVerified = true, viaGoogle = false))
        assertFalse(Moderators.isModerator("someone@example.com", emailVerified = true, viaGoogle = true))
        assertFalse(Moderators.isModerator(null, emailVerified = true, viaGoogle = true))
    }

    /** The app only decides what to show; the rules decide what's allowed. They must agree. */
    @Test fun rulesListTheSameAdminAddressesAsTheApp() {
        for (name in listOf("firestore.rules", "storage.rules")) {
            val rules = File("../../$name").readText()
            val list = Regex("""request\.auth\.token\.email in \[(.*?)]""", RegexOption.DOT_MATCHES_ALL).find(rules)?.groupValues?.get(1)
                ?: error("$name has no admin list")
            val emails = Regex("""'([^']+)'""").findAll(list).map { it.groupValues[1] }.toSet()
            assertEquals("$name admin list", Moderators.EMAILS, emails)
        }
    }
}
