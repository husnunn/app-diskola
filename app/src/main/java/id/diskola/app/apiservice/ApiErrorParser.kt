package id.diskola.app.apiservice

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

data class ParsedApiError(
    val message: String? = null,
    val fieldErrors: Map<String, List<String>> = emptyMap(),
    val traceId: String? = null,
)

object ApiErrorParser {

    private val adapter by lazy {
        Moshi.Builder().build().adapter<Map<String, Any>>(
            Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
        )
    }

    fun parse(body: String?): ParsedApiError = try {
        if (body.isNullOrBlank()) ParsedApiError() else fromMap(adapter.fromJson(body))
    } catch (e: Exception) {
        ParsedApiError()
    }

    fun fromMap(content: Map<String, Any>?): ParsedApiError = try {
        if (content == null) ParsedApiError() else ParsedApiError(
            message = text(content["message"]) ?: text(content["error"]),
            fieldErrors = fieldErrors(content["errors"]),
            traceId = text(content["trace_id"])
                ?: text((content["data"] as? Map<*, *>)?.get("trace_id")),
        )
    } catch (e: Exception) {
        ParsedApiError()
    }

    private fun text(value: Any?): String? = (value as? String)?.takeIf { it.isNotBlank() }

    private fun fieldErrors(raw: Any?): Map<String, List<String>> {
        val errors = raw as? Map<*, *> ?: return emptyMap()
        val result = LinkedHashMap<String, List<String>>()
        errors.forEach { (key, value) ->
            val field = key as? String ?: return@forEach
            val messages = when (value) {
                is String -> listOf(value)
                is List<*> -> value.mapNotNull { text(it) }
                else -> emptyList()
            }.filter { it.isNotBlank() }
            if (messages.isNotEmpty()) result[field] = messages
        }
        return result
    }
}
