package com.doihei.weathernow.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// iOS の ForecastResponse（Decodable）に対応
// DTO なので Domain モデルとは分けて定義する。
// iOS では struct ForecastResponse: Decodable として Responses/ に置いていた。
@Serializable
data class ForecastResponseDto(
    val current: ForecastCurrentDto,
    val hourly: ForecastHourlyDto,
    val daily: ForecastDailyDto,
)

// iOS の ForecastCurrentResponse に対応
// CodingKeys の snake_case → camelCase 変換を @SerialName で表現
@Serializable
data class ForecastCurrentDto(
    val time: String,
    @SerialName("temperature_2m") val temperature2m: Double,
    @SerialName("apparent_temperature") val apparentTemperature: Double,
    @SerialName("relativehumidity_2m") val relativeHumidity2m: Int,
    @SerialName("weathercode") val weatherCode: Int,
    @SerialName("windspeed_10m") val windSpeed10m: Double,
)

// iOS の ForecastHourlyResponse に対応
@Serializable
data class ForecastHourlyDto(
    val time: List<String>,
    @SerialName("temperature_2m") val temperature2m: List<Double>,
    val precipitation: List<Double>,
    @SerialName("weathercode") val weatherCode: List<Int>,
)

// iOS の ForecastDailyResponse に対応
@Serializable
data class ForecastDailyDto(
    val time: List<String>,
    @SerialName("temperature_2m_max") val temperature2mMax: List<Double>,
    @SerialName("temperature_2m_min") val temperature2mMin: List<Double>,
    @SerialName("precipitation_probability_max") val precipitationProbabilityMax: List<Int>,
    @SerialName("weathercode") val weatherCode: List<Int>,
)
