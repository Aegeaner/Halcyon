package dev.meteo.weather.data.model

import java.time.LocalDate
import java.time.LocalDateTime

/** One forecast day. */
data class Day(
    val date: LocalDate,
    val weatherCode: Int? = null,
    val temperatureMax: Double? = null,
    val temperatureMin: Double? = null,
    val apparentTemperatureMax: Double? = null,
    val precipitationSum: Double? = null,
    val rainSum: Double? = null,
    val showersSum: Double? = null,
    val snowfallSum: Double? = null,
    val precipitationHours: Double? = null,
    val precipitationProbabilityMax: Double? = null,
    val windSpeedMax: Double? = null,
    val windGustsMax: Double? = null,
    val windDirectionDominant: Double? = null,
    val sunrise: LocalDateTime? = null,
    val sunset: LocalDateTime? = null,
    val daylightDuration: Double? = null,
    val uvIndexMax: Double? = null,
)
