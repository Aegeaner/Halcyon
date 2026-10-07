package dev.meteo.weather.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.meteo.weather.R
import dev.meteo.weather.data.model.Hour
import dev.meteo.weather.domain.Fmt
import dev.meteo.weather.domain.HourlyPages
import dev.meteo.weather.domain.Si
import dev.meteo.weather.domain.Wmo

/** Section title, a link to the table behind the chart, and the three 24-hour pages. */
@Composable
fun HourlyHeader(
    selectedPage: Int,
    shownHours: Int,
    onPageSelected: (Int) -> Unit,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.hourly_range, shownHours),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onOpenDetails) { Text(stringResource(R.string.details)) }
        }
        PrimaryTabRow(selectedTabIndex = selectedPage) {
            HourlyPages.labels.forEachIndexed { index, label ->
                Tab(
                    selected = index == selectedPage,
                    onClick = { onPageSelected(index) },
                    text = { Text(text = label, maxLines = 1) },
                )
            }
        }
    }
}

/** One hourly row: the terminal table's Time/Weather/Temp/Feels columns, then Precip/PoP/Wind/Gust/Hum. */
@Composable
fun HourlyRow(hour: Hour, modifier: Modifier = Modifier) {
    val (symbol, description) = Wmo.describe(hour.weatherCode)
    val labelStyle = MaterialTheme.typography.labelLarge
    val detailStyle = MaterialTheme.typography.bodySmall

    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = Fmt.hourLabel(hour.time),
                style = labelStyle,
                modifier = Modifier.width(72.dp),
            )
            Text(
                text = "$symbol $description",
                style = labelStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${Fmt.num(hour.temperature)} ${Si.TEMPERATURE}",
                style = labelStyle,
                fontWeight = FontWeight.Bold,
            )
        }
        val details = mutableListOf<String>()
        details += stringResource(
            R.string.feels_like,
            "${Fmt.num(hour.apparentTemperature)} ${Si.TEMPERATURE}",
        )
        details += "${stringResource(R.string.precip)} ${Fmt.num(hour.precipitation)} ${Si.PRECIPITATION}"
        details += "${stringResource(R.string.pop)} ${Fmt.int(hour.precipitationProbability)} %"
        details += "${stringResource(R.string.wind)} ${Fmt.int(hour.windSpeed)} ${Si.WIND} ${Wmo.compass(hour.windDirection)}"
        details += "${stringResource(R.string.gusts)} ${Fmt.int(hour.windGusts)} ${Si.WIND}"
        details += "${stringResource(R.string.humidity)} ${Fmt.int(hour.relativeHumidity)} %"
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
