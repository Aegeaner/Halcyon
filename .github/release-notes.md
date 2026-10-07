First release of the Open-Meteo client for ECMWF's native-resolution **IFS HRES** forecast.

* Current conditions for the device position, the next 72 h as three 24-hour pages, and the next 16 days — SI units throughout.
* Location from the fused provider, the framework `LocationManager` where Play services are absent, and a cached fix as a placeholder; a city search or manual latitude/longitude takes over when the permission is denied.
* Responses cached on disk for an hour, refreshed every 900 s while the screen is visible.

**Install** — `adb install halcyon-0.1.0.apk`, or copy it to the device and open it. Requires Android 8.0 or later (minSdk 26).

Verified by 61 unit tests and on an API 36 emulator with an injected position. Requires no Google Play services.
