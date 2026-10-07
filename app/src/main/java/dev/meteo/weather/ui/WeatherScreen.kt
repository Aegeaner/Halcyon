package dev.meteo.weather.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.meteo.weather.R
import dev.meteo.weather.data.MODEL_LABELS
import dev.meteo.weather.data.model.Forecast
import dev.meteo.weather.domain.Fmt
import dev.meteo.weather.domain.HourlyPages
import dev.meteo.weather.location.REQUIRED_LOCATION_PERMISSIONS
import dev.meteo.weather.location.hasLocationPermission
import dev.meteo.weather.ui.components.CurrentCard
import dev.meteo.weather.ui.components.DailyHeader
import dev.meteo.weather.ui.components.DailyRow
import dev.meteo.weather.ui.components.HourlyHeader
import dev.meteo.weather.ui.components.HourlyRow
import dev.meteo.weather.ui.components.SettingsDialog
import dev.meteo.weather.ui.components.StatusCard
import kotlinx.coroutines.delay

/** The terminal version's `--refresh` default. */
private const val REFRESH_INTERVAL_MILLIS = 900_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    modifier: Modifier = Modifier,
    viewModel: WeatherViewModel = viewModel(
        factory = WeatherViewModel.factory(LocalContext.current.applicationContext),
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var hourlyPage by rememberSaveable { mutableIntStateOf(0) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants.values.any { it }) viewModel.locate() else viewModel.onLocationPermissionDenied()
    }
    val requestDeviceLocation = {
        if (context.hasLocationPermission()) {
            viewModel.locate()
        } else {
            permissionLauncher.launch(REQUIRED_LOCATION_PERMISSIONS)
        }
    }

    // The device position is the primary source: ask for it on the first run, and again when the
    // saved place came from a fix. A place the user searched for is left alone.
    LaunchedEffect(Unit) {
        if (viewModel.shouldUseDeviceLocation) requestDeviceLocation()
    }

    // Refresh on a timer while the screen is visible; the cache is reused, like the terminal timer.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                delay(REFRESH_INTERVAL_MILLIS)
                viewModel.refreshIfStale()
            }
        }
    }

    LaunchedEffect(state.openSettings) {
        if (state.openSettings) {
            settingsOpen = true
            viewModel.onSettingsShown()
        }
    }

    LaunchedEffect(snackbarHostState) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    // Derived once per composition, in a composable context: `LazyListScope` is not one, so this
    // cannot live inside the list body, and one anchor keeps the three pages consistent per paint.
    val forecast = state.visibleForecast
    val hours = forecast?.let { HourlyPages.page(it, HourlyPages.anchor(it), hourlyPage) }.orEmpty()
    val showUv = forecast?.days?.any { it.uvIndexMax != null } == true

    Scaffold(
        modifier = modifier,
        topBar = {
            WeatherTopBar(
                state = state,
                forecast = forecast,
                onRefresh = viewModel::refresh,
                onSettings = { settingsOpen = true },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (state.loading) {
                item(key = "loading") {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
            if (forecast == null) {
                item(key = "status") { StatusCard(state) }
                return@LazyColumn
            }

            item(key = "current") { CurrentCard(state, forecast) }

            item(key = "hourly-header") {
                HourlyHeader(
                    selectedPage = hourlyPage,
                    shownHours = hours.size,
                    onPageSelected = { hourlyPage = it },
                )
            }
            items(items = hours, key = { "hour-${it.time}" }) { hour ->
                HourlyRow(hour = hour)
            }

            item(key = "daily-header") { DailyHeader(dayCount = forecast.days.size) }
            items(items = forecast.days, key = { "day-${it.date}" }) { day ->
                DailyRow(day = day, showUv = showUv)
            }

            item(key = "attribution") {
                Text(
                    text = stringResource(R.string.attribution),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (settingsOpen) {
        SettingsDialog(
            state = state,
            onModelChange = viewModel::setModel,
            onSearchQueryChange = viewModel::onSearchQueryChanged,
            onSearch = viewModel::search,
            onPlaceSelected = viewModel::selectPlace,
            onLatitudeChange = viewModel::onLatitudeChanged,
            onLongitudeChange = viewModel::onLongitudeChanged,
            onUseCoordinates = viewModel::submitCoordinates,
            onUseDeviceLocation = requestDeviceLocation,
            onOpenAppSettings = { context.openAppSettings() },
            onDismiss = { settingsOpen = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeatherTopBar(
    state: UiState,
    forecast: Forecast?,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = if (state.fromDeviceLocation) {
                        stringResource(R.string.current_location)
                    } else {
                        state.place.label()
                    },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle(state, forecast),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        actions = {
            TextButton(onClick = onRefresh) { Text(stringResource(R.string.refresh)) }
            TextButton(onClick = onSettings) { Text(stringResource(R.string.settings)) }
        },
    )
}

@Composable
private fun subtitle(state: UiState, forecast: Forecast?): String = when {
    state.error != null -> "${stringResource(R.string.error_title)}: ${state.error}"

    forecast == null -> stringResource(if (state.locating) R.string.locating else R.string.loading)

    // The top bar has one line, so the model label's resolution parenthetical stays out of it.
    else -> listOf(
        (MODEL_LABELS[forecast.model] ?: forecast.model).substringBefore(" ("),
        Fmt.clockSeconds(forecast.fetchedAt),
    ).joinToString(" \u00b7 ")
}

private fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    )
    runCatching { startActivity(intent) }
}
