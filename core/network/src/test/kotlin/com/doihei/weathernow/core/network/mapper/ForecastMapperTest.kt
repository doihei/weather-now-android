package com.doihei.weathernow.core.network.mapper

import com.doihei.weathernow.core.model.weather.WeatherCode
import com.doihei.weathernow.core.network.dto.ForecastCurrentDto
import com.doihei.weathernow.core.network.dto.ForecastDailyDto
import com.doihei.weathernow.core.network.dto.ForecastHourlyDto
import com.doihei.weathernow.core.network.dto.ForecastResponseDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

// iOS の struct ForecastMapperTests に対応
// 1テスト1関心事の設計を Nested class で表現する
@DisplayName("ForecastMapper")
class ForecastMapperTest {
    // ---- テスト用フィクスチャ ----

    // 正常な ForecastCurrentDto を生成するヘルパー
    // iOS の makeCurrentResponse() に対応
    private fun makeCurrentDto(
        temperature2m: Double = 20.0,
        apparentTemperature: Double = 18.0,
        relativeHumidity2m: Int = 60,
        weatherCode: Int = 0, // CLEAR_SKY
        windSpeed10m: Double = 10.0,
    ) = ForecastCurrentDto(
        time = "2024-01-15T12:00",
        temperature2m = temperature2m,
        apparentTemperature = apparentTemperature,
        relativeHumidity2m = relativeHumidity2m,
        weatherCode = weatherCode,
        windSpeed10m = windSpeed10m,
    )

    // 正常な hourly DTO（2エントリ）
    private fun makeHourlyDto(
        times: List<String> =
            listOf(
                "2024-01-15T12:00",
                "2024-01-15T13:00",
            ),
    ) = ForecastHourlyDto(
        time = times,
        temperature2m = listOf(20.0, 21.0),
        precipitation = listOf(0.0, 0.1),
        weatherCode = listOf(0, 1),
    )

    // 正常な daily DTO（2エントリ）
    private fun makeDailyDto(
        times: List<String> =
            listOf(
                "2024-01-15",
                "2024-01-16",
            ),
    ) = ForecastDailyDto(
        time = times,
        temperature2mMax = listOf(22.0, 23.0),
        temperature2mMin = listOf(15.0, 16.0),
        precipitationProbabilityMax = listOf(10, 20),
        weatherCode = listOf(0, 1),
    )

    private fun makeResponseDto(
        current: ForecastCurrentDto = makeCurrentDto(),
        hourly: ForecastHourlyDto = makeHourlyDto(),
        daily: ForecastDailyDto = makeDailyDto(),
    ) = ForecastResponseDto(current = current, hourly = hourly, daily = daily)

    // ---- CurrentWeather の変換 ----

    @Nested
    @DisplayName("CurrentWeather への変換")
    inner class CurrentWeatherConversion {
        @Test
        @DisplayName("全フィールドが正しく Domain モデルにマッピングされる")
        fun `maps all fields correctly`() {
            val dto =
                makeResponseDto(
                    current =
                        makeCurrentDto(
                            temperature2m = 25.5,
                            apparentTemperature = 23.0,
                            relativeHumidity2m = 70,
                            weatherCode = 61, // LIGHT_RAIN
                            windSpeed10m = 15.0,
                        ),
                )

            val weather = dto.toWeather()

            assertEquals(25.5, weather.current.temperature)
            assertEquals(23.0, weather.current.feelsLike)
            assertEquals(70, weather.current.humidity)
            assertEquals(15.0, weather.current.windSpeed)
            assertEquals(WeatherCode.LIGHT_RAIN, weather.current.code)
        }

        @Test
        @DisplayName("未知の WMO コードは WeatherCode.UNKNOWN にフォールバックする")
        fun `unknown WMO code falls back to UNKNOWN`() {
            // iOS の guard let + UNKNOWN フォールバックに対応
            val dto = makeResponseDto(current = makeCurrentDto(weatherCode = 9999))

            val weather = dto.toWeather()

            assertEquals(WeatherCode.UNKNOWN, weather.current.code)
        }
    }

    // ---- HourlyForecast の変換 ----

    @Nested
    @DisplayName("HourlyForecast リストへの変換")
    inner class HourlyForecastConversion {
        @Test
        @DisplayName("正常な時刻文字列は HourlyForecast に変換される")
        fun `valid time strings are converted to HourlyForecast`() {
            val dto = makeResponseDto()

            val weather = dto.toWeather()

            assertEquals(2, weather.hourly.size)
            assertEquals(LocalDateTime.of(2024, 1, 15, 12, 0), weather.hourly[0].time)
            assertEquals(20.0, weather.hourly[0].temperature)
            assertEquals(WeatherCode.CLEAR_SKY, weather.hourly[0].code)
        }

        @Test
        @DisplayName("不正な日時文字列のエントリはスキップされる")
        fun `invalid time string entries are skipped`() {
            // iOS の compactMap { ... guard let date = ... else { return nil } } に対応
            // Kotlin の mapNotNull + runCatching が不正日時をスキップする境界値テスト
            val hourly =
                makeHourlyDto(
                    times =
                        listOf(
                            "2024-01-15T12:00", // 正常
                            "not-a-date", // 不正 → スキップされる
                            "2024-01-15T14:00", // 正常
                        ),
                ).copy(
                    temperature2m = listOf(20.0, 99.0, 22.0),
                    precipitation = listOf(0.0, 0.0, 0.0),
                    weatherCode = listOf(0, 0, 0),
                )
            val dto = makeResponseDto(hourly = hourly)

            val weather = dto.toWeather()

            // 不正な "not-a-date" エントリがスキップされて 2件になる
            assertEquals(2, weather.hourly.size)
            assertEquals(LocalDateTime.of(2024, 1, 15, 12, 0), weather.hourly[0].time)
            assertEquals(LocalDateTime.of(2024, 1, 15, 14, 0), weather.hourly[1].time)
        }

        @Test
        @DisplayName("hourly リストが空のとき空リストを返す")
        fun `empty hourly list returns empty list`() {
            val dto =
                makeResponseDto(
                    hourly =
                        ForecastHourlyDto(
                            time = emptyList(),
                            temperature2m = emptyList(),
                            precipitation = emptyList(),
                            weatherCode = emptyList(),
                        ),
                )

            val weather = dto.toWeather()

            assertEquals(0, weather.hourly.size)
        }

        @Test
        @DisplayName("全エントリの日時が不正なとき空リストを返す")
        fun `all invalid time strings returns empty list`() {
            val hourly =
                ForecastHourlyDto(
                    time = listOf("bad1", "bad2"),
                    temperature2m = listOf(20.0, 21.0),
                    precipitation = listOf(0.0, 0.0),
                    weatherCode = listOf(0, 0),
                )
            val dto = makeResponseDto(hourly = hourly)

            val weather = dto.toWeather()

            assertEquals(0, weather.hourly.size)
        }
    }

    // ---- DailyForecast の変換 ----

    @Nested
    @DisplayName("DailyForecast リストへの変換")
    inner class DailyForecastConversion {
        @Test
        @DisplayName("正常な日付文字列は DailyForecast に変換される")
        fun `valid date strings are converted to DailyForecast`() {
            val dto = makeResponseDto()

            val weather = dto.toWeather()

            assertEquals(2, weather.daily.size)
            assertEquals(LocalDate.of(2024, 1, 15), weather.daily[0].date)
            assertEquals(22.0, weather.daily[0].maxTemp)
            assertEquals(15.0, weather.daily[0].minTemp)
            assertEquals(10, weather.daily[0].precipitationProb)
        }

        @Test
        @DisplayName("不正な日付文字列のエントリはスキップされる")
        fun `invalid date string entries are skipped`() {
            val daily =
                makeDailyDto(
                    times =
                        listOf(
                            "2024-01-15", // 正常
                            "20240116", // 不正（フォーマット違い）→ スキップ
                        ),
                ).copy(
                    temperature2mMax = listOf(22.0, 23.0),
                    temperature2mMin = listOf(15.0, 16.0),
                    precipitationProbabilityMax = listOf(10, 20),
                    weatherCode = listOf(0, 1),
                )
            val dto = makeResponseDto(daily = daily)

            val weather = dto.toWeather()

            assertEquals(1, weather.daily.size)
            assertEquals(LocalDate.of(2024, 1, 15), weather.daily[0].date)
        }
    }

    // ---- Weather ルートの構造 ----

    @Nested
    @DisplayName("Weather ルートモデルへの変換")
    inner class WeatherRootConversion {
        @Test
        @DisplayName("current / hourly / daily がすべて含まれる")
        fun `weather contains current, hourly, and daily`() {
            val dto = makeResponseDto()

            val weather = dto.toWeather()

            // 構造の存在確認（中身の検証は各 Nested で済んでいる）
            assertEquals(WeatherCode.CLEAR_SKY, weather.current.code)
            assertEquals(2, weather.hourly.size)
            assertEquals(2, weather.daily.size)
        }
    }
}
