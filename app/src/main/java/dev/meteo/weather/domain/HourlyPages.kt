package dev.meteo.weather.domain

import dev.meteo.weather.data.model.Forecast
import dev.meteo.weather.data.model.Hour

/**
 * The hourly forecast is shown as three pages of 24 hours, starting at the current hour.
 *
 * The anchor index is passed in rather than read from the clock so that all pages of one paint
 * agree, and so the paging is a pure function of the parsed forecast.
 */
object HourlyPages {
    const val HOURS_PER_PAGE = 24
    const val PAGE_COUNT = 3

    /** Index of the current hour, i.e. the anchor of the first page. */
    fun anchor(forecast: Forecast): Int = forecast.currentIndex()

    /** Hours on [page] (0-based); empty when the response does not reach that far. */
    fun page(forecast: Forecast, anchorIndex: Int, page: Int): List<Hour> {
        val bounded = page.coerceIn(0, PAGE_COUNT - 1)
        val start = anchorIndex + bounded * HOURS_PER_PAGE
        if (start < 0 || start >= forecast.hours.size) return emptyList()
        val end = minOf(start + HOURS_PER_PAGE, forecast.hours.size)
        return forecast.hours.subList(start, end)
    }

    /** `1–24 h`, `25–48 h`, `49–72 h`. */
    fun label(page: Int): String {
        val first = page * HOURS_PER_PAGE + 1
        return "$first\u2013${first + HOURS_PER_PAGE - 1} h"
    }

    /** Labels for every page, in order. */
    val labels: List<String> get() = (0 until PAGE_COUNT).map(::label)
}
