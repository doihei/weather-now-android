package com.doihei.weathernow.core.model.weather

import java.time.LocalDate

// iOS の struct DailyForecast: Sendable, Equatable, Identifiable に対応
// Identifiable（id: String）はプロパティとして表現
data class DailyForecast(
    val date: LocalDate,
    val maxTemp: Double,
    val minTemp: Double,
    val precipitationProb: Int, // %
    val code: WeatherCode,
) {
    // iOS の id: String（ISO8601DateFormatter で生成）に対応
    // Kotlin では computed property として定義
    val id: String get() = date.toString()
}
