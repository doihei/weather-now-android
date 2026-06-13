package com.doihei.weathernow.core.domain.repository

import com.doihei.weathernow.core.domain.exception.WeatherException
import com.doihei.weathernow.core.model.error.WeatherError
import com.doihei.weathernow.core.network.api.OpenMeteoApi
import com.doihei.weathernow.core.network.dto.ForecastCurrentDto
import com.doihei.weathernow.core.network.dto.ForecastDailyDto
import com.doihei.weathernow.core.network.dto.ForecastHourlyDto
import com.doihei.weathernow.core.network.dto.ForecastResponseDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("DefaultWeatherRepository")
class DefaultWeatherRepositoryTest {
    // MockK で OpenMeteoApi をモック化
    // iOS の withDependencies { $0.weatherClient = .mock } に対応
    private lateinit var mockApi: OpenMeteoApi
    private lateinit var repository: DefaultWeatherRepository

    @BeforeEach
    fun setUp() {
        mockApi = mockk()
        repository = DefaultWeatherRepository(api = mockApi)
    }

    // ---- テスト用 DTO ファクトリ ----
    // 最小構成の ForecastResponseDto を生成する
    private fun makeForecastResponseDto() =
        ForecastResponseDto(
            current =
                ForecastCurrentDto(
                    time = "2024-01-15T12:00",
                    temperature2m = 20.0,
                    apparentTemperature = 18.0,
                    relativeHumidity2m = 60,
                    weatherCode = 0,
                    windSpeed10m = 10.0,
                ),
            hourly =
                ForecastHourlyDto(
                    time = emptyList(),
                    temperature2m = emptyList(),
                    precipitation = emptyList(),
                    weatherCode = emptyList(),
                ),
            daily =
                ForecastDailyDto(
                    time = emptyList(),
                    temperature2mMax = emptyList(),
                    temperature2mMin = emptyList(),
                    precipitationProbabilityMax = emptyList(),
                    weatherCode = emptyList(),
                ),
        )

    // ---- 正常系 ----

    @Nested
    @DisplayName("fetchWeather — 正常系")
    inner class FetchWeatherSuccess {
        @Test
        @DisplayName("API レスポンスが Weather ドメインモデルに変換されて返る")
        fun `api response is converted to weather domain model`() =
            runTest {
                coEvery { mockApi.forecast(any(), any(), any(), any(), any(), any(), any()) } returns
                    makeForecastResponseDto()

                val result = repository.fetchWeather(35.6895, 139.6917)

                assertTrue(result.isSuccess)
                assertEquals(20.0, result.getOrThrow().current.temperature)
            }

        @Test
        @DisplayName("同じ座標で 2 回呼ぶと API は 1 回しか呼ばれない（キャッシュヒット）")
        fun `second call with same coordinates hits cache and skips api`() =
            runTest {
                // iOS の actor キャッシュテストに対応
                // Mutexキャッシュがヒットしたとき API 呼び出しが増えないことを検証
                coEvery { mockApi.forecast(any(), any(), any(), any(), any(), any(), any()) } returns
                    makeForecastResponseDto()

                repository.fetchWeather(35.6895, 139.6917) // 1回目：API を呼ぶ
                repository.fetchWeather(35.6895, 139.6917) // 2回目：キャッシュから返る

                // API は 1 回だけ呼ばれたことを検証
                // iOS の #expect(callCounter.count == 1) に対応
                coVerify(exactly = 1) {
                    mockApi.forecast(any(), any(), any(), any(), any(), any(), any())
                }
            }

        @Test
        @DisplayName("異なる座標では別々にキャッシュされる")
        fun `different coordinates are cached independently`() =
            runTest {
                coEvery { mockApi.forecast(any(), any(), any(), any(), any(), any(), any()) } returns
                    makeForecastResponseDto()

                repository.fetchWeather(35.6895, 139.6917) // Tokyo
                repository.fetchWeather(34.6937, 135.5023) // Osaka

                // 座標が違うのでキャッシュキーが異なり、API は 2 回呼ばれる
                coVerify(exactly = 2) {
                    mockApi.forecast(any(), any(), any(), any(), any(), any(), any())
                }
            }
    }

    // ---- キャッシュ ----

    @Nested
    @DisplayName("fetchWeather — キャッシュ挙動")
    inner class FetchWeatherCache {
        @Test
        @DisplayName("キャッシュが有効な間は Result.success が返り続ける")
        fun `cached result is returned as success`() =
            runTest {
                coEvery { mockApi.forecast(any(), any(), any(), any(), any(), any(), any()) } returns
                    makeForecastResponseDto()

                val first = repository.fetchWeather(35.6895, 139.6917)
                val second = repository.fetchWeather(35.6895, 139.6917)

                // どちらも成功で、同じ weather が返ること
                assertTrue(first.isSuccess)
                assertTrue(second.isSuccess)
                assertEquals(
                    first.getOrThrow().current.temperature,
                    second.getOrThrow().current.temperature,
                )
            }
    }

    // ---- ネットワーク失敗 ----

    @Nested
    @DisplayName("fetchWeather — ネットワーク失敗")
    inner class FetchWeatherFailure {
        @Test
        @DisplayName("API が例外を投げると Result.failure(WeatherException) が返る")
        fun `api exception is wrapped in WeatherException`() =
            runTest {
                // iOS の catch let error → WeatherError.networkFailure に対応
                coEvery {
                    mockApi.forecast(any(), any(), any(), any(), any(), any(), any())
                } throws RuntimeException("Network error")

                val result = repository.fetchWeather(35.6895, 139.6917)

                assertTrue(result.isFailure)
                val exception = result.exceptionOrNull()
                assertTrue(exception is WeatherException)
                val weatherError = (exception as WeatherException).error
                assertTrue(weatherError is WeatherError.NetworkFailure)
            }

        @Test
        @DisplayName("ネットワーク失敗時はキャッシュに書き込まれない")
        fun `failed result is not cached`() =
            runTest {
                // 失敗したあとにキャッシュヒットしないことを確認
                // 1回目：失敗、2回目：成功 → API は 2 回呼ばれるはず
                coEvery {
                    mockApi.forecast(any(), any(), any(), any(), any(), any(), any())
                } throws RuntimeException("Network error") andThen makeForecastResponseDto()

                val firstResult = repository.fetchWeather(35.6895, 139.6917)
                val secondResult = repository.fetchWeather(35.6895, 139.6917)

                assertTrue(firstResult.isFailure)
                assertTrue(secondResult.isSuccess)

                // キャッシュに入っていないので 2 回 API が呼ばれる
                coVerify(exactly = 2) {
                    mockApi.forecast(any(), any(), any(), any(), any(), any(), any())
                }
            }
    }

    // ---- searchCities ----

    @Nested
    @DisplayName("searchCities")
    inner class SearchCities {
        @Test
        @DisplayName("検索結果が GeocodingResult リストとして返る")
        fun `search results are returned as geocoding result list`() =
            runTest {
                coEvery {
                    mockApi.searchCities(any(), any(), any())
                } returns
                    com.doihei.weathernow.core.network.dto.GeocodingResponseDto(
                        results =
                            listOf(
                                com.doihei.weathernow.core.network.dto.GeocodingResultItemDto(
                                    id = 1850147,
                                    name = "Tokyo",
                                    country = "Japan",
                                    latitude = 35.6895,
                                    longitude = 139.6917,
                                ),
                            ),
                    )

                val result = repository.searchCities("Tokyo")

                assertTrue(result.isSuccess)
                assertEquals(1, result.getOrThrow().size)
                assertEquals("Tokyo", result.getOrThrow()[0].name)
            }

        @Test
        @DisplayName("API が例外を投げると Result.failure が返る")
        fun `api exception returns failure`() =
            runTest {
                coEvery {
                    mockApi.searchCities(any(), any(), any())
                } throws RuntimeException("Search failed")

                val result = repository.searchCities("Tokyo")

                assertTrue(result.isFailure)
                assertTrue(result.exceptionOrNull() is WeatherException)
            }
    }
}
