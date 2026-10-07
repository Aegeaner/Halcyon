package dev.meteo.weather.data.model

/** A named geographic location. */
data class Place(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String? = null,
    val admin1: String? = null,
) {
    /** `Name, Admin1, Country`, skipping an admin1 that merely repeats the name. */
    fun label(): String {
        val parts = mutableListOf(name)
        if (!admin1.isNullOrEmpty() && admin1 != name) parts += admin1
        if (!country.isNullOrEmpty()) parts += country
        return parts.joinToString(", ")
    }
}

/** Fallback when there is no device fix and no saved place; the device position comes first. */
val OSLO = Place("Oslo", 59.9139, 10.7522, country = "Norway")
