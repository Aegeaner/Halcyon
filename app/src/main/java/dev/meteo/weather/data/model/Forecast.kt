package dev.meteo.weather.data.model

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * A parsed Open-Meteo forecast response.
 *
 * [hours] and [days] carry local wall-clock timestamps for [place]; [localNow] turns the device
 * clock into that same wall clock, so nothing depends on the device time zone.
 */
data class Forecast(
    val place: Place,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double?,
    val timezone: String,
    val utcOffsetSeconds: Int,
    val model: String,
    val fetchedAt: Instant,
    val hours: List<Hour>,
    val days: List<Day>,
) {
    /** Current wall-clock time at the forecast location. */
    fun localNow(nowUtc: Instant = Instant.now()): LocalDateTime =
        LocalDateTime.ofInstant(nowUtc.plusSeconds(utcOffsetSeconds.toLong()), ZoneOffset.UTC)

    /** Index of the last hour at or before [now] (0 when every hour is in the future). */
    fun currentIndex(now: LocalDateTime? = null): Int {
        val moment = now ?: localNow()
        var index = 0
        for (i in hours.indices) {
            if (hours[i].time <= moment) index = i else break
        }
        return index
    }

    fun currentHour(now: LocalDateTime? = null): Hour? =
        if (hours.isEmpty()) null else hours[currentIndex(now)]
}
