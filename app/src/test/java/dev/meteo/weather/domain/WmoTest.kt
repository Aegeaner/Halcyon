package dev.meteo.weather.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class WmoTest {

    @Test
    fun `describes a known code`() {
        assertEquals("\u2600" to "Clear sky", Wmo.describe(0))
        assertEquals("\u26C5" to "Partly cloudy", Wmo.describe(2))
        assertEquals("\uD83C\uDF27" to "Light rain", Wmo.describe(61))
        assertEquals("\u2744" to "Snow grains", Wmo.describe(77))
        assertEquals("\u26C8" to "Thunderstorm, heavy hail", Wmo.describe(99))
    }

    @Test
    fun `tolerates unknown and absent codes`() {
        assertEquals("?" to "Code 123", Wmo.describe(123))
        assertEquals(Fmt.MISSING to "Unknown", Wmo.describe(null))
    }

    @Test
    fun `maps bearings onto the 16-point compass`() {
        assertEquals("N", Wmo.compass(0.0))
        assertEquals("N", Wmo.compass(11.2))
        assertEquals("NNE", Wmo.compass(11.25))
        assertEquals("NNE", Wmo.compass(22.5))
        assertEquals("NE", Wmo.compass(45.0))
        assertEquals("E", Wmo.compass(90.0))
        assertEquals("S", Wmo.compass(180.0))
        assertEquals("SW", Wmo.compass(225.0))
        assertEquals("W", Wmo.compass(270.0))
        assertEquals("NNW", Wmo.compass(337.5))
        assertEquals("N", Wmo.compass(348.75))
        assertEquals("N", Wmo.compass(359.9))
    }

    @Test
    fun `wraps out-of-range bearings`() {
        assertEquals("N", Wmo.compass(360.0))
        assertEquals("W", Wmo.compass(-90.0))
        assertEquals("NW", Wmo.compass(-45.0))
        assertEquals("N", Wmo.compass(720.0))
    }

    @Test
    fun `has no direction for a missing bearing`() {
        assertEquals(Fmt.MISSING, Wmo.compass(null))
    }
}
