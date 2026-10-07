package dev.meteo.weather.domain

/** Outcome of reading the manual latitude and longitude fields. */
sealed interface CoordinateParse {
    /** A usable position. */
    data class Valid(val latitude: Double, val longitude: Double) : CoordinateParse

    /** Nothing typed yet, which is not an error. */
    data object Empty : CoordinateParse

    /** Exactly one of the two fields was filled in. */
    data object Incomplete : CoordinateParse

    /** At least one field is not a finite number. */
    data object NotANumber : CoordinateParse

    /** A number outside the valid range for its axis. */
    data object OutOfRange : CoordinateParse
}

/** Manual coordinate entry, mirroring the terminal version's `--lat/--lon` rules. */
object CoordinateInput {
    const val MAX_LATITUDE = 90.0
    const val MAX_LONGITUDE = 180.0

    fun parse(latitude: String, longitude: String): CoordinateParse {
        val lat = latitude.trim()
        val lon = longitude.trim()
        if (lat.isEmpty() && lon.isEmpty()) return CoordinateParse.Empty
        if (lat.isEmpty() || lon.isEmpty()) return CoordinateParse.Incomplete

        val parsedLat = lat.toDoubleOrNull() ?: return CoordinateParse.NotANumber
        val parsedLon = lon.toDoubleOrNull() ?: return CoordinateParse.NotANumber
        if (!parsedLat.isFinite() || !parsedLon.isFinite()) return CoordinateParse.NotANumber
        if (parsedLat !in -MAX_LATITUDE..MAX_LATITUDE) return CoordinateParse.OutOfRange
        if (parsedLon !in -MAX_LONGITUDE..MAX_LONGITUDE) return CoordinateParse.OutOfRange

        return CoordinateParse.Valid(parsedLat, parsedLon)
    }
}
