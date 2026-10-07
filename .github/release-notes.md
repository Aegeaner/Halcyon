Hourly forecast as a chart, and the table it replaces one tap away.

* Every 24-hour span now draws a meteogram: temperature as a line with its highs and lows labelled, precipitation as bars over a fainter precipitation-probability band, wind as a line with gusts dashed above it, and humidity as a thin line — all on one shared time axis.
* Tapping a chart, or `Details`, opens that span's hourly table: time, weather, temperature, apparent temperature, precipitation, probability, wind, gusts and humidity.
* Current conditions, the 16-day list, device location and the city/coordinate fallback are unchanged.

**Install** — `adb install halcyon-0.2.0.apk`, or copy it to the device and open it. Requires Android 8.0 or later (minSdk 26).

Verified by 68 unit tests and on an API 36 emulator with an injected position. Requires no Google Play services.
