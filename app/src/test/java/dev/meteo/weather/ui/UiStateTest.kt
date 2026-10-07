package dev.meteo.weather.ui

import dev.meteo.weather.SyntheticPayload
import dev.meteo.weather.data.ForecastParser
import dev.meteo.weather.data.model.OSLO
import dev.meteo.weather.data.model.Place
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class UiStateTest {

    private val forecast = ForecastParser.parse(
        OSLO,
        SyntheticPayload.build(hours = 4, days = 2),
        fetchedAt = Instant.parse("2026-10-07T13:00:00Z"),
    )

    @Test
    fun `keeps a forecast that belongs to the selected place`() {
        val state = UiState(place = OSLO, forecast = forecast)

        assertEquals(forecast, state.visibleForecast)
    }

    @Test
    fun `hides a forecast of a different place so the wrong city is never shown`() {
        val cork = Place("Cork", 51.8985, -8.4756, country = "Norway")
        val state = UiState(place = cork, forecast = forecast)

        assertNull(state.visibleForecast)
    }

    @Test
    fun `hides a forecast whose coordinates moved but whose name stayed`() {
        val moved = OSLO.copy(latitude = 59.92)
        val state = UiState(place = moved, forecast = forecast)

        assertNull(state.visibleForecast)
    }

    @Test
    fun `starts on the native-resolution model, the fallback place and no device fix`() {
        val state = UiState()

        assertEquals("ecmwf_ifs", state.model)
        assertEquals(OSLO, state.place)
        assertFalse(state.fromDeviceLocation)
    }
}
