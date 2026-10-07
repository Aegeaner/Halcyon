package dev.meteo.weather.data

import dev.meteo.weather.SyntheticPayload
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ResponseCacheTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private var now = 1_000_000.0
    private val directory: File get() = temporaryFolder.root

    private fun cache(ttl: Double = 3600.0) =
        ResponseCache(directory, ttlSeconds = ttl, clock = { now })

    @Test
    fun `stores and returns a payload`() {
        val cache = cache()
        val payload = SyntheticPayload.build(hours = 2, days = 1)

        cache.put("key", payload)

        assertEquals(payload, cache.get("key"))
    }

    @Test
    fun `expires entries after the time to live`() {
        val cache = cache(ttl = 10.0)
        cache.put("key", SyntheticPayload.build(hours = 1, days = 1))

        now += 9.0
        assertNotNull("entry must still be fresh", cache.get("key"))

        now += 2.0
        assertNull("entry must be expired", cache.get("key"))
    }

    @Test
    fun `treats a missing, corrupt or unexpected entry as a miss`() {
        val cache = cache()

        assertNull(cache.get("absent"))

        directory.mkdirs()
        File(directory, "corrupt.json").writeText("not json")
        assertNull(cache.get("corrupt"))

        File(directory, "wrongtype.json").writeText("""{"stored_at": 1000.0, "payload": [1, 2]}""")
        assertNull(cache.get("wrongtype"))

        File(directory, "nopayload.json").writeText("""{"stored_at": 1000.0}""")
        assertNull(cache.get("nopayload"))

        File(directory, "nostamp.json").writeText("""{"payload": {"latitude": 1}}""")
        assertNull(cache.get("nostamp"))
    }

    @Test
    fun `never fails when the cache directory cannot be created`() {
        // A regular file where the directory should go makes `mkdirs` fail.
        File(directory, "blocked").writeText("not a directory")
        val cache = ResponseCache(File(directory, "blocked/openmeteo"), clock = { now })

        cache.put("key", buildJsonObject { put("latitude", 1.0) })

        assertNull(cache.get("key"))
    }

    @Test
    fun `derives a stable key from the url and parameters, independent of order`() {
        val url = "https://api.open-meteo.com/v1/forecast"
        val params = linkedMapOf<String, Any>(
            "latitude" to 59.9139,
            "longitude" to 10.7522,
            "models" to "ecmwf_ifs",
            "forecast_days" to 16,
        )
        val reordered = linkedMapOf<String, Any>(
            "forecast_days" to 16,
            "models" to "ecmwf_ifs",
            "longitude" to 10.7522,
            "latitude" to 59.9139,
        )

        val key = ResponseCache.makeKey(url, params)
        assertEquals(32, key.length)
        assertEquals(key, ResponseCache.makeKey(url, params))
        assertEquals("key order must not matter", key, ResponseCache.makeKey(url, reordered))
        assertNotEquals(key, ResponseCache.makeKey(url, params + ("models" to "best_match")))
        assertNotEquals(key, ResponseCache.makeKey("$url?x=1", params))
    }

    @Test
    fun `reads a stored entry that carries extra fields`() {
        val cache = cache()
        cache.put("key", buildJsonObject { put("latitude", 59.9139) })

        val stored = File(directory, "key.json")
        val parsed = Json.parseToJsonElement(stored.readText()) as JsonObject
        stored.writeText(
            JsonObject(parsed.toMutableMap().apply { put("version", JsonPrimitive(2)) }).toString(),
        )

        val result = cache.get("key")
        assertNotNull(result)
        assertEquals(59.9139, (result!!["latitude"] as JsonPrimitive).content.toDouble(), 1e-9)
    }
}
