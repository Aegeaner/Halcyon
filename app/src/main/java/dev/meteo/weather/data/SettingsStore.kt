package dev.meteo.weather.data

import android.content.Context
import androidx.core.content.edit
import dev.meteo.weather.data.model.OSLO
import dev.meteo.weather.data.model.Place

/** Model choice and the last place used, persisted across launches. */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("weather-settings", Context.MODE_PRIVATE)

    var model: String
        get() = prefs.getString(KEY_MODEL, null) ?: DEFAULT_MODEL
        set(value) = prefs.edit { putString(KEY_MODEL, value) }

    var refreshInterval: RefreshInterval
        get() = RefreshInterval.fromSeconds(prefs.getInt(KEY_REFRESH_SECONDS, Int.MIN_VALUE))
        set(value) = prefs.edit { putInt(KEY_REFRESH_SECONDS, value.seconds) }

    /**
     * Last place used; Oslo before anything is stored.
     * A place that came from a device fix is labelled `lat, lon` exactly like `--lat/--lon`.
     */
    var place: Place
        get() {
            val name = prefs.getString(KEY_NAME, null)
            val latitude = prefs.getString(KEY_LATITUDE, null)?.toDoubleOrNull()
            val longitude = prefs.getString(KEY_LONGITUDE, null)?.toDoubleOrNull()
            if (name.isNullOrEmpty() || latitude == null || longitude == null) return OSLO
            return Place(
                name = name,
                latitude = latitude,
                longitude = longitude,
                country = prefs.getString(KEY_COUNTRY, null),
                admin1 = prefs.getString(KEY_ADMIN1, null),
            )
        }
        set(value) = prefs.edit {
            putString(KEY_NAME, value.name)
            putString(KEY_LATITUDE, value.latitude.toString())
            putString(KEY_LONGITUDE, value.longitude.toString())
            putString(KEY_COUNTRY, value.country)
            putString(KEY_ADMIN1, value.admin1)
        }

    /** True once a place has been chosen; false only before the very first run. */
    val hasPlace: Boolean
        get() = prefs.contains(KEY_LATITUDE) && prefs.contains(KEY_LONGITUDE)

    /** True when [place] came from a device fix, so a fresh fix may replace it silently. */
    var placeIsDeviceLocation: Boolean
        get() = prefs.getBoolean(KEY_FROM_DEVICE, false)
        set(value) = prefs.edit { putBoolean(KEY_FROM_DEVICE, value) }

    private companion object {
        const val KEY_MODEL = "model"
        const val KEY_REFRESH_SECONDS = "refresh.seconds"
        const val KEY_NAME = "place.name"
        const val KEY_LATITUDE = "place.latitude"
        const val KEY_LONGITUDE = "place.longitude"
        const val KEY_COUNTRY = "place.country"
        const val KEY_ADMIN1 = "place.admin1"
        const val KEY_FROM_DEVICE = "place.from_device"
    }
}
