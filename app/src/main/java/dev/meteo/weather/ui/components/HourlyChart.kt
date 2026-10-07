package dev.meteo.weather.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
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

private val BAND_HEIGHTS = listOf(96.dp, 58.dp, 74.dp, 46.dp)
private val CAPTION_STRIP = 15.dp

/** The probability bars sit behind the amount bars, faint enough not to compete. */
private const val PROBABILITY_ALPHA = 0.16f
private val BAND_GAP = 16.dp
private val AXIS_HEIGHT = 18.dp

/**
 * Meteogram for one page of hours: temperature, precipitation, wind and humidity bands sharing one
 * x axis, so the series can be read against each other at a glance.
 */
@Composable
fun HourlyChart(hours: List<Hour>, modifier: Modifier = Modifier) {
    if (hours.isEmpty()) return

    val measurer = rememberTextMeasurer()
    val axis = MaterialTheme.colorScheme.onSurfaceVariant
    val grid = axis.copy(alpha = 0.20f)
    val labelStyle = TextStyle(fontSize = 9.sp, color = axis)
    val captionStyle = TextStyle(fontSize = 10.sp, color = axis)

    val captions = listOf(
        "${stringResource(R.string.chart_temperature)} \u00b0C",
        "${stringResource(R.string.chart_precipitation)} mm",
        "${stringResource(R.string.chart_wind)} km/h",
        "${stringResource(R.string.chart_humidity)} %",
    )
    val noPrecipitation = stringResource(R.string.chart_no_precipitation)
    val peak = stringResource(R.string.chart_peak)

    val temperature = HourlyChartData.temperature(hours)
    val precipitation = HourlyChartData.precipitation(hours)
    val probability = HourlyChartData.precipitationProbability(hours)
    val wind = HourlyChartData.windWithGusts(hours)
    val gustBand = wind.copy(points = HourlyChartData.gusts(hours, wind))
    val humidity = HourlyChartData.humidity(hours)

    val totalHeight = BAND_HEIGHTS.fold(AXIS_HEIGHT) { sum, band -> sum + band + BAND_GAP }

    Canvas(modifier = modifier.fillMaxWidth().height(totalHeight)) {
        val step = size.width / hours.size
        var bandTop = 0f
        val strip = CAPTION_STRIP.toPx()
        BAND_HEIGHTS.forEachIndexed { index, height ->
            val bandHeight = height.toPx()
            val plotTop = bandTop + strip
            val plotHeight = bandHeight - strip
            when (index) {
                0 -> drawTemperatureBand(bandTop, plotTop, plotHeight, temperature, step, measurer, captions[0], captionStyle, labelStyle, grid)
                1 -> drawPrecipitationBand(bandTop, plotTop, plotHeight, precipitation, probability, step, measurer, captions[1], captionStyle, labelStyle, grid, noPrecipitation)
                2 -> drawWindBand(bandTop, plotTop, plotHeight, wind, gustBand, step, measurer, captions[2], captionStyle, labelStyle, grid, peak)
                3 -> drawHumidityBand(bandTop, plotTop, plotHeight, humidity, step, measurer, captions[3], captionStyle, labelStyle, grid)
            }
            bandTop += bandHeight + BAND_GAP.toPx()
        }
        val top = bandTop - BAND_GAP.toPx()
        drawAxis(top, hours, step, measurer, labelStyle, grid)
    }
}

/** Horizontal line at the top of a band plus its caption, drawn for every band. */
private fun DrawScope.drawBandFrame(
    top: Float,
    caption: String,
    captionStyle: TextStyle,
    grid: Color,
    measurer: TextMeasurer,
) {
    drawLine(grid, Offset(0f, top), Offset(size.width, top), strokeWidth = 1f)
    drawText(textMeasurer = measurer, text = caption, style = captionStyle, topLeft = Offset(2f, top + 2f))
}

private fun DrawScope.valuePath(band: ChartBand, top: Float, height: Float, step: Float): Path {
    val path = Path()
    var started = false
    band.points.forEachIndexed { index, value ->
        val fraction = band.fraction(value)
        if (fraction == null) {
            started = false
            return@forEachIndexed
        }
        val x = step / 2f + index * step
        val y = top + height - fraction * height
        if (started) path.lineTo(x, y) else path.moveTo(x, y)
        started = true
    }
    return path
}

private fun DrawScope.label(measurer: TextMeasurer, text: String, style: TextStyle, x: Float, y: Float) {
    val measured = measurer.measure(text, style)
    drawText(
        textMeasurer = measurer,
        text = text,
        style = style,
        topLeft = Offset(
            x = x.coerceIn(0f, (size.width - measured.size.width).coerceAtLeast(0f)),
            y = y.coerceIn(0f, (size.height - measured.size.height).coerceAtLeast(0f)),
        ),
    )
}

private fun DrawScope.drawTemperatureBand(
    bandTop: Float,
    top: Float,
    height: Float,
    band: ChartBand,
    step: Float,
    measurer: TextMeasurer,
    caption: String,
    captionStyle: TextStyle,
    labelStyle: TextStyle,
    grid: Color,
) {
    drawBandFrame(bandTop, caption, captionStyle, grid, measurer)
    drawPath(valuePath(band, top, height, step), ChartColours.temperature, style = Stroke(width = 2.5f))

    val present = band.points.filterNotNull()
    if (present.isEmpty()) return
    for (value in listOf(present.max(), present.min()).distinct()) {
        val index = band.points.indexOfFirst { it == value }
        val fraction = band.fraction(value) ?: continue
        if (index < 0) continue
        val x = step / 2f + index * step
        val y = top + height - fraction * height
        drawCircle(ChartColours.temperature, radius = 3f, center = Offset(x, y))
        label(measurer, Fmt.num(value), labelStyle, x - 6f, (y - 12f).coerceAtLeast(top))
    }
}

private fun DrawScope.drawPrecipitationBand(
    bandTop: Float,
    top: Float,
    height: Float,
    band: ChartBand,
    probability: ChartBand,
    step: Float,
    measurer: TextMeasurer,
    caption: String,
    captionStyle: TextStyle,
    labelStyle: TextStyle,
    grid: Color,
    noPrecipitation: String,
) {
    drawBandFrame(bandTop, caption, captionStyle, grid, measurer)
    val barWidth = (step * 0.55f).coerceAtLeast(2f)
    band.points.forEachIndexed { index, value ->
        val x = 2f + index * step
        probability.fraction(probability.points.getOrNull(index))?.let { fraction ->
            drawRect(
                color = ChartColours.precipitation.copy(alpha = PROBABILITY_ALPHA),
                topLeft = Offset(x, top + height - fraction * height),
                size = Size(barWidth, fraction * height),
            )
        }
        band.fraction(value)?.let { fraction ->
            drawRect(
                color = ChartColours.precipitation,
                topLeft = Offset(x, top + height - fraction * height),
                size = Size(barWidth, fraction * height),
            )
        }
    }
    if (band.points.filterNotNull().sum() <= 0.0) {
        label(measurer, noPrecipitation, labelStyle, 2f, top + height - 14f)
    }
}

private fun DrawScope.drawWindBand(
    bandTop: Float,
    top: Float,
    height: Float,
    band: ChartBand,
    gustBand: ChartBand,
    step: Float,
    measurer: TextMeasurer,
    caption: String,
    captionStyle: TextStyle,
    labelStyle: TextStyle,
    grid: Color,
    peakLabel: String,
) {
    drawBandFrame(bandTop, caption, captionStyle, grid, measurer)
    val dashed = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f)))
    drawPath(valuePath(gustBand, top, height, step), ChartColours.wind.copy(alpha = 0.45f), style = dashed)
    drawPath(valuePath(band, top, height, step), ChartColours.wind, style = Stroke(width = 2.5f))

    val peak = band.points.filterNotNull().maxOrNull() ?: return
    val measured = measurer.measure(peakLabel.format(Fmt.int(peak)), labelStyle)
    label(measurer, peakLabel.format(Fmt.int(peak)), labelStyle, size.width - measured.size.width - 2f, top + height - 14f)
}

private fun DrawScope.drawHumidityBand(
    bandTop: Float,
    top: Float,
    height: Float,
    band: ChartBand,
    step: Float,
    measurer: TextMeasurer,
    caption: String,
    captionStyle: TextStyle,
    labelStyle: TextStyle,
    grid: Color,
) {
    drawBandFrame(bandTop, caption, captionStyle, grid, measurer)
    val inset = height * 0.30f
    val path = Path()
    var started = false
    band.points.forEachIndexed { index, value ->
        val fraction = band.fraction(value)
        if (fraction == null) {
            started = false
            return@forEachIndexed
        }
        val x = step / 2f + index * step
        val y = top + height - inset - fraction * (height - inset)
        if (started) path.lineTo(x, y) else path.moveTo(x, y)
        started = true
    }
    drawPath(path, ChartColours.humidity, style = Stroke(width = 1.5f))
    label(measurer, "0", labelStyle, size.width - 14f, top + height - 12f)
    label(measurer, "100", labelStyle, size.width - 20f, top + 2f)
}

/** Hour ticks every six hours, plus the weekday where a day begins. */
private fun DrawScope.drawAxis(
    top: Float,
    hours: List<Hour>,
    step: Float,
    measurer: TextMeasurer,
    labelStyle: TextStyle,
    grid: Color,
) {
    drawLine(grid, Offset(0f, top), Offset(size.width, top), strokeWidth = 1f)
    hours.forEachIndexed { index, hour ->
        val x = step / 2f + index * step
        if (hour.time.hour == 0) {
            drawLine(grid, Offset(x, 0f), Offset(x, top), strokeWidth = 1f)
        }
        val text = when {
            hour.time.hour == 0 -> hour.time.dayOfWeek.getDisplayName(JavaTextStyle.SHORT, Locale.ENGLISH)
            hour.time.hour % 6 == 0 -> "%02d".format(Locale.ENGLISH, hour.time.hour)
            else -> null
        } ?: return@forEachIndexed
        val measured = measurer.measure(text, labelStyle)
        label(measurer, text, labelStyle, x - measured.size.width / 2f, top + 3f)
    }
}

private enum class Swatch { LINE, DASHED, BAR, FAINT_BAR }

/**
 * Colour key for the bands. The precipitation band draws two series - amount in mm and the fainter
 * probability in percent - so they are named here rather than left to be guessed.
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
