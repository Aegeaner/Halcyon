package dev.meteo.weather.data

import dev.meteo.weather.LocalHttpServer
import dev.meteo.weather.SyntheticPayload
import dev.meteo.weather.data.model.OSLO
import java.io.File
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class OpenMeteoApiTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val servers = mutableListOf<LocalHttpServer>()
    private val client: OkHttpClient = OpenMeteoApi.defaultClient()

    @After
    fun tearDown() {
        servers.forEach { it.close() }
        client.connectionPool.evictAll()
        client.dispatcher.executorService.shutdown()
    }

    private fun serve(body: String, status: Int = 200): LocalHttpServer =
        LocalHttpServer(body, status).also { servers += it }

    private fun api(server: LocalHttpServer): OpenMeteoApi = OpenMeteoApi(
        client = client,
        forecastUrl = "${server.baseUrl}/v1/forecast",
        geocodingUrl = "${server.baseUrl}/v1/search",
    )

    private fun cache(directory: File = temporaryFolder.newFolder()): ResponseCache =
        responseCache(directory)

    @Test
    fun `request carries the query contract of the terminal version`() {
        val server = serve(SyntheticPayload.build().toString())
        val forecast = api(server).fetchForecast(OSLO, days = 16)

        val query = server.queries.single()
        assertTrue(query, query.contains("latitude=59.9139"))
        assertTrue(query, query.contains("longitude=10.7522"))
        assertTrue(query, query.contains("models=ecmwf_ifs"))
        assertTrue(query, query.contains("forecast_days=16"))
        assertTrue(query, query.contains("timezone=auto"))
        assertTrue(query, query.contains("temperature_2m"))
        assertTrue(query, query.contains("boundary_layer_height"))
        assertTrue(query, query.contains("uv_index_max"))
        assertTrue(query, query.contains("wind_direction_10m_dominant"))
        assertTrue(server.userAgents.single().startsWith("halcyon"))
        assertEquals("application/json", server.accepts.single())

        assertEquals(384, forecast.hours.size)
        assertEquals(16, forecast.days.size)
        assertEquals("ecmwf_ifs", forecast.model)
        assertEquals(OSLO, forecast.place)
    }

    @Test
    fun `rounds coordinates to six decimals`() {
        val server = serve(SyntheticPayload.build().toString())
        api(server).fetchForecast(OSLO.copy(latitude = 59.91390049, longitude = 10.7522499))

        val query = server.queries.single()
        assertTrue(query, query.contains("latitude=59.9139"))
        assertTrue(query, query.contains("longitude=10.7522"))
    }

    @Test
    fun `clamps the forecast length to the supported range`() {
        val server = serve(SyntheticPayload.build().toString())
        val api = api(server)

        api.fetchForecast(OSLO, days = 99)
        api.fetchForecast(OSLO, days = 0)

        assertTrue(server.queries[0], server.queries[0].contains("forecast_days=16"))
        assertTrue(server.queries[1], server.queries[1].contains("forecast_days=1"))
    }

    @Test
    fun `treats a false error flag as success`() {
        val payload = JsonObject(
            SyntheticPayload.build().toMutableMap().apply { put("error", JsonPrimitive(false)) },
        )
        val server = serve(payload.toString())

        val forecast = api(server).fetchForecast(OSLO)

        assertEquals(384, forecast.hours.size)
    }

    @Test
    fun `reports the API error reason`() {
        val server = serve(SyntheticPayload.error("Cannot initialize Forecast").toString())

        val thrown = assertThrows(OpenMeteoException::class.java) {
            api(server).fetchForecast(OSLO)
        }

        assertEquals("Cannot initialize Forecast", thrown.message)
    }

    @Test
    fun `rejects a failing HTTP status`() {
        val server = serve("nope", status = 500)

        val thrown = assertThrows(OpenMeteoException::class.java) {
            api(server).fetchForecast(OSLO)
        }

        assertTrue(thrown.message!!, thrown.message!!.contains("500"))
    }

    @Test
    fun `rejects malformed and unexpected bodies`() {
        val malformed = serve("not json at all")
        assertThrows(OpenMeteoException::class.java) { api(malformed).fetchForecast(OSLO) }

        val array = serve("[1, 2, 3]")
        assertThrows(OpenMeteoException::class.java) { api(array).fetchForecast(OSLO) }
    }

    @Test
    fun `serves a repeated request from the cache and bypasses it when forced`() {
        val server = serve(SyntheticPayload.build().toString())
        val api = api(server)
        val cache = cache()

        api.fetchForecast(OSLO, cache = cache)
        api.fetchForecast(OSLO, cache = cache)
        assertEquals("second call must come from the cache", 1L, server.requestCount.toLong())

        api.fetchForecast(OSLO, cache = null)
        assertEquals("a forced refresh must hit the network", 2L, server.requestCount.toLong())

        // A different place is a different cache entry.
        api.fetchForecast(OSLO.copy(latitude = 51.8985, longitude = -8.4756), cache = cache)
        assertEquals(3L, server.requestCount.toLong())
    }

    @Test
    fun `parses geocoding results and skips unusable entries`() {
        val server = serve(SyntheticPayload.geocoding().toString())

        val places = api(server).geocode("Cork")

        assertEquals(2, places.size)
        assertEquals("Cork, Munster, Norway", places[0].label())
        assertEquals(51.8985, places[0].latitude, 1e-9)
        assertEquals("Cork", places[1].label())
        assertTrue(server.queries.single().contains("count=5"))
        assertTrue(server.queries.single().contains("format=json"))
    }

    @Test
    fun `keys the cache on the request parameters`() {
        val server = serve(SyntheticPayload.build().toString())
        val api = api(server)
        val cache = cache()

        api.fetchForecast(OSLO, model = "ecmwf_ifs", cache = cache)
        api.fetchForecast(OSLO, model = "best_match", cache = cache)

        assertEquals(2L, server.requestCount.toLong())
    }
}
