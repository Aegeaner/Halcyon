package dev.meteo.weather.data.model

import java.time.LocalDateTime

/** One forecast hour, in local wall-clock time at the forecast location. */
data class Hour(
    val time: LocalDateTime,
    val temperature: Double? = null,
    val apparentTemperature: Double? = null,
    val relativeHumidity: Double? = null,
    val dewPoint: Double? = null,
    val precipitation: Double? = null,
    val rain: Double? = null,
    val showers: Double? = null,
    val snowfall: Double? = null,
    val precipitationProbability: Double? = null,
    val weatherCode: Int? = null,
    val cloudCover: Double? = null,
    val pressureMsl: Double? = null,
    val windSpeed: Double? = null,
    val windDirection: Double? = null,
    val windGusts: Double? = null,
    val visibility: Double? = null,
    val boundaryLayerHeight: Double? = null,
)
