package dev.meteo.weather.domain

import dev.meteo.weather.data.model.Hour

/** Pulls the series the hourly chart draws out of a page of hours. */
object HourlyChartData {
    fun temperature(hours: List<Hour>): ChartBand = ChartBand.ranged(hours.map { it.temperature })

    fun precipitation(hours: List<Hour>): ChartBand = ChartBand.zeroBased(hours.map { it.precipitation })

    fun precipitationProbability(hours: List<Hour>): ChartBand =
        ChartBand.capped(hours.map { it.precipitationProbability }, ceiling = 100.0)

    /** Gusts share the wind band, so both lines are comparable. */
    fun gusts(hours: List<Hour>): List<Double?> = hours.map { it.windGusts }

    fun humidity(hours: List<Hour>): ChartBand = ChartBand.capped(hours.map { it.relativeHumidity }, ceiling = 100.0)

    /**
     * The wind band draws the wind speed, one point per hour, in a band widened up to the gusts so
     * the dashed gust line stays inside it.
     */
    fun windWithGusts(hours: List<Hour>): ChartBand = ChartBand.zeroBased(
        values = hours.map { it.windSpeed },
        scale = hours.flatMap { listOf(it.windSpeed, it.windGusts) },
    )
}
