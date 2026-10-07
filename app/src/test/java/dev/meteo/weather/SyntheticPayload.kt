package dev.meteo.weather

import java.time.LocalDateTime
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/**
 * Deterministic Open-Meteo payload shaped like a real `ecmwf_ifs` response: 384 hourly entries
 * (16 days) and 16 daily entries, with no UV index — the 9 km IFS model does not produce one.
 *
 * Fixed timestamps keep the assertions independent of the clock.
 */
object SyntheticPayload {
    val START: LocalDateTime = LocalDateTime.of(2026, 10, 7, 0, 0)

    const val HOURS = 384
    const val DAYS = 16

    fun build(hours: Int = HOURS, days: Int = DAYS): JsonObject = buildJsonObject {
        put("latitude", 59.9139)
        put("longitude", 10.7522)
        put("generationtime_ms", 0.5)
        put("utc_offset_seconds", 7200)
        put("timezone", "Europe/Oslo")
        put("timezone_abbreviation", "CET")
        put("elevation", 11.0)
        put("hourly", buildJsonObject {
            put("time", hoursStringArray(hours))
            put("temperature_2m", doubles(hours) { 10.0 + 0.5 * it })
            put("apparent_temperature", doubles(hours) { 9.0 + 0.5 * it })
            put("relative_humidity_2m", doubles(hours) { 80.0 })
            put("dew_point_2m", doubles(hours) { 8.0 })
            put("precipitation", doubles(hours) { 0.2 * (it % 3) })
            put("rain", doubles(hours) { 0.2 * (it % 3) })
            put("showers", doubles(hours) { 0.0 })
            put("snowfall", doubles(hours) { 0.0 })
            put("precipitation_probability", doubles(hours) { ((it * 7) % 100).toDouble() })
            put("weather_code", buildJsonArray { repeat(hours) { add(WEATHER_CODES[it % WEATHER_CODES.size]) } })
            put("cloud_cover", doubles(hours) { 50.0 })
            put("pressure_msl", doubles(hours) { 1013.0 })
            put("wind_speed_10m", doubles(hours) { 12.0 })
            put("wind_direction_10m", doubles(hours) { 225.0 })
            put("wind_gusts_10m", doubles(hours) { 25.0 })
            put("visibility", doubles(hours) { 20000.0 })
            put("boundary_layer_height", doubles(hours) { 500.0 })
        })
        put("daily", buildJsonObject {
            put("time", buildJsonArray {
                repeat(days) { add(START.toLocalDate().plusDays(it.toLong()).toString()) }
            })
            put("weather_code", buildJsonArray { repeat(days) { add(61) } })
            put("temperature_2m_max", doubles(days) { 15.0 })
            put("temperature_2m_min", doubles(days) { 7.0 })
            put("apparent_temperature_max", doubles(days) { 14.0 })
            put("precipitation_sum", doubles(days) { 3.2 })
            put("rain_sum", doubles(days) { 3.0 })
            put("showers_sum", doubles(days) { 0.2 })
            put("snowfall_sum", doubles(days) { 0.0 })
            put("precipitation_hours", doubles(days) { 5.0 })
            put("precipitation_probability_max", doubles(days) { 80.0 })
            put("wind_speed_10m_max", doubles(days) { 30.0 })
            put("wind_gusts_10m_max", doubles(days) { 55.0 })
            put("wind_direction_10m_dominant", doubles(days) { 240.0 })
            put("sunrise", buildJsonArray {
                repeat(days) { add("${START.toLocalDate().plusDays(it.toLong())}T07:35") }
            })
            put("sunset", buildJsonArray {
                repeat(days) { add("${START.toLocalDate().plusDays(it.toLong())}T18:45") }
            })
            put("daylight_duration", doubles(days) { 40000.0 })
            put("uv_index_max", buildJsonArray { repeat(days) { add(JsonNull) } })
        })
    }

    /**
     * The shapes the parser has to survive: a missing column, a short column, a non-numeric
     * string, a boolean, a numeric string, a null, and an unparsable timestamp in the last row.
     */
    fun tolerant(hours: Int = 6): JsonObject {
        val base = build(hours = hours, days = 2)
        val hourly = base.getValue("hourly").jsonObject.toMutableMap()
        hourly.remove("visibility")
        hourly["temperature_2m"] = doubles(3) { 10.0 + 0.5 * it }
        hourly["weather_code"] = buildJsonArray {
            add(0)
            add(JsonPrimitive("oops"))
            add(JsonPrimitive(true))
            add(61)
        }
        hourly["cloud_cover"] = buildJsonArray {
            repeat(hours) { add(JsonPrimitive("50.0")) }
        }
        hourly["relative_humidity_2m"] = buildJsonArray {
            add(JsonNull)
            repeat(hours - 1) { add(JsonPrimitive(80.0)) }
        }
        hourly["time"] = buildJsonArray {
            val times = base.getValue("hourly").jsonObject.getValue("time").jsonArray
            times.forEachIndexed { index, time ->
                add(if (index == times.size - 1) JsonPrimitive("not-a-time") else time)
            }
        }
        return JsonObject(base.toMutableMap().apply { put("hourly", JsonObject(hourly)) })
    }

    /** `{"hourly": {"time": "2026-10-07T00:00"}}` — the one malformed shape that is fatal. */
    fun malformedHourlyTimes(): JsonObject = buildJsonObject {
        put("hourly", buildJsonObject { put("time", JsonPrimitive(START.toString())) })
    }

    fun empty(): JsonObject = buildJsonObject { }

    fun geocoding(): JsonObject = buildJsonObject {
        put("results", buildJsonArray {
            add(buildJsonObject {
                put("name", "Cork")
                put("latitude", 51.8985)
                put("longitude", -8.4756)
                put("country", "Norway")
                put("admin1", "Munster")
            })
            add(buildJsonObject {
                put("name", "Cork")
                put("longitude", -8.4756)
            })
            add(buildJsonObject {
                put("name", "Cork")
                put("latitude", 51.9)
                put("longitude", -8.48)
                put("country", JsonNull)
                put("admin1", JsonNull)
            })
        })
    }

    fun error(reason: String = "Cannot initialize Forecast"): JsonObject = buildJsonObject {
        put("error", true)
        put("reason", reason)
    }

    private val WEATHER_CODES = listOf(0, 1, 2, 3, 61)

    private fun hoursStringArray(count: Int): JsonArray = buildJsonArray {
        repeat(count) { add(START.plusHours(it.toLong()).toString()) }
    }

    private fun doubles(count: Int, value: (Int) -> Double): JsonArray = buildJsonArray {
        repeat(count) { add(value(it)) }
    }
}
