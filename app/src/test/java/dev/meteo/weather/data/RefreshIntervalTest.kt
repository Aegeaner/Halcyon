package dev.meteo.weather.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RefreshIntervalTest {

    @Test
    fun `falls back to the default for a missing or unknown stored value`() {
        assertEquals(RefreshInterval.DEFAULT, RefreshInterval.fromSeconds(null))
        assertEquals(RefreshInterval.DEFAULT, RefreshInterval.fromSeconds(42))
        assertEquals(RefreshInterval.DEFAULT, RefreshInterval.fromSeconds(Int.MIN_VALUE))
    }

    @Test
    fun `round-trips every interval through its stored seconds`() {
        RefreshInterval.entries.forEach { interval ->
            assertEquals(interval, RefreshInterval.fromSeconds(interval.seconds))
        }
    }

    @Test
    fun `turns off cleanly and keeps the default at fifteen minutes`() {
        assertEquals(0L, RefreshInterval.OFF.millis)
        assertEquals(15 * 60 * 1000L, RefreshInterval.DEFAULT.millis)
    }
}
