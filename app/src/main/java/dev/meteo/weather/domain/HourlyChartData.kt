package dev.meteo.weather.domain

import dev.meteo.weather.data.model.Hour

/** Pulls the series the hourly chart draws out of a page of hours. */
object HourlyChartData {
    fun temperature(hours: List<Hour>): ChartBand = ChartBand.ranged(hours.map { it.temperature })

    fun precipitation(hours: List<Hour>): ChartBand = ChartBand.zeroBased(hours.map { it.precipitation })

    fun precipitationProbability(hours: List<Hour>): ChartBand =
        ChartBand.capped(hours.map { it.precipitationProbability }, ceiling = 100.0)

    /** Gusts share the wind band, so both lines are comparable. */
    fun gusts(hours: List<Hour>, band: ChartBand): List<Double?> =
        hours.map { it.windGusts?.coerceAtLeast(band.min) }

    fun humidity(hours: List<Hour>): ChartBand = ChartBand.capped(hours.map { it.relativeHumidity }, ceiling = 100.0)

    /** Gusts can peak above the wind speed; widen the band so the dashed line stays inside it. */
    fun windWithGusts(hours: List<Hour>): ChartBand =
        ChartBand.zeroBased(hours.flatMap { listOf(it.windSpeed, it.windGusts) })
}
