A readable hourly chart: every series now labels its own extremes.

* Temperature carries its high and low, the precipitation band its tallest bar, the wind band its strongest wind and gust, and the humidity band its dampest and driest hour — all in real values rather than a single unexplained figure.
* The precipitation and wind bands draw their zero line, and the humidity band no longer shows a fixed 0–100 scale that the curve did not follow.
* The time axis ticks every three hours, the day boundary keeps the weekday, and each band's top edge is the ceiling of its own scale, so a labelled figure sits where its value is.
* The band captions name both precipitation series, so the paler bars are recognisably the probability.

**Install** — `adb install halcyon-0.2.1.apk`, or copy it to the device and open it. Requires Android 8.0 or later (minSdk 26).

Verified by 74 unit tests and on an API 36 emulator with an injected position. Requires no Google Play services.
