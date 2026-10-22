package id.diskola.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Doc `08` Q14: a payment PIN must never reach Logcat or a log file. */
class LogRedactorTest {

    @Test
    fun `json string secrets are masked and other fields kept`() {
        val out = LogRedactor.redact("""{"password":"","pin":"123456","amount":5000}""")
        assertEquals("""{"password":"***","pin":"***","amount":5000}""", out)
    }

    @Test
    fun `numeric pin is masked too`() {
        assertEquals("""{"pin":"***","nominal":25000}""", LogRedactor.redact("""{"pin":123456,"nominal":25000}"""))
    }

    @Test
    fun `reset token and passwords are masked`() {
        val out = LogRedactor.redact("""{"pin":"654321","reset_token":"abc.def-123"}""")
        assertFalse(out.contains("654321"))
        assertFalse(out.contains("abc.def-123"))
        val change = LogRedactor.redact("""{"old_password":"lama1","new_password":"baru2"}""")
        assertFalse(change.contains("lama1") || change.contains("baru2"))
    }

    @Test
    fun `escaped quotes inside a secret do not leak the tail`() {
        val out = LogRedactor.redact("""{"password":"ab\"cd1234","x":1}""")
        assertFalse(out.contains("cd1234"))
        assertTrue(out.contains(""""x":1"""))
    }

    @Test
    fun `whitespace and nesting are handled`() {
        val out = LogRedactor.redact("{\n  \"data\": { \"pin\" : \"112233\" }\n}")
        assertFalse(out.contains("112233"))
    }

    @Test
    fun `form encoded secrets are masked`() {
        assertEquals("pin=***&amount=5000", LogRedactor.redact("pin=123456&amount=5000"))
        assertEquals("amount=5000&pin=***", LogRedactor.redact("amount=5000&pin=123456"))
    }

    @Test
    fun `text without secrets is unchanged`() {
        val plain = """{"status":"success","data":{"balance":150000}}"""
        assertEquals(plain, LogRedactor.redact(plain))
        assertEquals("", LogRedactor.redact(""))
        assertEquals("--> GET https://api/payment/wallet", LogRedactor.redact("--> GET https://api/payment/wallet"))
    }

    @Test
    fun `a field that only contains the word pin is not touched`() {
        val text = """{"pinned":"yes","shipping":"cheap"}"""
        assertEquals(text, LogRedactor.redact(text))
    }

    @Test
    fun `authorization and cookies are sensitive headers`() {
        assertTrue(LogRedactor.isSensitiveHeader("Authorization"))
        assertTrue(LogRedactor.isSensitiveHeader("cookie"))
        assertFalse(LogRedactor.isSensitiveHeader("Content-Type"))
    }
}
