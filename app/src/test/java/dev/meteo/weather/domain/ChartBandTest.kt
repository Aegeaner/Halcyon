package dev.meteo.weather.domain

import dev.meteo.weather.SyntheticPayload
import dev.meteo.weather.data.ForecastParser
import dev.meteo.weather.data.model.OSLO
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChartBandTest {

    @Test
    fun `maps a value range onto fractions of the band`() {
        val band = ChartBand.ranged(listOf(10.0, 20.0, 0.0))

        assertEquals(0f, band.fraction(0.0)!!, 1e-6f)
        assertEquals(1f, band.fraction(20.0)!!, 1e-6f)
        assertEquals(0.5f, band.fraction(10.0)!!, 1e-6f)
    }

    @Test
    fun `scales bars from zero even when the values are all positive`() {
        val band = ChartBand.zeroBased(listOf(2.0, 8.0))

        assertEquals(0.0, band.min, 1e-9)
        assertEquals(8.0, band.max, 1e-9)
        assertEquals(0.25f, band.fraction(2.0)!!, 1e-6f)
    }

    @Test
    fun `centres a flat band instead of dividing by zero`() {
        val band = ChartBand.ranged(listOf(12.0, 12.0, 12.0))

        assertEquals(0.5f, band.fraction(12.0)!!, 1e-6f)
    }

    @Test
    fun `keeps a dry series flat on the baseline`() {
        val band = ChartBand.zeroBased(listOf(0.0, 0.0))

        assertEquals(0f, band.fraction(0.0)!!, 1e-6f)
    }

    @Test
    fun `gives an all-empty band a unit span`() {
        val band = ChartBand.zeroBased(listOf(null, null, null))

        assertEquals(1.0, band.max, 1e-9)
        assertNull(band.fraction(null))
    }

    @Test
    fun `clamps values that fall outside the band`() {
        val band = ChartBand.capped(listOf(50.0), ceiling = 100.0)

        assertEquals(0f, band.fraction(-5.0)!!, 1e-6f)
        assertEquals(1f, band.fraction(140.0)!!, 1e-6f)
    }

    @Test
    fun `reads the series off a parsed forecast`() {
        val forecast = ForecastParser.parse(
            OSLO,
            SyntheticPayload.build(hours = 4, days = 1),
            fetchedAt = Instant.parse("2026-10-07T13:00:00Z"),
        )

        val temperature = HourlyChartData.temperature(forecast.hours)
        assertEquals(listOf(10.0, 10.5, 11.0, 11.5), temperature.points)
        assertEquals(10.0, temperature.min, 1e-9)

        val humidity = HourlyChartData.humidity(forecast.hours)
        assertEquals(0.0, humidity.min, 1e-9)
        assertEquals(100.0, humidity.max, 1e-9)
        assertEquals(0.8f, humidity.fraction(80.0)!!, 1e-6f)
    }
}
