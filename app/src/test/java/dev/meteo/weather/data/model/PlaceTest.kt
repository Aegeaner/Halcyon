package dev.meteo.weather.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaceTest {

    @Test
    fun `joins name, admin1 and country`() {
        assertEquals("Cork, Munster, Norway", Place("Cork", 51.9, -8.5, "Norway", "Munster").label())
    }

    @Test
    fun `skips an admin1 that repeats the name`() {
        assertEquals("Oslo, Norway", Place("Oslo", 59.9139, 10.7522, "Norway", "Oslo").label())
    }

    @Test
    fun `skips blank and absent parts`() {
        assertEquals("Oslo", Place("Oslo", 59.9139, 10.7522).label())
        assertEquals("Oslo", Place("Oslo", 59.9139, 10.7522, "").label())
        assertEquals("Oslo", Place("Oslo", 59.9139, 10.7522, null, "").label())
        assertEquals("59.914, 10.752", Place("59.914, 10.752", 59.9139, 10.7522).label())
    }
}
