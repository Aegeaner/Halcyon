package dev.meteo.weather.data

import java.io.File
import java.security.MessageDigest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.put

/**
 * Tiny JSON-on-disk cache with a time-to-live; the same scheme as the terminal version
 * (`sha256(url+params)[:32].json`, `{stored_at, payload}`), but a per-app directory, so the two
 * tools never share files.
 *
 * A cache miss or an unreadable entry is never fatal.
 */
class ResponseCache(
    private val directory: File,
    private val ttlSeconds: Double = DEFAULT_TTL_SECONDS,
    private val clock: () -> Double = { System.currentTimeMillis() / 1000.0 },
) {
    private fun fileFor(key: String) = File(directory, "$key.json")

    fun get(key: String): JsonObject? {
        val stored = try {
            json.parseToJsonElement(fileFor(key).readText(Charsets.UTF_8)) as? JsonObject
        } catch (_: Exception) {
            return null
        } ?: return null

        val storedAt = (stored["stored_at"] as? JsonPrimitive)?.doubleOrNull ?: return null
        if (clock() - storedAt > ttlSeconds) return null
        return stored["payload"] as? JsonObject
    }

    fun put(key: String, payload: JsonObject) {
        try {
            directory.mkdirs()
            val body = buildJsonObject {
                put("stored_at", JsonPrimitive(clock()))
                put("payload", payload)
            }.toString()
            val tmp = File(directory, "$key.tmp")
            tmp.writeText(body, Charsets.UTF_8)
            tmp.renameTo(fileFor(key))
        } catch (_: Exception) {
            // A failure to write the cache is never fatal.
        }
    }

    companion object {
        const val DEFAULT_TTL_SECONDS = 3600.0

        private val json = Json { ignoreUnknownKeys = true }

        private val HEX = "0123456789abcdef".toCharArray()

        /** Deterministic key for a request: sha256 of the canonical `{params, url}` JSON, 32 hex. */
        fun makeKey(url: String, params: Map<String, Any>): String {
            val sorted = JsonObject(params.toSortedMap().mapValues { primitive(it.value) })
            val blob = buildJsonObject {
                put("params", sorted)
                put("url", url)
            }.toString()
            return MessageDigest.getInstance("SHA-256")
                .digest(blob.toByteArray(Charsets.UTF_8))
                .joinToString("") { byte ->
                    val unsigned = byte.toInt() and 0xFF
                    HEX[unsigned ushr 4].toString() + HEX[unsigned and 0x0F]
                }
                .take(32)
        }

        private fun primitive(value: Any): JsonPrimitive = when (value) {
            is String -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is Int -> JsonPrimitive(value)
            is Long -> JsonPrimitive(value)
            is Double -> JsonPrimitive(value)
            is Float -> JsonPrimitive(value)
            else -> JsonPrimitive(value.toString())
        }
    }
}
