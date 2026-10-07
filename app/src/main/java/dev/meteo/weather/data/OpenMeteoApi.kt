package dev.meteo.weather.data

import dev.meteo.weather.data.model.Forecast
import dev.meteo.weather.data.model.Place
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Synchronous Open-Meteo client; call it from an IO dispatcher.
 *
 * Requests carry the same query as the terminal version: the native-resolution `ecmwf_ifs` model,
 * the full hourly and daily variable lists, `forecast_days` clamped to 1..16 and `timezone=auto`
 * so every timestamp comes back as local wall-clock time at the requested point.
 */
class OpenMeteoApi(
    private val client: OkHttpClient = defaultClient(),
    private val forecastUrl: String = FORECAST_URL,
    private val geocodingUrl: String = GEOCODING_URL,
) {

    fun fetchForecast(
        place: Place,
        days: Int = MAX_FORECAST_DAYS,
        model: String = DEFAULT_MODEL,
        cache: ResponseCache? = null,
        force: Boolean = false,
    ): Forecast {
        val params = linkedMapOf<String, Any>(
            "latitude" to round6(place.latitude),
            "longitude" to round6(place.longitude),
            "hourly" to HOURLY_PARAM,
            "daily" to DAILY_PARAM,
            "models" to model,
            "forecast_days" to days.coerceIn(1, MAX_FORECAST_DAYS),
            // The app runs at the requested point, so `auto` always returns local wall-clock times.
            "timezone" to TIMEZONE,
        )
        val payload = requestJson(forecastUrl, params, cache, useCache = !force)
        return ForecastParser.parse(place, payload, model = model)
    }

    fun geocode(name: String): List<Place> {
        val params = linkedMapOf<String, Any>(
            "name" to name,
            "count" to GEOCODING_COUNT,
            "language" to GEOCODING_LANGUAGE,
            "format" to "json",
        )
        val payload = requestJson(geocodingUrl, params, cache = null)
        val results = payload["results"] as? JsonArray ?: return emptyList()
        return results.mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            val latitude = number(item["latitude"]) ?: return@mapNotNull null
            val longitude = number(item["longitude"]) ?: return@mapNotNull null
            Place(
                name = text(item["name"]) ?: name,
                latitude = latitude,
                longitude = longitude,
                country = text(item["country"]),
                admin1 = text(item["admin1"]),
            )
        }
    }

    /**
     * [useCache] false reads past the cache - a forced refresh - but the response is still stored,
     * so the next ordinary load does not have to repeat the request.
     */
    private fun requestJson(
        url: String,
        params: Map<String, Any>,
        cache: ResponseCache?,
        useCache: Boolean = true,
    ): JsonObject {
        val key = if (cache != null) ResponseCache.makeKey(url, params) else ""
        if (cache != null && useCache) cache.get(key)?.let { return it }

        val request = Request.Builder()
            .url(buildUrl(url, params))
            .header("Accept", "application/json")
            .header("User-Agent", USER_AGENT)
            .build()

        val body = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw OpenMeteoException("HTTP ${response.code} from $url")
            response.body.string()
        }

        val data = try {
            Json.parseToJsonElement(body) as? JsonObject
        } catch (cause: Exception) {
            throw OpenMeteoException("Malformed JSON from Open-Meteo API", cause)
        } ?: throw OpenMeteoException("Unexpected response from Open-Meteo API")

        if (isTruthy(data["error"])) {
            throw OpenMeteoException(text(data["reason"]) ?: "Unknown Open-Meteo error")
        }

        if (cache != null) cache.put(key, data)
        return data
    }

    private fun buildUrl(url: String, params: Map<String, Any>): HttpUrl {
        val builder = url.toHttpUrl().newBuilder()
        params.forEach { (name, value) -> builder.addQueryParameter(name, queryValue(value)) }
        return builder.build()
    }

    companion object {
        /** `59.9139`, never `5.99139E1`. Open-Meteo reads at most six decimals of a coordinate. */
        private fun round6(value: Double): Double =
            BigDecimal(value).setScale(6, RoundingMode.HALF_EVEN).toDouble()

        /** `59.9139`, never `5.99139E1` and never a bare `0`. */
        private fun queryValue(value: Any): String {
            if (value !is Double) return value.toString()
            val rounded = BigDecimal(value).setScale(6, RoundingMode.HALF_EVEN).stripTrailingZeros()
            return if (rounded.scale() < 1) rounded.setScale(1).toPlainString() else rounded.toPlainString()
        }

        /** Mirrors Python's truthiness test on `error`: `false`/`null`/absent all mean success. */
        private fun isTruthy(element: JsonElement?): Boolean {
            if (element == null || element is JsonNull) return false
            val primitive = element as? JsonPrimitive ?: return true
            primitive.booleanOrNull?.let { return it }
            return primitive.contentOrNull?.isNotEmpty() ?: true
        }

        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        private const val TIMEZONE = "auto"
        private const val GEOCODING_COUNT = 5
        private const val GEOCODING_LANGUAGE = "en"
    }
}
