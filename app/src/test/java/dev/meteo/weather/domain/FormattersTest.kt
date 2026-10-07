package dev.meteo.weather.domain

import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {

    @Test
    fun `renders missing values with an en dash`() {
        assertEquals("\u2013", Fmt.num(null))
        assertEquals("\u2013", Fmt.int(null))
        assertEquals("\u2013", Fmt.clock(null))
    }

    @Test
    fun `renders non-finite values as missing instead of crashing`() {
        assertEquals("\u2013", Fmt.num(Double.NaN))
        assertEquals("\u2013", Fmt.num(Double.POSITIVE_INFINITY))
        assertEquals("\u2013", Fmt.num(Double.NEGATIVE_INFINITY))
    }

    @Test
    fun `rounds half to even, like the terminal version`() {
        assertEquals("12.2", Fmt.num(12.25))
        assertEquals("50", Fmt.int(50.5))
        assertEquals("52", Fmt.int(51.5))
        assertEquals("0.1", Fmt.num(0.05))
        assertEquals("2.67", Fmt.num(2.675, 2))
        assertEquals("0.3", Fmt.num(0.1 + 0.2))
    }

    @Test
    fun `never uses exponent notation`() {
        assertEquals("0.00001", Fmt.num(0.00001, 5))
        assertEquals("1234567.0", Fmt.num(1234567.0))
    }

    @Test
    fun `formats timestamps in a fixed locale`() {
        val moment = LocalDateTime.of(2026, 10, 7, 14, 5)

        assertEquals("14:05", Fmt.clock(moment))
        assertEquals("Wed 14:05", Fmt.hourLabel(moment))
        assertEquals("Wed 07 Oct 14:05", Fmt.stamp(moment))
        assertEquals("Wed 07 Oct", Fmt.dayLabel(LocalDate.of(2026, 10, 7)))
    }

    @Test
    fun `converts visibility from metres to kilometres`() {
        assertEquals("20.0", Si.visibility(20000.0))
        assertEquals("0.5", Si.visibility(500.0))
        assertEquals("1.2", Si.visibility(1234.0))
        assertEquals("\u2013", Si.visibility(null))
    }
}
