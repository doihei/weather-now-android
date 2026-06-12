package com.doihei.weathernow.core.model.weather

import java.time.LocalDateTime

// iOS の struct HourlyForecast: Sendable, Equatable, Identifiable に対応
data class HourlyForecast(
    val time: LocalDateTime,
    val temperature: Double,
    val precipitation: Double, // mm
    val code: WeatherCode,
) {
    // ISO 8601 形式の文字列を id に（"2024-01-15T12:00" 形式）
    val id: String get() = time.toString()
}
