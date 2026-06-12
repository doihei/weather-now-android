package com.doihei.weathernow.core.model.weather

// iOS の struct Weather: Sendable, Equatable に対応
// API レスポンス全体を表すルートモデル
data class Weather(
    val current: CurrentWeather,
    val hourly: List<HourlyForecast>,
    val daily: List<DailyForecast>,
)
