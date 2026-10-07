package dev.meteo.weather.data

/**
 * How often the visible screen asks Open-Meteo for a fresh forecast.
 *
 * Open-Meteo is open source, needs no API key for non-commercial use, and its free tier allows on
 * the order of 10,000 requests a day; even the shortest option here is 96 a day. The forecast
 * itself is what limits the useful cadence: ECMWF reruns the model a few times a day, so polling
 * faster mostly re-reads the same numbers, and the response cache keeps the other loads - a cold
 * start, a place or model change - from repeating requests. `OFF` stops the timer; the toolbar's
 * refresh always goes to the network.
 */
enum class RefreshInterval(val seconds: Int) {
    OFF(0),
    FIFTEEN_MINUTES(15 * 60),
    THIRTY_MINUTES(30 * 60),
    HOURLY(60 * 60),
    THREE_HOURLY(3 * 60 * 60),
    ;

    val millis: Long get() = seconds * 1000L

    companion object {
        val DEFAULT = FIFTEEN_MINUTES

        /** Falls back to [DEFAULT] for a missing or unknown stored value. */
        fun fromSeconds(seconds: Int?): RefreshInterval =
            entries.firstOrNull { it.seconds == seconds } ?: DEFAULT
    }
}
