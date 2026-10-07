The hourly wind line now follows the wind speed.

* The wind band carried two points per hour — the wind speed and the gust — so the solid line sawtoothed between them, stretched the first half of the page across the full width, and left the rest of its points off the canvas, out of step with the shared time axis. It now draws one point per hour, on the hour.
* The wind band's figure is the strongest wind of the page with the gust labelled separately, instead of two labels printing the same gust value, one of them clamped to the edge.

**Install** — `adb install halcyon-0.2.2.apk`, or copy it to the device and open it. Requires Android 8.0 or later (minSdk 26).

Verified by 77 unit tests and on an API 36 emulator with an injected position, where the rendered wind line matched the live ECMWF hourly wind speed within 0.34 km/h at every hour of the page. Requires no Google Play services.
