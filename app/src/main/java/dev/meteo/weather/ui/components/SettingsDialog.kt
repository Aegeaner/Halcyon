package dev.meteo.weather.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.meteo.weather.R
import dev.meteo.weather.data.MODEL_LABELS
import dev.meteo.weather.data.model.Place
import dev.meteo.weather.domain.Fmt
import dev.meteo.weather.ui.CoordinateError
import dev.meteo.weather.ui.Notice
import dev.meteo.weather.ui.UiState

/** Unit system, model choice and place selection, mirroring the terminal CLI options. */
@Composable
fun SettingsDialog(
    state: UiState,
    onModelChange: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onPlaceSelected: (Place) -> Unit,
    onLatitudeChange: (String) -> Unit,
    onLongitudeChange: (String) -> Unit,
    onUseCoordinates: () -> Unit,
    onUseDeviceLocation: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.settings)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                SectionTitle(stringResource(R.string.place))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = onSearchQueryChange,
                        label = { Text(stringResource(R.string.search_city)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onSearch) { Text(stringResource(R.string.search_action)) }
                }
                if (state.searching) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                state.searchError?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (!state.searching && state.searchPerformed && state.searchResults.isEmpty() &&
                    state.searchError == null
                ) {
                    Text(
                        text = stringResource(R.string.no_results),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                state.searchResults.forEach { place ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlaceSelected(place) }
                            .padding(vertical = 8.dp),
                    ) {
                        Text(text = place.label(), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = Fmt.coordinates(place.latitude, place.longitude),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                SectionTitle(stringResource(R.string.coordinates))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = state.latitudeInput,
                        onValueChange = onLatitudeChange,
                        label = { Text(stringResource(R.string.latitude)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = state.longitudeInput,
                        onValueChange = onLongitudeChange,
                        label = { Text(stringResource(R.string.longitude)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { onUseCoordinates() }),
                        modifier = Modifier.weight(1f),
                    )
                }
                state.coordinateError?.let { error ->
                    Text(
                        text = stringResource(
                            when (error) {
                                CoordinateError.INCOMPLETE -> R.string.coordinates_incomplete
                                CoordinateError.INVALID -> R.string.coordinates_invalid
                            },
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                TextButton(onClick = onUseCoordinates) {
                    Text(stringResource(R.string.use_coordinates))
                }

                TextButton(onClick = onUseDeviceLocation) {
                    Text(stringResource(R.string.use_device_location))
                }
                if (state.notice == Notice.LOCATION_PERMISSION_DENIED) {
                    TextButton(onClick = onOpenAppSettings) {
                        Text(stringResource(R.string.open_app_settings))
                    }
                }

                SectionTitle(stringResource(R.string.model))
                MODEL_LABELS.forEach { (id, label) ->
                    ChoiceRow(
                        label = label,
                        selected = id == state.model,
                        onClick = { onModelChange(id) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}
