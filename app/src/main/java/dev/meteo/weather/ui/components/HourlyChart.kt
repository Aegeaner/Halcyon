package dev.meteo.weather.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.meteo.weather.R
import dev.meteo.weather.data.model.Hour
import dev.meteo.weather.domain.ChartBand
import dev.meteo.weather.domain.Fmt
import dev.meteo.weather.domain.HourlyChartData
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

/** Mid-tone data colours, readable on both the light and the dark Material surfaces. */
private object ChartColours {
    val temperature = Color(0xFFF2C14E)
    val precipitation = Color(0xFF6FB3D9)
    val wind = Color(0xFF7FB8B2)
    val humidity = Color(0xFF9AA7B0)
}

private val BAND_HEIGHTS = listOf(96.dp, 64.dp, 76.dp, 54.dp)
private val BAND_GAP = 18.dp
private val AXIS_HEIGHT = 18.dp
private val CAPTION_STRIP = 15.dp

/** The probability bars sit behind the amount bars, faint enough not to compete. */
private const val PROBABILITY_ALPHA = 0.16f

/**
 * Meteogram for one page of hours: temperature, precipitation, wind and humidity bands sharing one
 * x axis.
 *
 * Every series labels its own extremes with real values - the highest and lowest reading, the
 * tallest bar, the strongest wind and gust, the dampest and driest hour - so the bands can be read
 * without the table, which stays one tap away for exact figures.
 */
@Composable
fun HourlyChart(hours: List<Hour>, modifier: Modifier = Modifier) {
    if (hours.isEmpty()) return

    val measurer = rememberTextMeasurer()
    val axis = MaterialTheme.colorScheme.onSurfaceVariant
    val grid = axis.copy(alpha = 0.20f)
    val labelStyle = TextStyle(fontSize = 9.sp, color = axis)
    val captionStyle = TextStyle(fontSize = 10.sp, color = axis)

    val captionTemperature = "${stringResource(R.string.chart_temperature)} \u00b0C"
    val captionPrecipitation = "${stringResource(R.string.chart_precipitation)} mm / " +
        "${stringResource(R.string.chart_probability)} %"
    val captionWind = "${stringResource(R.string.chart_wind)} km/h"
    val captionHumidity = "${stringResource(R.string.chart_humidity)} %"
    val noPrecipitation = stringResource(R.string.chart_no_precipitation)
    val percent = stringResource(R.string.chart_percent)

    val temperature = HourlyChartData.temperature(hours)
    val precipitation = HourlyChartData.precipitation(hours)
    val probability = HourlyChartData.precipitationProbability(hours)
    val wind = HourlyChartData.windWithGusts(hours)
    val gusts = wind.copy(points = HourlyChartData.gusts(hours))
    val humidity = HourlyChartData.humidity(hours)

    val totalHeight = BAND_HEIGHTS.fold(AXIS_HEIGHT) { sum, band -> sum + band + BAND_GAP }

    Canvas(modifier = modifier.fillMaxWidth().height(totalHeight)) {
        val step = size.width / hours.size
        val strip = CAPTION_STRIP.toPx()
        var bandTop = 0f

        BAND_HEIGHTS.forEachIndexed { index, height ->
            val bandHeight = height.toPx()
            val frame = Frame(bandTop, bandTop + strip, bandHeight - strip, step, measurer, labelStyle)
            when (index) {
                0 -> drawTemperatureBand(frame, captionTemperature, captionStyle, temperature)
                1 -> drawPrecipitationBand(
                    frame,
                    captionPrecipitation,
                    captionStyle,
                    precipitation,
                    probability,
                    noPrecipitation,
                    percent,
                )

                2 -> drawWindBand(frame, captionWind, captionStyle, wind, gusts)
                3 -> drawHumidityBand(frame, captionHumidity, captionStyle, humidity, percent)
            }
            bandTop += bandHeight + BAND_GAP.toPx()
        }
        val axisTop = bandTop - BAND_GAP.toPx()
        drawLine(grid, Offset(0f, axisTop), Offset(size.width, axisTop), strokeWidth = 1f)
        drawHourAxis(axisTop, hours, step, measurer, labelStyle, grid)
    }
}

/** Where one band lives on the canvas, plus what every band needs to draw into it. */
private data class Frame(
    val bandTop: Float,
    val plotTop: Float,
    val plotHeight: Float,
    val step: Float,
    val measurer: TextMeasurer,
    val labelStyle: TextStyle,
) {
    fun x(index: Int): Float = step / 2f + index * step
    fun y(fraction: Float): Float = plotTop + plotHeight - fraction * plotHeight
    val baseline: Float get() = plotTop + plotHeight
}

private fun DrawScope.drawBandFrame(
    frame: Frame,
    caption: String,
    captionStyle: TextStyle,
    grid: Color,
) {
    drawLine(grid, Offset(0f, frame.plotTop), Offset(size.width, frame.plotTop), strokeWidth = 1f)
    drawText(
        textMeasurer = frame.measurer,
        text = caption,
        style = captionStyle,
        topLeft = Offset(2f, frame.bandTop + 2f),
    )
}

/** The zero line of a bar or speed band, so heights can be read against it. */
private fun DrawScope.drawBaseline(frame: Frame, grid: Color) {
    drawLine(grid, Offset(0f, frame.baseline), Offset(size.width, frame.baseline), strokeWidth = 1.5f)
}

private fun bandPath(band: ChartBand, frame: Frame): Path {
    val path = Path()
    var started = false
    band.points.forEachIndexed { index, value ->
        val fraction = band.fraction(value)
        if (fraction == null) {
            started = false
            return@forEachIndexed
        }
        val x = frame.x(index)
        val y = frame.y(fraction)
        if (started) path.lineTo(x, y) else path.moveTo(x, y)
        started = true
    }
    return path
}

/** A dot on the series plus its value, kept inside the band. */
private fun DrawScope.labelPoint(
    frame: Frame,
    index: Int,
    fraction: Float,
    text: String,
    colour: Color = frame.labelStyle.color,
) {
    val x = frame.x(index)
    val y = frame.y(fraction)
    drawCircle(colour, radius = 3f, center = Offset(x, y))
    val measured = frame.measurer.measure(text, frame.labelStyle)
    val labelX = (x - measured.size.width / 2f)
        .coerceIn(0f, (size.width - measured.size.width).coerceAtLeast(0f))
    val above = y - measured.size.height - 5f
    val labelY = if (above >= frame.plotTop) above else y + 6f
    drawText(
        textMeasurer = frame.measurer,
        text = text,
        style = frame.labelStyle,
        topLeft = Offset(labelX, labelY),
    )
}

/** Labels the extremes of a series, if it has any. */
private fun DrawScope.labelExtremes(frame: Frame, band: ChartBand, colour: Color) {
    for (index in listOfNotNull(band.peakIndex(), band.troughIndex())) {
        val value = band.points.getOrNull(index) ?: continue
        val fraction = band.fraction(value) ?: continue
        labelPoint(frame, index, fraction, Fmt.num(value), colour)
    }
}

/** A fixed scale pinned to the band's top-right, for a series whose ceiling is not a data point. */
private fun DrawScope.labelCeiling(frame: Frame, text: String, colour: Color) {
    val style = frame.labelStyle.copy(color = colour)
    val measured = frame.measurer.measure(text, style)
    drawText(
        textMeasurer = frame.measurer,
        text = text,
        style = style,
        topLeft = Offset(
            x = size.width - measured.size.width - 3f,
            y = frame.plotTop - measured.size.height / 2f,
        ),
    )
}

private fun DrawScope.drawTemperatureBand(
    frame: Frame,
    caption: String,
    captionStyle: TextStyle,
    band: ChartBand,
) {
    drawBandFrame(frame, caption, captionStyle, ChartColours.temperature.copy(alpha = 0.25f))
    drawPath(bandPath(band, frame), ChartColours.temperature, style = Stroke(width = 2.5f))
    labelExtremes(frame, band, ChartColours.temperature)
}

private fun DrawScope.drawPrecipitationBand(
    frame: Frame,
    caption: String,
    captionStyle: TextStyle,
    amounts: ChartBand,
    probability: ChartBand,
    noPrecipitation: String,
    percent: String,
) {
    val grid = ChartColours.precipitation.copy(alpha = 0.25f)
    drawBandFrame(frame, caption, captionStyle, grid)
    drawBaseline(frame, grid)

    val barWidth = (frame.step * 0.55f).coerceAtLeast(2f)
    amounts.points.forEachIndexed { index, value ->
        val x = 2f + index * frame.step
        probability.fraction(probability.points.getOrNull(index))?.let { fraction ->
            val height = frame.baseline - frame.y(fraction)
            drawRect(
                color = ChartColours.precipitation.copy(alpha = PROBABILITY_ALPHA),
                topLeft = Offset(x, frame.baseline - height),
                size = Size(barWidth, height),
            )
        }
        amounts.fraction(value)?.let { fraction ->
            val height = frame.baseline - frame.y(fraction)
            drawRect(
                color = ChartColours.precipitation,
                topLeft = Offset(x, frame.baseline - height),
                size = Size(barWidth, height),
            )
        }
    }

    if (amounts.points.filterNotNull().sum() <= 0.0) {
        drawText(
            textMeasurer = frame.measurer,
            text = noPrecipitation,
            style = frame.labelStyle,
            topLeft = Offset(2f, frame.plotTop + 2f),
        )
    } else {
        amounts.peakIndex()?.let { index ->
            val value = amounts.points.getOrNull(index) ?: return@let
            val fraction = amounts.fraction(value) ?: return@let
            labelPoint(frame, index, fraction, Fmt.num(value), ChartColours.precipitation)
        }
    }

    // Probability is always scaled 0..100, so its ceiling is a scale label, and the series carries
    // its own maximum next to it.
    labelCeiling(frame, percent.format(Fmt.int(100.0)), ChartColours.precipitation.copy(alpha = 0.55f))
    probability.peakIndex()?.let { index ->
        val value = probability.points.getOrNull(index) ?: return@let
        if (value >= 100.0) return@let
        val fraction = probability.fraction(value) ?: return@let
        labelPoint(frame, index, fraction, percent.format(Fmt.int(value)), ChartColours.precipitation.copy(alpha = 0.7f))
    }
}

private fun DrawScope.drawWindBand(
    frame: Frame,
    caption: String,
    captionStyle: TextStyle,
    wind: ChartBand,
    gusts: ChartBand,
) {
    val grid = ChartColours.wind.copy(alpha = 0.25f)
    drawBandFrame(frame, caption, captionStyle, grid)
    drawBaseline(frame, grid)

    drawPath(
        path = bandPath(gusts, frame),
        color = ChartColours.wind.copy(alpha = 0.45f),
        style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))),
    )
    drawPath(bandPath(wind, frame), ChartColours.wind, style = Stroke(width = 2.5f))

    // The dashed series is unreadable without its own figure, so both maxima are labelled.
    gusts.peakIndex()?.let { index ->
        val value = gusts.points.getOrNull(index) ?: return@let
        val fraction = gusts.fraction(value) ?: return@let
        labelPoint(frame, index, fraction, Fmt.num(value), ChartColours.wind.copy(alpha = 0.6f))
    }
    wind.peakIndex()?.let { index ->
        val value = wind.points.getOrNull(index) ?: return@let
        val fraction = wind.fraction(value) ?: return@let
        labelPoint(frame, index, fraction, Fmt.num(value), ChartColours.wind)
    }
}

private fun DrawScope.drawHumidityBand(
    frame: Frame,
    caption: String,
    captionStyle: TextStyle,
    band: ChartBand,
    percent: String,
) {
    val grid = ChartColours.humidity.copy(alpha = 0.3f)
    drawBandFrame(frame, caption, captionStyle, grid)
    drawPath(bandPath(band, frame), ChartColours.humidity, style = Stroke(width = 2f))

    for (index in listOfNotNull(band.peakIndex(), band.troughIndex())) {
        val value = band.points.getOrNull(index) ?: continue
        val fraction = band.fraction(value) ?: continue
        labelPoint(frame, index, fraction, percent.format(Fmt.int(value)), ChartColours.humidity)
    }
}

/** Hour ticks every six hours, plus the weekday where a day begins. */
private fun DrawScope.drawHourAxis(
    top: Float,
    hours: List<Hour>,
    step: Float,
    measurer: TextMeasurer,
    labelStyle: TextStyle,
    grid: Color,
) {
    hours.forEachIndexed { index, hour ->
        val x = step / 2f + index * step
        if (hour.time.hour == 0) drawLine(grid, Offset(x, 0f), Offset(x, top), strokeWidth = 1f)
        val text = when {
            hour.time.hour == 0 -> hour.time.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.ENGLISH)
            hour.time.hour % 3 == 0 -> "%02d".format(Locale.ENGLISH, hour.time.hour)
            else -> null
        } ?: return@forEachIndexed
        val measured = measurer.measure(text, labelStyle)
        drawText(
            textMeasurer = measurer,
            text = text,
            style = labelStyle,
            topLeft = Offset((x - measured.size.width / 2f).coerceIn(0f, size.width - measured.size.width), top + 3f),
        )
    }
}

private enum class Swatch { LINE, DASHED, BAR, FAINT_BAR }

/**
 * Colour key for the bands: the precipitation band draws two series - amount in millimetres and the
 * fainter probability in percent - so they are named here rather than left to be guessed.
 */
@Composable
fun HourlyChartLegend(modifier: Modifier = Modifier) {
    val labelStyle = TextStyle(fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LegendItem(Swatch.LINE, ChartColours.temperature, stringResource(R.string.chart_temperature), labelStyle)
            LegendItem(Swatch.BAR, ChartColours.precipitation, stringResource(R.string.chart_precipitation), labelStyle)
            LegendItem(Swatch.FAINT_BAR, ChartColours.precipitation, stringResource(R.string.chart_probability), labelStyle)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LegendItem(Swatch.LINE, ChartColours.wind, stringResource(R.string.chart_wind), labelStyle)
            LegendItem(Swatch.DASHED, ChartColours.wind, stringResource(R.string.chart_gusts), labelStyle)
            LegendItem(Swatch.LINE, ChartColours.humidity, stringResource(R.string.chart_humidity), labelStyle)
        }
    }
}

@Composable
private fun LegendItem(swatch: Swatch, colour: Color, text: String, labelStyle: TextStyle) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(modifier = Modifier.width(18.dp).height(8.dp)) {
            when (swatch) {
                Swatch.LINE -> drawLine(
                    colour,
                    Offset(0f, size.height / 2f),
                    Offset(size.width, size.height / 2f),
                    strokeWidth = 2.5f,
                )

                Swatch.DASHED -> drawLine(
                    colour.copy(alpha = 0.45f),
                    Offset(0f, size.height / 2f),
                    Offset(size.width, size.height / 2f),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)),
                )

                Swatch.BAR -> drawRect(colour, size = Size(size.width * 0.7f, size.height))
                Swatch.FAINT_BAR -> drawRect(
                    colour.copy(alpha = PROBABILITY_ALPHA),
                    size = Size(size.width * 0.7f, size.height),
                )
            }
        }
        Text(text = text, style = labelStyle, maxLines = 1)
    }
}
