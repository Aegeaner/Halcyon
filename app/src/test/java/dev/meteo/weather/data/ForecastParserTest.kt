package dev.meteo.weather.data

import dev.meteo.weather.SyntheticPayload
import dev.meteo.weather.data.model.Place
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ForecastParserTest {

    private val place = Place("Oslo", 59.9139, 10.7522, country = "Norway")
    private val fetchedAt = Instant.parse("2026-10-07T13:00:00Z")

    private fun parse(payload: kotlinx.serialization.json.JsonObject) =
        ForecastParser.parse(place, payload, fetchedAt = fetchedAt)

    @Test
    fun `parses every hour and day of a full response`() {
        val forecast = parse(SyntheticPayload.build())

        assertEquals(384, forecast.hours.size)
        assertEquals(16, forecast.days.size)
        assertEquals(59.9139, forecast.latitude, 1e-9)
        assertEquals(10.7522, forecast.longitude, 1e-9)
        assertEquals(11.0, forecast.elevation!!, 1e-9)
        assertEquals("Europe/Oslo", forecast.timezone)
        assertEquals(7200, forecast.utcOffsetSeconds)
        assertEquals("ecmwf_ifs", forecast.model)
        assertEquals(fetchedAt, forecast.fetchedAt)
        assertEquals(place, forecast.place)
    }

    @Test
    fun `maps hourly columns onto the expected fields`() {
        val forecast = parse(SyntheticPayload.build())
        val first = forecast.hours.first()
        val sixth = forecast.hours[5]

        assertEquals(SyntheticPayload.START, first.time)
        assertEquals(10.0, first.temperature!!, 1e-9)
        assertEquals(9.0, first.apparentTemperature!!, 1e-9)
        assertEquals(80.0, first.relativeHumidity!!, 1e-9)
        assertEquals(8.0, first.dewPoint!!, 1e-9)
        assertEquals(0.0, first.precipitation!!, 1e-9)
        assertEquals(0.0, first.precipitationProbability!!, 1e-9)
        assertEquals(0, first.weatherCode)
        assertEquals(50.0, first.cloudCover!!, 1e-9)
        assertEquals(1013.0, first.pressureMsl!!, 1e-9)
        assertEquals(12.0, first.windSpeed!!, 1e-9)
        assertEquals(225.0, first.windDirection!!, 1e-9)
        assertEquals(25.0, first.windGusts!!, 1e-9)
        assertEquals(20000.0, first.visibility!!, 1e-9)
        assertEquals(500.0, first.boundaryLayerHeight!!, 1e-9)

        assertEquals(12.5, sixth.temperature!!, 1e-9)
        assertEquals(0.4, sixth.precipitation!!, 1e-9) // 0.2 * (5 % 3)
        assertEquals(35.0, sixth.precipitationProbability!!, 1e-9) // (5 * 3) % 100
        assertEquals(0, sixth.weatherCode) // 5 % 5
    }

    @Test
    fun `maps daily columns onto the expected fields`() {
        val forecast = parse(SyntheticPayload.build())
        val first = forecast.days.first()

        assertEquals(LocalDate.of(2026, 10, 7), first.date)
        assertEquals(61, first.weatherCode)
        assertEquals(7.0, first.temperatureMin!!, 1e-9)
        assertEquals(15.0, first.temperatureMax!!, 1e-9)
        assertEquals(14.0, first.apparentTemperatureMax!!, 1e-9)
        assertEquals(3.2, first.precipitationSum!!, 1e-9)
        assertEquals(3.0, first.rainSum!!, 1e-9)
        assertEquals(0.2, first.showersSum!!, 1e-9)
        assertEquals(0.0, first.snowfallSum!!, 1e-9)
        assertEquals(5.0, first.precipitationHours!!, 1e-9)
        assertEquals(80.0, first.precipitationProbabilityMax!!, 1e-9)
        assertEquals(30.0, first.windSpeedMax!!, 1e-9)
        assertEquals(55.0, first.windGustsMax!!, 1e-9)
        assertEquals(240.0, first.windDirectionDominant!!, 1e-9)
        assertEquals(LocalDateTime.of(2026, 10, 7, 7, 35), first.sunrise)
        assertEquals(LocalDateTime.of(2026, 10, 7, 18, 45), first.sunset)
        assertEquals(40000.0, first.daylightDuration!!, 1e-9)
        assertNull("the 9 km IFS model has no UV index", first.uvIndexMax)
    }

    @Test
    fun `tolerates missing, short, non-numeric and malformed columns`() {
        val forecast = parse(SyntheticPayload.tolerant(hours = 6))

        // The last row has an unparsable timestamp and is dropped.
        assertEquals(5, forecast.hours.size)
        assertEquals(SyntheticPayload.START, forecast.hours.first().time)
        assertEquals(SyntheticPayload.START.plusHours(4), forecast.hours.last().time)

        // A missing column is null everywhere.
        assertTrue(forecast.hours.all { it.visibility == null })

        // A short column is padded with nulls beyond its length.
        assertEquals(10.0, forecast.hours[0].temperature!!, 1e-9)
        assertEquals(11.0, forecast.hours[2].temperature!!, 1e-9)
        assertNull(forecast.hours[3].temperature)

        // Non-numeric and boolean values become null; the rest of the column still parses.
        assertEquals(0, forecast.hours[0].weatherCode)
        assertNull(forecast.hours[1].weatherCode)
        assertNull(forecast.hours[2].weatherCode)
        assertEquals(61, forecast.hours[3].weatherCode)

        // Numeric strings parse, an explicit null stays null.
        assertEquals(50.0, forecast.hours[0].cloudCover!!, 1e-9)
        assertNull(forecast.hours[0].relativeHumidity)
        assertEquals(80.0, forecast.hours[1].relativeHumidity!!, 1e-9)
    }

    @Test
    fun `reads coordinates and timezone from the payload but falls back to the request`() {
        val payload = buildJsonObject {
            put("latitude", JsonPrimitive("59.914"))
            put("timezone", "UTC")
        }
        val forecast = parse(payload)

        assertEquals(59.914, forecast.latitude, 1e-9)
        assertEquals(10.7522, forecast.longitude, 1e-9) // absent in the payload
        assertEquals("UTC", forecast.timezone)
        assertEquals(0, forecast.utcOffsetSeconds)
        assertNull(forecast.elevation)
        assertTrue(forecast.hours.isEmpty())
        assertTrue(forecast.days.isEmpty())
    }

    @Test
    fun `rejects a malformed hourly time column`() {
        val thrown = assertThrows(OpenMeteoException::class.java) {
            parse(SyntheticPayload.malformedHourlyTimes())
        }
        assertTrue(thrown.message!!.contains("hourly.time"))
    }

    @Test
    fun `returns an empty forecast for an empty payload`() {
        val forecast = parse(SyntheticPayload.empty())

        assertTrue(forecast.hours.isEmpty())
        assertTrue(forecast.days.isEmpty())
        assertEquals("UTC", forecast.timezone)
        assertEquals(place.latitude, forecast.latitude, 1e-9)
    }
}
