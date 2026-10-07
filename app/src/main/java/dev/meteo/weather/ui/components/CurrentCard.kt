package dev.meteo.weather.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.meteo.weather.R
import dev.meteo.weather.data.model.Forecast
import dev.meteo.weather.data.model.Hour
import dev.meteo.weather.domain.Fmt
import dev.meteo.weather.domain.Si
import dev.meteo.weather.domain.Wmo
import dev.meteo.weather.ui.Notice
import dev.meteo.weather.ui.UiState
import java.time.LocalDateTime

/**
 * Current conditions: the terminal version's `Now` panel, without what the screen already shows
 * elsewhere — the place and the model live in the top bar, and the requested position is not
 * repeated, so the coordinates below are the response's own (grid-snapped) point.
 */
@Composable
fun CurrentCard(state: UiState, forecast: Forecast, modifier: Modifier = Modifier) {
    val now = forecast.localNow()
    val hour = forecast.currentHour(now)

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(R.string.now),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )

            state.notice?.let { NoticeLine(it) }

            if (hour == null) {
                Text(
                    text = stringResource(R.string.no_hourly_data),
                    style = MaterialTheme.typography.bodyMedium,
                )
                return@Column
            }

            Headline(hour)
            Text(
                text = coordinates(forecast),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${forecast.timezone} · local ${Fmt.stamp(now)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Details(hour, forecast, now)
        }
    }
}

private fun coordinates(forecast: Forecast): String =
    "${Fmt.num(forecast.latitude, 3)}°, ${Fmt.num(forecast.longitude, 3)}° · " +
        "${Fmt.int(forecast.elevation, "?")} m asl"

@Composable
private fun Headline(hour: Hour) {
    val (symbol, description) = Wmo.describe(hour.weatherCode)
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = symbol, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "${Fmt.num(hour.temperature)} ${Si.TEMPERATURE}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = stringResource(
                R.string.feels_like,
                "${Fmt.num(hour.apparentTemperature)} ${Si.TEMPERATURE}",
            ) + " · $description",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun Details(hour: Hour, forecast: Forecast, now: LocalDateTime) {
    val today = forecast.days.firstOrNull { it.date == now.toLocalDate() }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FieldRow(
            Field(
                stringResource(R.string.wind),
                "${Fmt.int(hour.windSpeed)} ${Si.WIND} " +
                    "${Wmo.compass(hour.windDirection)} " +
                    "(${Fmt.int(hour.windDirection, "?")}°)",
            ),
            Field(stringResource(R.string.gusts), "${Fmt.int(hour.windGusts)} ${Si.WIND}"),
        )
        FieldRow(
            Field(stringResource(R.string.humidity), "${Fmt.int(hour.relativeHumidity, "?")} %"),
            Field(
                stringResource(R.string.dew_point),
                "${Fmt.num(hour.dewPoint)} ${Si.TEMPERATURE}",
            ),
        )
        FieldRow(
            Field(stringResource(R.string.cloud), "${Fmt.int(hour.cloudCover, "?")} %"),
            Field(stringResource(R.string.pressure), "${Fmt.num(hour.pressureMsl, 0)} hPa"),
        )
        FieldRow(
            Field(
                stringResource(R.string.precip),
                "${Fmt.num(hour.precipitation)} ${Si.PRECIPITATION} " +
                    "(${Fmt.int(hour.precipitationProbability, "?")} %)",
            ),
            Field(stringResource(R.string.visibility), "${Si.visibility(hour.visibility)} ${Si.VISIBILITY}"),
        )
        if (today != null) {
            FieldRow(
                Field(stringResource(R.string.sunrise), Fmt.clock(today.sunrise)),
                Field(stringResource(R.string.sunset), Fmt.clock(today.sunset)),
            )
        }
    }
}

@Composable
private fun NoticeLine(notice: Notice) {
    val text = when (notice) {
        Notice.LOCATION_PERMISSION_DENIED ->
            stringResource(R.string.notice_location_permission_denied)

        Notice.LOCATION_UNAVAILABLE -> stringResource(R.string.notice_location_unavailable)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.error,
    )
}

/** Placeholder card shown while the first response for a place is in flight or failed. */
@Composable
fun StatusCard(state: UiState, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = state.place.label(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            val error = state.error
            if (error != null) {
                Text(
                    text = "${stringResource(R.string.error_title)}: $error",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                Text(
                    text = stringResource(if (state.locating) R.string.locating else R.string.loading),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (state.loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}

private data class Field(val label: String, val value: String)

@Composable
private fun FieldRow(left: Field, right: Field) {
    Row(modifier = Modifier.fillMaxWidth()) {
        FieldCell(left, Modifier.weight(1f))
        FieldCell(right, Modifier.weight(1f))
    }
}

@Composable
private fun FieldCell(field: Field, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = field.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = field.value, style = MaterialTheme.typography.labelMedium)
    }
}
