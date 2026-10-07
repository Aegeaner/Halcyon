package dev.meteo.weather.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.meteo.weather.R
import dev.meteo.weather.data.model.Day
import dev.meteo.weather.domain.Fmt
import dev.meteo.weather.domain.Si
import dev.meteo.weather.domain.Wmo

@Composable
fun DailyHeader(dayCount: Int, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.daily_range, dayCount),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier.fillMaxWidth().padding(top = 8.dp),
    )
}

/**
 * One daily row: Day/Weather/Min/Max on the first line, then Precip/PoP/Wind/Gust and
 * Sunrise/Sunset, plus UV when the model produces it.
 */
@Composable
fun DailyRow(day: Day, showUv: Boolean, modifier: Modifier = Modifier) {
    val (symbol, description) = Wmo.describe(day.weatherCode)
    val labelStyle = MaterialTheme.typography.labelLarge
    val detailStyle = MaterialTheme.typography.bodySmall

    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = Fmt.dayLabel(day.date),
                style = labelStyle,
                modifier = Modifier.width(96.dp),
            )
            Text(
                text = "$symbol $description",
                style = labelStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${Fmt.num(day.temperatureMin)}° / " +
                    "${Fmt.num(day.temperatureMax)} ${Si.TEMPERATURE}",
                style = labelStyle,
                fontWeight = FontWeight.Bold,
            )
        }
        val details = mutableListOf<String>()
        details += "${stringResource(R.string.precip)} ${Fmt.num(day.precipitationSum)} ${Si.PRECIPITATION}"
        details += "${stringResource(R.string.pop)} ${Fmt.int(day.precipitationProbabilityMax)} %"
        details += "${stringResource(R.string.wind)} ${Fmt.int(day.windSpeedMax)} ${Si.WIND}"
        details += "${stringResource(R.string.gusts)} ${Fmt.int(day.windGustsMax)} ${Si.WIND}"
        details += "${stringResource(R.string.sunrise)} ${Fmt.clock(day.sunrise)}"
        details += "${stringResource(R.string.sunset)} ${Fmt.clock(day.sunset)}"
        if (showUv) details += "UV ${Fmt.num(day.uvIndexMax)}"
        Text(
            text = details.joinToString(" \u00b7 "),
            style = detailStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 4.dp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}
