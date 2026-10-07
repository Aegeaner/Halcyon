package dev.meteo.weather.domain

import dev.meteo.weather.SyntheticPayload
import dev.meteo.weather.data.ForecastParser
import dev.meteo.weather.data.model.OSLO
import java.time.Instant
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HourlyPagesTest {

    private fun forecast(hours: Int = SyntheticPayload.HOURS) = ForecastParser.parse(
        OSLO,
        SyntheticPayload.build(hours = hours, days = 2),
        fetchedAt = Instant.parse("2026-10-07T13:00:00Z"),
    )

    @Test
    fun `labels the three pages`() {
        assertEquals(listOf("1\u201324 h", "25\u201348 h", "49\u201372 h"), HourlyPages.labels)
    }

    @Test
    fun `first page starts at the anchor and holds 24 hours`() {
        val page = HourlyPages.page(forecast(), anchorIndex = 5, page = 0)

        assertEquals(24, page.size)
        assertEquals(LocalDateTime.of(2026, 10, 7, 5, 0), page.first().time)
        assertEquals(LocalDateTime.of(2026, 10, 8, 4, 0), page.last().time)
    }

    @Test
    fun `later pages continue every 24 hours`() {
        val fc = forecast()

        assertEquals(LocalDateTime.of(2026, 10, 8, 5, 0), HourlyPages.page(fc, 5, 1).first().time)
        assertEquals(LocalDateTime.of(2026, 10, 9, 5, 0), HourlyPages.page(fc, 5, 2).first().time)
        assertEquals(24, HourlyPages.page(fc, 5, 2).size)
    }

    @Test
    fun `a short response yields a partial or empty page`() {
        val fc = forecast(hours = 30)

        assertEquals(24, HourlyPages.page(fc, 5, 0).size)
        assertEquals(1, HourlyPages.page(fc, 5, 1).size)
        assertTrue(HourlyPages.page(fc, 5, 2).isEmpty())
    }

    @Test
    fun `pages beyond the end of the response are empty`() {
        val fc = forecast()

        assertEquals(4, HourlyPages.page(fc, 380, 0).size)
        assertTrue(HourlyPages.page(fc, 383, 1).isEmpty())
        assertTrue(HourlyPages.page(fc, 384, 0).isEmpty())
    }

    @Test
    fun `clamps the page number and rejects a negative anchor`() {
        val fc = forecast()

        assertEquals(
            HourlyPages.page(fc, 5, 2).first().time,
            HourlyPages.page(fc, 5, 7).first().time,
        )
        assertEquals(
            HourlyPages.page(fc, 5, 0).first().time,
            HourlyPages.page(fc, 5, -1).first().time,
        )
        assertTrue(HourlyPages.page(fc, -1, 0).isEmpty())
    }
}
