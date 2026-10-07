package dev.meteo.weather.domain

/**
 * One band of the hourly chart: the range it is drawn against and the values placed on it.
 *
 * Kept free of Compose so the scaling is unit tested; the renderer only turns fractions into
 * pixels. Null entries stay null and are drawn as gaps.
 */
data class ChartBand(
    val min: Double,
    val max: Double,
    val points: List<Double?>,
) {
    /** Where [value] sits in the band: 0 at [min], 1 at [max]; a flat band sits in the middle. */
    fun fraction(value: Double?): Float? {
        if (value == null) return null
        val span = max - min
        if (span <= 0.0) return 0.5f
        return ((value - min) / span).coerceIn(0.0, 1.0).toFloat()
    }


    /** Index of the largest value, or null when the series is empty. Ties keep the earliest. */
    fun peakIndex(): Int? = points.indices.filter { points[it] != null }.maxByOrNull { points[it]!! }

    /** Index of the smallest value, or null when the series is empty. Ties keep the earliest. */
    fun troughIndex(): Int? = points.indices.filter { points[it] != null }.minByOrNull { points[it]!! }

    companion object {
        /**
         * Scaled to the values themselves, for a line that fills its band. A constant series gets a
         * unit window around it so the line lands in the middle instead of on the baseline.
         */
        fun ranged(values: List<Double?>): ChartBand {
            val present = values.filterNotNull()
            val lowest = present.minOrNull() ?: return ChartBand(0.0, 1.0, values)
            val highest = present.maxOrNull() ?: lowest
            return if (highest - lowest <= 0.0) {
                ChartBand(lowest - 0.5, lowest + 0.5, values)
            } else {
                ChartBand(lowest, highest, values)
            }
        }

        /**
         * Scaled from zero, for bars: a dry, still series keeps the baseline at zero so the bars
         * stay flat on it rather than floating mid-band.
         *
         * [scale] holds the values that set the top of the band when they are not the ones drawn -
         * gusts sharing the wind band. It defaults to the drawn series.
         */
        fun zeroBased(values: List<Double?>, scale: List<Double?> = values): ChartBand {
            val highest = scale.filterNotNull().maxOrNull() ?: 0.0
            return ChartBand(0.0, if (highest > 0.0) highest else 1.0, values)
        }

        /** Scaled from zero up to [ceiling], for percentages that always span 0..100. */
        fun capped(values: List<Double?>, ceiling: Double): ChartBand =
            ChartBand(0.0, ceiling, values)
    }
}
