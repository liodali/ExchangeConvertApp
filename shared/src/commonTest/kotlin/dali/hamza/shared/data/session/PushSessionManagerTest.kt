package dali.hamza.shared.data.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The installId is the anonymous session identity — the server rejects
 * anything outside `^[A-Za-z0-9_-]{8,64}$` with a 400
 * (`INSTALL_ID_PATTERN` in AlertsRouting.kt).
 */
class PushSessionManagerTest {

    private val serverPattern = Regex("^[A-Za-z0-9_-]{8,64}$")

    @Test
    fun installIdMatchesServerPattern() {
        repeat(100) {
            val id = PushSessionManager.mintInstallId()
            assertTrue(serverPattern.matches(id), "installId $id would be rejected by the server")
        }
    }

    @Test
    fun installIdIsUuidV4Shaped() {
        val id = PushSessionManager.mintInstallId()
        assertEquals(36, id.length, "8-4-4-4-12 hex groups + dashes")
        assertEquals(5, id.split('-').size)
        assertEquals('4', id[14], "version nibble")
        // IETF variant: first hex digit of group 4 is 8/9/a/b
        assertTrue(id[19] in "89ab", "variant nibble was ${id[19]}")
    }

    @Test
    fun installIdsAreUnique() {
        val ids = (1..500).map { PushSessionManager.mintInstallId() }.toSet()
        assertEquals(500, ids.size)
    }
}
