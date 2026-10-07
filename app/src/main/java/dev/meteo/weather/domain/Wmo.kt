package dev.meteo.weather.domain

/** WMO 4677 weather codes used by Open-Meteo, plus wind-direction helpers. */
object Wmo {
    // Symbol first, short description second.
    private val CODES: Map<Int, Pair<String, String>> = mapOf(
        0 to ("\u2600" to "Clear sky"),
        1 to ("\uD83C\uDF24" to "Mainly clear"),
        2 to ("\u26C5" to "Partly cloudy"),
        3 to ("\u2601" to "Overcast"),
        45 to ("\uD83C\uDF2B" to "Fog"),
        48 to ("\uD83C\uDF2B" to "Rime fog"),
        51 to ("\uD83C\uDF26" to "Light drizzle"),
        53 to ("\uD83C\uDF26" to "Drizzle"),
        55 to ("\uD83C\uDF26" to "Dense drizzle"),
        56 to ("\uD83C\uDF27" to "Freezing drizzle"),
        57 to ("\uD83C\uDF27" to "Freezing drizzle"),
        61 to ("\uD83C\uDF27" to "Light rain"),
        63 to ("\uD83C\uDF27" to "Rain"),
        65 to ("\uD83C\uDF27" to "Heavy rain"),
        66 to ("\uD83C\uDF27" to "Freezing rain"),
        67 to ("\uD83C\uDF27" to "Freezing rain"),
        71 to ("\uD83C\uDF28" to "Light snow"),
        73 to ("\uD83C\uDF28" to "Snow"),
        75 to ("\uD83C\uDF28" to "Heavy snow"),
        77 to ("\u2744" to "Snow grains"),
        80 to ("\uD83C\uDF26" to "Light showers"),
        81 to ("\uD83C\uDF26" to "Showers"),
        82 to ("\u26C8" to "Violent showers"),
        85 to ("\uD83C\uDF28" to "Snow showers"),
        86 to ("\uD83C\uDF28" to "Heavy snow showers"),
        95 to ("\u26C8" to "Thunderstorm"),
        96 to ("\u26C8" to "Thunderstorm, hail"),
        99 to ("\u26C8" to "Thunderstorm, heavy hail"),
    )

    private val COMPASS = listOf(
        "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
        "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW",
    )

    /** Returns (symbol, description) for a WMO code, tolerating unknown and null codes. */
    fun describe(code: Int?): Pair<String, String> {
        if (code == null) return Fmt.MISSING to "Unknown"
        return CODES[code] ?: ("?" to "Code $code")
    }

    /** 16-point compass direction for a bearing in degrees. */
    fun compass(degrees: Double?): String {
        if (degrees == null) return Fmt.MISSING
        val normalized = ((degrees % 360.0) + 360.0) % 360.0
        return COMPASS[((normalized / 22.5 + 0.5).toInt()) % 16]
    }
}
