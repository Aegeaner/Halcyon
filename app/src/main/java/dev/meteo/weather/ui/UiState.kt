package dev.meteo.weather.ui

import dev.meteo.weather.data.DEFAULT_MODEL
import dev.meteo.weather.data.RefreshInterval
import dev.meteo.weather.data.model.OSLO
import dev.meteo.weather.data.model.Forecast
import dev.meteo.weather.data.model.Place

/** Why a manual coordinate entry was rejected. */
enum class CoordinateError {
    INCOMPLETE,
    INVALID,
}

/** Everything the screen renders; immutable and derived only from the view model. */
data class UiState(
    val place: Place = OSLO,
    val model: String = DEFAULT_MODEL,
    val refreshInterval: RefreshInterval = RefreshInterval.DEFAULT,
    val forecast: Forecast? = null,
    val loading: Boolean = false,
    val locating: Boolean = false,
    /** True when the selected place came from a device fix rather than a search. */
    val fromDeviceLocation: Boolean = false,
    val notice: Notice? = null,
    /** Page whose hourly table is open, or null for the forecast overview. */
    val detailsPage: Int? = null,
    /** True when the app should open the settings sheet, e.g. to offer the manual fallback. */
    val openSettings: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val searchResults: List<Place> = emptyList(),
    val searching: Boolean = false,
    val searchPerformed: Boolean = false,
    val searchError: String? = null,
    val latitudeInput: String = "",
    val longitudeInput: String = "",
    /** Set when the last coordinate submission was rejected; cleared on the next edit or place. */
    val coordinateError: CoordinateError? = null,
) {
    /** Forecasts whose place does not match the selected place are stale by definition. */
    val visibleForecast: Forecast?
        get() = forecast?.takeIf {
            it.place.name == place.name &&
                it.place.latitude == place.latitude &&
                it.place.longitude == place.longitude
        }
}
