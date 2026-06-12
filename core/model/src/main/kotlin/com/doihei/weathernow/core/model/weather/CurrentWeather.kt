package com.doihei.weathernow.core.model.weather

// iOS の struct CurrentWeather: Sendable, Equatable に対応
// data class は equals / hashCode / copy / toString を自動生成する
// Sendable 相当のマークは不要（val のみで構成された data class はスレッドセーフ）
data class CurrentWeather(
    val temperature: Double, // temperature_2m
    val feelsLike: Double, // apparent_temperature
    val humidity: Int, // relative humidity_2m
    val windSpeed: Double, // winds peed_10m (km/h)
    val code: WeatherCode,
)
