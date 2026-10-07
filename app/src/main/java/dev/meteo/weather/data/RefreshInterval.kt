package dev.meteo.weather.data

/**
 * How often the screen refreshes itself while it is visible.
 *
 * A timer refresh reuses the response cache, whose TTL is an hour, so the network is hit at most
 * once per hour whatever the interval; these choices trade freshness of the on-screen clock for
 * requests, and `OFF` stops the timer altogether. Manual refreshes always bypass the cache.
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
