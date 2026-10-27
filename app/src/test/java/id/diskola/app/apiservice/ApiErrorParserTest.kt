package id.diskola.app.apiservice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiErrorParserTest {

    @Test
    fun parse_readsMessageFieldErrorsAndTraceId() {
        val parsed = ApiErrorParser.parse(
            """
            {
              "status": "error",
              "message": "The given data was invalid.",
              "trace_id": "abc-123",
              "errors": {
                "link": ["Link tidak valid"],
                "file.1": ["Format tidak didukung", "Terlalu besar"],
                "title": "Judul wajib diisi"
              }
            }
            """.trimIndent()
        )
        assertEquals("The given data was invalid.", parsed.message)
        assertEquals("abc-123", parsed.traceId)
        assertEquals(listOf("Link tidak valid"), parsed.fieldErrors["link"])
        assertEquals(listOf("Format tidak didukung", "Terlalu besar"), parsed.fieldErrors["file.1"])
        assertEquals(listOf("Judul wajib diisi"), parsed.fieldErrors["title"])
    }

    @Test
    fun parse_readsTraceIdNestedInData() {
        val parsed = ApiErrorParser.parse("""{"message":"Diblokir","data":{"trace_id":"xyz"}}""")
        assertEquals("xyz", parsed.traceId)
    }

    @Test
    fun parse_fallsBackToErrorWhenMessageMissing() {
        assertEquals("Gagal", ApiErrorParser.parse("""{"error":"Gagal"}""").message)
    }

    @Test
    fun parse_neverThrowsOnBadBodies() {
        listOf(null, "", "   ", "<html>413</html>", "[1,2]", "{", """{"errors":"bukan map"}""").forEach {
            val parsed = ApiErrorParser.parse(it)
            assertTrue(parsed.fieldErrors.isEmpty())
            assertNull(parsed.traceId)
        }
    }

    @Test
    fun parse_skipsEmptyAndNonStringMessages() {
        val parsed = ApiErrorParser.parse("""{"errors":{"a":[],"b":[1,null,""],"c":["ok"]}}""")
        assertEquals(mapOf("c" to listOf("ok")), parsed.fieldErrors)
    }

    @Test
    fun fromMap_handlesNullAndBlankValues() {
        assertTrue(ApiErrorParser.fromMap(null).fieldErrors.isEmpty())
        val parsed = ApiErrorParser.fromMap(mapOf("message" to " ", "trace_id" to ""))
        assertNull(parsed.message)
        assertNull(parsed.traceId)
    }
}
