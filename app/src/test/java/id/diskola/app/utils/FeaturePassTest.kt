package id.diskola.app.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeaturePassTest {

    private var now = 1_000L
    private val pass = FeaturePass(clock = { now }, ttlMillis = 10_000L)

    @Test
    fun `no note means the opened screen asks the server itself`() {
        assertFalse(pass.consume("payment"))
    }

    @Test
    fun `a fresh note is used once`() {
        pass.grant("payment")
        now += 2_000
        assertTrue(pass.consume("payment"))
        assertFalse(pass.consume("payment"))
    }

    @Test
    fun `a stale note is not trusted and is dropped`() {
        pass.grant("payment")
        now += 10_001
        assertFalse(pass.consume("payment"))
        now = 1_000
        assertFalse(pass.consume("payment"))
    }

    @Test
    fun `the note is good right up to the limit`() {
        pass.grant("payment")
        now += 10_000
        assertTrue(pass.consume("payment"))
    }

    @Test
    fun `notes are kept per feature`() {
        pass.grant("presensi")
        assertFalse(pass.consume("payment"))
        assertTrue(pass.consume("presensi"))
    }

    @Test
    fun `a clock that went backwards does not pass`() {
        pass.grant("payment")
        now -= 1
        assertFalse(pass.consume("payment"))
    }
}
