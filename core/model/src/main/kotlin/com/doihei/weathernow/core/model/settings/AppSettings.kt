package com.doihei.weathernow.core.model.settings

// iOS の struct AppSettings: Sendable, Equatable に対応
data class AppSettings(
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val windUnit: WindUnit = WindUnit.KMH,
    val theme: Theme = Theme.SYSTEM,
) {
    // iOS の enum TemperatureUnit に対応
    enum class TemperatureUnit(
        val symbol: String,
    ) {
        CELSIUS("℃"),
        FAHRENHEIT("℉"),
        ;

        // iOS の func convert(_ celsius: Double) -> Double に対応
        fun convert(celsius: Double): Double =
            when (this) {
                CELSIUS -> celsius
                FAHRENHEIT -> celsius * FAHRENHEIT_MULTIPLIER / FAHRENHEIT_DIVISOR + FAHRENHEIT_OFFSET
            }

        companion object {
            private const val FAHRENHEIT_MULTIPLIER = 9.0
            private const val FAHRENHEIT_DIVISOR = 5.0
            private const val FAHRENHEIT_OFFSET = 32.0
        }
    }

    // iOS の enum WindUnit に対応
    enum class WindUnit(
        val symbol: String,
    ) {
        KMH("km/h"),
        MPH("mph"),
        ;

        fun convert(kmh: Double): Double =
            when (this) {
                KMH -> kmh
                MPH -> kmh * MPH_FACTOR
            }

        companion object {
            private const val MPH_FACTOR = 0.621371
        }
    }

    // iOS の enum Theme に対応
    enum class Theme(
        val displayName: String,
    ) {
        SYSTEM("システム"),
        LIGHT("ライト"),
        DARK("ダーク"),
    }

    companion object {
        // iOS の static let `default` に対応
        val DEFAULT = AppSettings()
    }
}
