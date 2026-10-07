package dev.meteo.weather.data.model

import dev.meteo.weather.SyntheticPayload
import dev.meteo.weather.data.ForecastParser
import java.time.Instant
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForecastTest {

    private val place = Place("Oslo", 59.9139, 10.7522, country = "Norway")
    private val forecast = ForecastParser.parse(
        place,
        SyntheticPayload.build(),
        fetchedAt = Instant.parse("2026-10-07T13:00:00Z"),
    )

    @Test
    fun `turns the device clock into the wall clock of the forecast location`() {
        assertEquals(
            LocalDateTime.of(2026, 10, 7, 12, 0),
            forecast.localNow(Instant.parse("2026-10-07T10:00:00Z")),
        )
    }

    @Test
    fun `finds the last hour at or before the given moment`() {
        assertEquals(5, forecast.currentIndex(LocalDateTime.of(2026, 10, 7, 5, 30)))
        assertEquals(5, forecast.currentIndex(LocalDateTime.of(2026, 10, 7, 5, 0)))
        assertEquals(6, forecast.currentIndex(LocalDateTime.of(2026, 10, 7, 6, 0)))
    }

    @Test
    fun `clamps the index to the ends of the response`() {
        assertEquals(
            "before the first hour the first index is used",
            0,
            forecast.currentIndex(LocalDateTime.of(2026, 10, 6, 23, 0)),
        )
        assertEquals(383, forecast.currentIndex(LocalDateTime.of(2027, 1, 1, 0, 0)))
    }

    @Test
    fun `returns the current hour`() {
        val hour = forecast.currentHour(LocalDateTime.of(2026, 10, 7, 5, 30))

        assertEquals(LocalDateTime.of(2026, 10, 7, 5, 0), hour!!.time)
        assertEquals(12.5, hour.temperature!!, 1e-9)
    }

    @Test
    fun `hands out the first hour when the response is entirely in the future`() {
        assertEquals(0, forecast.currentIndex(LocalDateTime.of(2026, 10, 1, 0, 0)))
    }

    @Test
    fun `has no current hour in an empty response`() {
        val empty = ForecastParser.parse(place, SyntheticPayload.empty())

        assertTrue(empty.hours.isEmpty())
        assertNull(empty.currentHour())
        assertEquals(0, empty.currentIndex())
    }
}
