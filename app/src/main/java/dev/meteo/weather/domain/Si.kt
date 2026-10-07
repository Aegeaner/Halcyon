package dev.meteo.weather.domain

/**
 * The app renders SI units only — °C, km/h, mm and km — so there is no unit-system abstraction;
 * these are the symbols shown next to the values.
 */
object Si {
    const val TEMPERATURE = "\u00b0C"
    const val WIND = "km/h"
    const val PRECIPITATION = "mm"
    const val VISIBILITY = "km"

    /** Open-Meteo reports visibility in metres; the UI shows kilometres. */
    fun visibility(meters: Double?): String = Fmt.num(meters?.let { it / 1000.0 })
}
