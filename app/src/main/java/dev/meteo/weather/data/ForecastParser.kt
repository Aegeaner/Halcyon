package dev.meteo.weather.data

import dev.meteo.weather.data.model.Day
import dev.meteo.weather.data.model.Forecast
import dev.meteo.weather.data.model.Hour
import dev.meteo.weather.data.model.Place
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Turns a raw Open-Meteo payload into a [Forecast]. */
object ForecastParser {

    fun parse(
        place: Place,
        payload: JsonObject,
        model: String = DEFAULT_MODEL,
        fetchedAt: Instant = Instant.now(),
    ): Forecast = Forecast(
        place = place,
        latitude = number(payload["latitude"]) ?: place.latitude,
        longitude = number(payload["longitude"]) ?: place.longitude,
        elevation = number(payload["elevation"]),
        timezone = text(payload["timezone"]) ?: "UTC",
        utcOffsetSeconds = number(payload["utc_offset_seconds"])?.toInt() ?: 0,
        model = model,
        fetchedAt = fetchedAt,
        hours = parseHours(payload),
        days = parseDays(payload),
    )

    private fun parseHours(payload: JsonObject): List<Hour> {
        val block = payload["hourly"] as? JsonObject
        val times = block?.get("time").asTimestampList(raiseOnMalformed = true)
        val n = times.size
        val temperature = block.column("temperature_2m", n)
        val apparent = block.column("apparent_temperature", n)
        val humidity = block.column("relative_humidity_2m", n)
        val dewPoint = block.column("dew_point_2m", n)
        val precipitation = block.column("precipitation", n)
        val rain = block.column("rain", n)
        val showers = block.column("showers", n)
        val snowfall = block.column("snowfall", n)
        val probability = block.column("precipitation_probability", n)
        val weatherCode = block.column("weather_code", n)
        val cloudCover = block.column("cloud_cover", n)
        val pressure = block.column("pressure_msl", n)
        val windSpeed = block.column("wind_speed_10m", n)
        val windDirection = block.column("wind_direction_10m", n)
        val windGusts = block.column("wind_gusts_10m", n)
        val visibility = block.column("visibility", n)
        val boundaryLayer = block.column("boundary_layer_height", n)

        val hours = ArrayList<Hour>(n)
        for (i in 0 until n) {
            // An unparsable timestamp drops the row, exactly like the Python parser.
            val moment = dateTime(times[i]) ?: continue
            hours += Hour(
                time = moment,
                temperature = number(temperature[i]),
                apparentTemperature = number(apparent[i]),
                relativeHumidity = number(humidity[i]),
                dewPoint = number(dewPoint[i]),
                precipitation = number(precipitation[i]),
                rain = number(rain[i]),
                showers = number(showers[i]),
                snowfall = number(snowfall[i]),
                precipitationProbability = number(probability[i]),
                weatherCode = integer(weatherCode[i]),
                cloudCover = number(cloudCover[i]),
                pressureMsl = number(pressure[i]),
                windSpeed = number(windSpeed[i]),
                windDirection = number(windDirection[i]),
                windGusts = number(windGusts[i]),
                visibility = number(visibility[i]),
                boundaryLayerHeight = number(boundaryLayer[i]),
            )
        }
        return hours
    }

    private fun parseDays(payload: JsonObject): List<Day> {
        val block = payload["daily"] as? JsonObject
        val times = block?.get("time").asTimestampList(raiseOnMalformed = false)
        val n = times.size
        val weatherCode = block.column("weather_code", n)
        val temperatureMax = block.column("temperature_2m_max", n)
        val temperatureMin = block.column("temperature_2m_min", n)
        val apparentMax = block.column("apparent_temperature_max", n)
        val precipitationSum = block.column("precipitation_sum", n)
        val rainSum = block.column("rain_sum", n)
        val showersSum = block.column("showers_sum", n)
        val snowfallSum = block.column("snowfall_sum", n)
        val precipitationHours = block.column("precipitation_hours", n)
        val probabilityMax = block.column("precipitation_probability_max", n)
        val windSpeedMax = block.column("wind_speed_10m_max", n)
        val windGustsMax = block.column("wind_gusts_10m_max", n)
        val windDominant = block.column("wind_direction_10m_dominant", n)
        val sunrise = block.column("sunrise", n)
        val sunset = block.column("sunset", n)
        val daylight = block.column("daylight_duration", n)
        val uvIndex = block.column("uv_index_max", n)

        val days = ArrayList<Day>(n)
        for (i in 0 until n) {
            val day = dateTime(times[i])?.toLocalDate() ?: continue
            days += Day(
                date = day,
                weatherCode = integer(weatherCode[i]),
                temperatureMax = number(temperatureMax[i]),
                temperatureMin = number(temperatureMin[i]),
                apparentTemperatureMax = number(apparentMax[i]),
                precipitationSum = number(precipitationSum[i]),
                rainSum = number(rainSum[i]),
                showersSum = number(showersSum[i]),
                snowfallSum = number(snowfallSum[i]),
                precipitationHours = number(precipitationHours[i]),
                precipitationProbabilityMax = number(probabilityMax[i]),
                windSpeedMax = number(windSpeedMax[i]),
                windGustsMax = number(windGustsMax[i]),
                windDirectionDominant = number(windDominant[i]),
                sunrise = dateTime(sunrise[i]),
                sunset = dateTime(sunset[i]),
                daylightDuration = number(daylight[i]),
                uvIndexMax = number(uvIndex[i]),
            )
        }
        return days
    }
}

/** Reads a column as a list of exactly [length] entries, padding short columns with nulls. */
internal fun JsonObject?.column(name: String, length: Int): List<JsonElement?> {
    val array = this?.get(name) as? JsonArray ?: return List(length) { null }
    return List(length) { array.getOrNull(it) }
}

/**
 * `null`/missing becomes an empty list; a non-array value is malformed. Only the hourly block
 * treats that as fatal, mirroring the Python client.
 */
private fun JsonElement?.asTimestampList(raiseOnMalformed: Boolean): List<JsonElement> = when (this) {
    null, is JsonNull -> emptyList()
    is JsonArray -> this
    else -> if (raiseOnMalformed) {
        throw OpenMeteoException("Malformed 'hourly.time' in response")
    } else {
        emptyList()
    }
}

/** `null`, booleans and non-numeric strings all become `null`. */
internal fun number(element: JsonElement?): Double? =
    (element as? JsonPrimitive)?.content?.toDoubleOrNull()

internal fun integer(element: JsonElement?): Int? = number(element)?.toInt()

internal fun text(element: JsonElement?): String? =
    (element as? JsonPrimitive)?.takeUnless { it is JsonNull }?.content

/** Parses `2026-10-07T14:00` and `2026-10-07` into local wall-clock time. */
internal fun dateTime(element: JsonElement?): LocalDateTime? {
    val raw = (element as? JsonPrimitive)?.takeIf { it.isString }?.content ?: return null
    if (raw.isEmpty()) return null
    return runCatching { LocalDateTime.parse(raw) }.getOrNull()
        ?: runCatching { LocalDate.parse(raw).atStartOfDay() }.getOrNull()
}
