package com.doihei.weathernow.core.network.mapper

import com.doihei.weathernow.core.model.weather.CurrentWeather
import com.doihei.weathernow.core.model.weather.DailyForecast
import com.doihei.weathernow.core.model.weather.HourlyForecast
import com.doihei.weathernow.core.model.weather.Weather
import com.doihei.weathernow.core.model.weather.WeatherCode
import com.doihei.weathernow.core.network.dto.ForecastResponseDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// iOS の extension ForecastResponse { func toWeather() -> Weather } に対応
// Kotlin では extension 関数として定義する（同じ感覚で書ける）
fun ForecastResponseDto.toWeather(): Weather =
    Weather(
        current =
            current.let {
                CurrentWeather(
                    temperature = it.temperature2m,
                    feelsLike = it.apparentTemperature,
                    humidity = it.relativeHumidity2m,
                    windSpeed = it.windSpeed10m,
                    code = WeatherCode.from(it.weatherCode),
                )
            },
        hourly = toHourlyForecasts(),
        daily = toDailyForecasts(),
    )

// iOS の hourly 変換ロジック（zip + compactMap）に対応
// Kotlin では zip で index と time を合わせ、不正な日時はスキップする
private fun ForecastResponseDto.toHourlyForecasts(): List<HourlyForecast> {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
    return hourly.time.indices.mapNotNull { i ->
        val time =
            runCatching {
                LocalDateTime.parse(hourly.time[i], formatter)
            }.getOrNull() ?: return@mapNotNull null

        HourlyForecast(
            time = time,
            temperature = hourly.temperature2m[i],
            precipitation = hourly.precipitation[i],
            code = WeatherCode.from(hourly.weatherCode[i]),
        )
    }
}

// iOS の daily 変換ロジックに対応
private fun ForecastResponseDto.toDailyForecasts(): List<DailyForecast> {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    return daily.time.indices.mapNotNull { i ->
        val date =
            runCatching {
                LocalDate.parse(daily.time[i], formatter)
            }.getOrNull() ?: return@mapNotNull null

        DailyForecast(
            date = date,
            maxTemp = daily.temperature2mMax[i],
            minTemp = daily.temperature2mMin[i],
            precipitationProb = daily.precipitationProbabilityMax[i],
            code = WeatherCode.from(daily.weatherCode[i]),
        )
    }
}
