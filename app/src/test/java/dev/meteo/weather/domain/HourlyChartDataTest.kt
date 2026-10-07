package dev.meteo.weather.domain

import dev.meteo.weather.SyntheticPayload
import dev.meteo.weather.data.ForecastParser
import dev.meteo.weather.data.model.Hour
import dev.meteo.weather.data.model.OSLO
import java.time.Instant
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class HourlyChartDataTest {

    @Test
    fun `draws one wind point per hour, scaled up to the gusts`() {
        val forecast = ForecastParser.parse(
            OSLO,
            SyntheticPayload.build(hours = 5, days = 1),
            fetchedAt = Instant.parse("2026-10-07T13:00:00Z"),
        )

        val wind = HourlyChartData.windWithGusts(forecast.hours)

        // The payload's wind speed is a flat 12 km/h and its gusts a flat 25 km/h: one point per
        // hour at the wind speed, in a band whose ceiling is the gust, not the interleaved pair.
        assertEquals(List(5) { 12.0 }, wind.points)
        assertEquals(25.0, wind.max, 1e-9)
        assertEquals(0.48f, wind.fraction(12.0)!!, 1e-6f)

        val gusts = wind.copy(points = HourlyChartData.gusts(forecast.hours))
        assertEquals(List(5) { 25.0 }, gusts.points)
    }

    @Test
    fun `keeps a missing gust as a gap`() {
        val start = LocalDateTime.of(2026, 10, 7, 21, 0)
        val hours = listOf(
            Hour(time = start, windSpeed = 4.0, windGusts = 11.0),
            Hour(time = start.plusHours(1), windSpeed = 6.0),
            Hour(time = start.plusHours(2), windSpeed = 8.0, windGusts = 15.0),
        )

        val wind = HourlyChartData.windWithGusts(hours)

        assertEquals(listOf(4.0, 6.0, 8.0), wind.points)
        assertEquals(15.0, wind.max, 1e-9)
        assertEquals(listOf(11.0, null, 15.0), HourlyChartData.gusts(hours))
    }
}
