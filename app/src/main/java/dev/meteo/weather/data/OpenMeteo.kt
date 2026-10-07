package dev.meteo.weather.data

/** Open-Meteo endpoints and the exact request contract used by the terminal version. */

const val FORECAST_URL = "https://api.open-meteo.com/v1/forecast"
const val GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search"

/** Native-resolution IFS HRES (~9 km, reduced Gaussian grid O1280). */
const val DEFAULT_MODEL = "ecmwf_ifs"
const val MAX_FORECAST_DAYS = 16
const val USER_AGENT = "halcyon/0.1 (+https://open-meteo.com)"

val MODEL_LABELS: Map<String, String> = mapOf(
    "ecmwf_ifs" to "ECMWF IFS HRES (9 km, native)",
    "ecmwf_ifs025" to "ECMWF IFS open-data (0.25\u00b0)",
    "ecmwf_aifs025_single" to "ECMWF AIFS single (0.25\u00b0)",
    "best_match" to "Open-Meteo best match",
)

private val HOURLY_VARIABLES = listOf(
    "temperature_2m",
    "apparent_temperature",
    "relative_humidity_2m",
    "dew_point_2m",
    "precipitation",
    "rain",
    "showers",
    "snowfall",
    "precipitation_probability",
    "weather_code",
    "cloud_cover",
    "pressure_msl",
    "wind_speed_10m",
    "wind_direction_10m",
    "wind_gusts_10m",
    "visibility",
    "boundary_layer_height",
)

// `uv_index_max` is not produced by the 9 km IFS model; it is requested anyway and the column is
// shown only when at least one value is present (e.g. with the best_match model).
private val DAILY_VARIABLES = listOf(
    "weather_code",
    "temperature_2m_max",
    "temperature_2m_min",
    "apparent_temperature_max",
    "precipitation_sum",
    "rain_sum",
    "showers_sum",
    "snowfall_sum",
    "precipitation_hours",
    "precipitation_probability_max",
    "wind_speed_10m_max",
    "wind_gusts_10m_max",
    "wind_direction_10m_dominant",
    "sunrise",
    "sunset",
    "daylight_duration",
    "uv_index_max",
)

val HOURLY_PARAM: String = HOURLY_VARIABLES.joinToString(",")
val DAILY_PARAM: String = DAILY_VARIABLES.joinToString(",")

/** Raised when the API returns an error payload or unusable data. */
class OpenMeteoException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
