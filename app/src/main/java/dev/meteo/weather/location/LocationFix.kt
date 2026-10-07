package dev.meteo.weather.location

import dev.meteo.weather.domain.Fmt

/**
 * The position the forecast is requested for.
 *
 * Only the coordinates are carried: quality and freshness are decided inside [LocationProvider],
 * which prefers an active fix over a cached one and compares candidates by their monotonic
 * timestamps, never by which provider produced them.
 */
data class LocationFix(
    val latitude: Double,
    val longitude: Double,
) {
    /** `59.914, 10.752`, the label used for a position that has no name. */
    fun label(): String = Fmt.coordinates(latitude, longitude)
}
