package dev.meteo.weather.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CoordinateInputTest {

    @Test
    fun `parses a decimal pair`() {
        assertEquals(
            CoordinateParse.Valid(59.9139, 10.7522),
            CoordinateInput.parse("59.9139", "10.7522"),
        )
    }

    @Test
    fun `accepts the boundaries of both axes`() {
        assertEquals(CoordinateParse.Valid(90.0, 180.0), CoordinateInput.parse("90", "180"))
        assertEquals(CoordinateParse.Valid(-90.0, -180.0), CoordinateInput.parse("-90", "-180"))
        assertEquals(CoordinateParse.Valid(0.0, 0.0), CoordinateInput.parse("0", "0"))
    }

    @Test
    fun `ignores surrounding whitespace and accepts an explicit sign`() {
        assertEquals(
            CoordinateParse.Valid(59.9139, 10.7522),
            CoordinateInput.parse("  59.9139 ", " +10.7522"),
        )
    }

    @Test
    fun `reports nothing typed as empty rather than as an error`() {
        assertEquals(CoordinateParse.Empty, CoordinateInput.parse("", ""))
        assertEquals(CoordinateParse.Empty, CoordinateInput.parse("   ", " "))
    }

    @Test
    fun `needs both fields`() {
        assertEquals(CoordinateParse.Incomplete, CoordinateInput.parse("", "10.7522"))
        assertEquals(CoordinateParse.Incomplete, CoordinateInput.parse("59.9139", ""))
        assertEquals(CoordinateParse.Incomplete, CoordinateInput.parse("59.9139", "   "))
    }

    @Test
    fun `rejects values that are not finite numbers`() {
        assertEquals(CoordinateParse.NotANumber, CoordinateInput.parse("north", "10"))
        assertEquals(CoordinateParse.NotANumber, CoordinateInput.parse("59.9139", "east"))
        assertEquals(CoordinateParse.NotANumber, CoordinateInput.parse("NaN", "10"))
        assertEquals(CoordinateParse.NotANumber, CoordinateInput.parse("Infinity", "10"))
        assertEquals(CoordinateParse.NotANumber, CoordinateInput.parse("59,9139", "10"))
    }

    @Test
    fun `rejects values outside the range of their axis`() {
        assertEquals(CoordinateParse.OutOfRange, CoordinateInput.parse("90.1", "10"))
        assertEquals(CoordinateParse.OutOfRange, CoordinateInput.parse("-90.1", "10"))
        assertEquals(CoordinateParse.OutOfRange, CoordinateInput.parse("59.9139", "180.1"))
        assertEquals(CoordinateParse.OutOfRange, CoordinateInput.parse("59.9139", "-180.1"))
    }
}
