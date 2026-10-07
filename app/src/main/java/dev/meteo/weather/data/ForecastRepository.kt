package dev.meteo.weather.data

import dev.meteo.weather.data.model.Forecast
import dev.meteo.weather.data.model.Place
import java.io.File

/**
 * Forecast access used by the UI layer: the single place that decides whether the response cache
 * applies and how long a forecast horizon is requested.
 *
 * The hourly horizon is 72 h and the daily horizon is 16 days, so every request asks for the
 * maximum allowed `forecast_days` and the UI slices what it shows.
 *
 * Instances are cheap and hold an [OpenMeteoApi]; keep one per application and reuse it.
 */
class ForecastRepository(
    private val api: OpenMeteoApi = OpenMeteoApi(),
    private val cache: ResponseCache? = null,
) {
    fun forecast(
        place: Place,
        model: String = DEFAULT_MODEL,
        force: Boolean = false,
    ): Forecast = api.fetchForecast(
        place = place,
        days = MAX_FORECAST_DAYS,
        model = model,
        cache = cache,
        // A forced refresh reads past the cache but still refreshes it, so later loads - a place
        // change, a cold start - do not have to repeat the request.
        force = force,
    )

    fun geocode(name: String): List<Place> = api.geocode(name)
}

/** One hour of TTL, the `--cache-ttl` default of the terminal version. */
fun responseCache(appCacheDir: File): ResponseCache =
    ResponseCache(File(appCacheDir, "openmeteo"), ttlSeconds = ResponseCache.DEFAULT_TTL_SECONDS)
