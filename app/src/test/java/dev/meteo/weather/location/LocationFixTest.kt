package dev.meteo.weather.location

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class LocationFixTest {

    @Test
    fun `labels the position with three decimals regardless of the device locale`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            val fix = LocationFix(59.9139, 10.7522)

            assertEquals("59.914, 10.752", fix.label())
        } finally {
            Locale.setDefault(original)
        }
    }
}
