package dev.meteo.weather.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Value formatting shared by every renderer. */
object Fmt {
    const val MISSING = "\u2013"

    // Locale.ENGLISH rather than Locale.ROOT: on Android the CLDR root locale renders month names
    // as M01-M12, so a root-formatted "EEE dd MMM" reads "Wed 07 M10" on a device.
    private val CLOCK: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
    private val CLOCK_SECONDS: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ENGLISH)
    private val HOUR_LABEL: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE HH:mm", Locale.ENGLISH)
    private val DAY_LABEL: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE dd MMM", Locale.ENGLISH)
    private val STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE dd MMM HH:mm", Locale.ENGLISH)

    /**
     * Fixed-point rendering of a double.
     *
     * Uses `BigDecimal` with HALF_EVEN on the *exact* binary value, which is what CPython's
     * `f"{value:.1f}"` does; `String.format("%.1f", …)` would round half up and disagree on
     * values such as 12.25 (12.2 here, 12.3 with `String.format`).
     */
    fun num(value: Double?, digits: Int = 1, missing: String = MISSING): String {
        // BigDecimal rejects NaN and the infinities, so they render as missing instead.
        if (value == null || !value.isFinite()) return missing
        // `toPlainString` keeps large and tiny values free of exponent notation.
        return BigDecimal(value).setScale(digits, RoundingMode.HALF_EVEN).toPlainString()
    }

    fun int(value: Double?, missing: String = MISSING): String = num(value, 0, missing)

    fun clock(value: LocalDateTime?): String = value?.format(CLOCK) ?: MISSING

    /** `HH:mm:ss` of a device-local instant, e.g. when the response was fetched. */
    fun clockSeconds(value: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
        value.atZone(zone).format(CLOCK_SECONDS)

    /** `Tue 14:00`, the hourly table's time column. */
    fun hourLabel(value: LocalDateTime): String = value.format(HOUR_LABEL)

    /** `Tue 07 Oct`, the daily table's day column. */
    fun dayLabel(value: LocalDate): String = value.format(DAY_LABEL)

    /** `Tue 07 Oct 14:00`, the local timestamp in the current-conditions panel. */
    fun stamp(value: LocalDateTime): String = value.format(STAMP)

    /** `59.914, 10.752` — the label for a position with no name, e.g. a fix or a manual entry. */
    fun coordinates(latitude: Double, longitude: Double): String =
        "%.3f, %.3f".format(Locale.ROOT, latitude, longitude)
}
