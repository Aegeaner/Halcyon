package dev.meteo.weather.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.meteo.weather.data.ForecastRepository
import dev.meteo.weather.data.SettingsStore
import dev.meteo.weather.data.model.Place
import dev.meteo.weather.data.responseCache
import dev.meteo.weather.domain.CoordinateInput
import dev.meteo.weather.domain.CoordinateParse
import dev.meteo.weather.domain.Fmt
import dev.meteo.weather.location.LocationProvider
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Non-fatal situation the UI turns into localised text. */
enum class Notice {
    LOCATION_PERMISSION_DENIED,
    LOCATION_UNAVAILABLE,
}

/**
 * Holds the forecast, the selected place and the user's settings.
 *
 * All network work happens on [Dispatchers.IO]; a newer request cancels the previous one, so a
 * slow response can never overwrite a fresh one.
 */
class WeatherViewModel(
    private val settings: SettingsStore,
    private val repository: ForecastRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(
        UiState(
            place = settings.place,
            model = settings.model,
            fromDeviceLocation = settings.placeIsDeviceLocation,
        ),
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var loadJob: Job? = null
    private var locateJob: Job? = null
    private var searchJob: Job? = null

    /**
     * True when the screen should try the device position on start: nothing has been chosen yet,
     * or the saved place came from a fix. A place the user searched for is left alone.
     */
    val shouldUseDeviceLocation: Boolean
        get() = !settings.hasPlace || settings.placeIsDeviceLocation

    init {
        load(force = false)
    }

    // ------------------------------------------------------------------ data -- //

    /** Manual refresh: bypasses the response cache, like pressing `r` in the terminal version. */
    fun refresh() = load(force = true)

    /** Timer refresh: reuses the cache, like the terminal version's interval timer. */
    fun refreshIfStale() = load(force = false)

    private fun load(force: Boolean) {
        val request = _state.value
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    repository.forecast(
                        place = request.place,
                        model = request.model,
                        force = force,
                    )
                }
            }
            result
                .onSuccess { forecast ->
                    _state.update { it.copy(forecast = forecast, loading = false, error = null) }
                }
                .onFailure { cause ->
                    if (cause is CancellationException) throw cause
                    val message = cause.message ?: cause::class.java.simpleName
                    _state.update { it.copy(loading = false, error = message) }
                    _messages.tryEmit("Forecast request failed: $message")
                }
        }
    }

    // -------------------------------------------------------------- location -- //

    /** Ask the device for a position and switch to it; falls back to the saved place. */
    fun locate() {
        if (!locationProvider.hasPermission()) {
            onLocationPermissionDenied()
            return
        }
        locateJob?.cancel()
        locateJob = viewModelScope.launch {
            _state.update { it.copy(locating = true) }
            val fix = runCatching { locationProvider.currentFix() }.getOrNull()
            if (fix == null) {
                // The device position is the primary source; without it, offer the manual fallback.
                _state.update {
                    it.copy(
                        locating = false,
                        notice = Notice.LOCATION_UNAVAILABLE,
                        openSettings = true,
                    )
                }
                return@launch
            }
            val place = Place(name = fix.label(), latitude = fix.latitude, longitude = fix.longitude)
            val current = _state.value
            val moved = place.latitude != current.place.latitude ||
                place.longitude != current.place.longitude
            settings.place = place
            settings.placeIsDeviceLocation = true
            _state.update {
                it.copy(place = place, fromDeviceLocation = true, locating = false, notice = null)
            }
            if (moved || current.visibleForecast == null) load(force = false)
        }
    }

    fun onLocationPermissionDenied() {
        _state.update {
            it.copy(
                locating = false,
                notice = Notice.LOCATION_PERMISSION_DENIED,
                openSettings = true,
            )
        }
    }

    /** The settings sheet has been shown, so the request is consumed. */
    fun onSettingsShown() {
        if (_state.value.openSettings) _state.update { it.copy(openSettings = false) }
    }

    // ---------------------------------------------------------------- places -- //

    fun onSearchQueryChanged(query: String) {
        _state.update {
            it.copy(searchQuery = query, searchPerformed = false, searchResults = emptyList(), searchError = null)
        }
    }

    /** Manual latitude/longitude entry; the geocoding API covers the by-name path. */
    fun onLatitudeChanged(value: String) {
        _state.update { it.copy(latitudeInput = value, coordinateError = null) }
    }

    fun onLongitudeChanged(value: String) {
        _state.update { it.copy(longitudeInput = value, coordinateError = null) }
    }

    fun submitCoordinates() {
        val current = _state.value
        when (val parsed = CoordinateInput.parse(current.latitudeInput, current.longitudeInput)) {
            is CoordinateParse.Valid -> selectPlace(
                Place(
                    name = Fmt.coordinates(parsed.latitude, parsed.longitude),
                    latitude = parsed.latitude,
                    longitude = parsed.longitude,
                ),
            )

            CoordinateParse.Empty -> _state.update { it.copy(coordinateError = null) }
            CoordinateParse.Incomplete ->
                _state.update { it.copy(coordinateError = CoordinateError.INCOMPLETE) }

            CoordinateParse.NotANumber, CoordinateParse.OutOfRange ->
                _state.update { it.copy(coordinateError = CoordinateError.INVALID) }
        }
    }

    /** Explicit search, so typing never triggers a request per keystroke. */
    fun search() {
        val query = _state.value.searchQuery.trim()
        if (query.isEmpty()) {
            _state.update {
                it.copy(searchPerformed = false, searchResults = emptyList(), searchError = null)
            }
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.update { it.copy(searching = true, searchPerformed = true, searchError = null) }
            val result = runCatching {
                withContext(Dispatchers.IO) { repository.geocode(query) }
            }
            result
                .onSuccess { places ->
                    _state.update {
                        it.copy(searching = false, searchResults = places, searchError = null)
                    }
                }
                .onFailure { cause ->
                    if (cause is CancellationException) throw cause
                    val message = cause.message ?: cause::class.java.simpleName
                    _state.update {
                        it.copy(searching = false, searchResults = emptyList(), searchError = message)
                    }
                    _messages.tryEmit("Location search failed: $message")
                }
        }
    }

    fun selectPlace(place: Place) {
        settings.place = place
        settings.placeIsDeviceLocation = false
        _state.update {
            it.copy(
                place = place,
                fromDeviceLocation = false,
                searchQuery = "",
                searchResults = emptyList(),
                searchPerformed = false,
                coordinateError = null,
                notice = null,
            )
        }
        load(force = false)
    }

    // -------------------------------------------------------------- settings -- //

    fun setModel(model: String) {
        if (model == _state.value.model) return
        settings.model = model
        _state.update { it.copy(model = model) }
        load(force = false)
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return viewModelFactory {
                initializer {
                    WeatherViewModel(
                        settings = SettingsStore(appContext),
                        repository = ForecastRepository(cache = responseCache(appContext.cacheDir)),
                        locationProvider = LocationProvider(appContext),
                    )
                }
            }
        }
    }
}
