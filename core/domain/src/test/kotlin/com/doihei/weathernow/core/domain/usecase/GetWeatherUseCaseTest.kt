package com.doihei.weathernow.core.domain.usecase

import com.doihei.weathernow.core.domain.exception.WeatherException
import com.doihei.weathernow.core.domain.fake.FakeLocationService
import com.doihei.weathernow.core.domain.fake.FakeWeatherRepository
import com.doihei.weathernow.core.domain.fake.makeLocation
import com.doihei.weathernow.core.domain.fake.makeWeather
import com.doihei.weathernow.core.model.error.WeatherError
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("GetWeatherUseCase")
class GetWeatherUseCaseTest {
    // ---- 正常系 ----

    @Nested
    @DisplayName("成功パス")
    inner class SuccessPath {
        @Test
        @DisplayName("位置情報と天気の取得が成功すると Weather が返る")
        fun `returns weather when location and fetch succeed`() =
            runTest {
                val expectedWeather = makeWeather(temperature = 25.0)
                val useCase =
                    GetWeatherUseCase(
                        repository =
                            FakeWeatherRepository(
                                fetchResult = Result.success(expectedWeather),
                            ),
                        locationService = FakeLocationService(),
                    )

                val result = useCase()

                assertTrue(result.isSuccess)
                assertEquals(25.0, result.getOrThrow().current.temperature)
            }

        @Test
        @DisplayName("位置情報の座標が Repository に正しく渡される")
        fun `location coordinates are passed to repository correctly`() =
            runTest {
                // iOS の #expect(repository.lastFetchLatitude == 35.6895) に対応
                val fakeLocation = makeLocation(latitude = 35.6895, longitude = 139.6917)
                val fakeRepository = FakeWeatherRepository()
                val useCase =
                    GetWeatherUseCase(
                        repository = fakeRepository,
                        locationService = FakeLocationService(result = Result.success(fakeLocation)),
                    )

                useCase()

                assertEquals(35.6895, fakeRepository.lastFetchLatitude)
                assertEquals(139.6917, fakeRepository.lastFetchLongitude)
            }
    }

    // ---- 位置情報失敗 ----

    @Nested
    @DisplayName("位置情報の失敗")
    inner class LocationFailure {
        @Test
        @DisplayName("位置情報の取得に失敗した場合、天気取得を試みずに失敗を返す")
        fun `returns failure immediately when location fails`() =
            runTest {
                // iOS の early return テストに対応
                // getOrElse { return Result.failure(it) } の分岐を検証する
                val fakeRepository = FakeWeatherRepository()
                val useCase =
                    GetWeatherUseCase(
                        repository = fakeRepository,
                        locationService =
                            FakeLocationService(
                                result =
                                    Result.failure(
                                        WeatherException(WeatherError.LocationUnavailable),
                                    ),
                            ),
                    )

                val result = useCase()

                // 失敗が返ること
                assertTrue(result.isFailure)
                // Repository は一切呼ばれていないこと（early return の確認）
                assertEquals(0, fakeRepository.fetchCallCount)
            }

        @Test
        @DisplayName("位置情報失敗時のエラーが WeatherException として伝搬する")
        fun `location failure error propagates as WeatherException`() =
            runTest {
                val useCase =
                    GetWeatherUseCase(
                        repository = FakeWeatherRepository(),
                        locationService =
                            FakeLocationService(
                                result =
                                    Result.failure(
                                        WeatherException(WeatherError.LocationDenied),
                                    ),
                            ),
                    )

                val result = useCase()
                val exception = result.exceptionOrNull()

                assertTrue(exception is WeatherException)
                assertEquals(
                    WeatherError.LocationDenied,
                    (exception as WeatherException).error,
                )
            }
    }

    // ---- 天気取得失敗 ----

    @Nested
    @DisplayName("天気取得の失敗")
    inner class FetchFailure {
        @Test
        @DisplayName("位置情報が成功して天気取得が失敗すると Result.failure が返る")
        fun `returns failure when fetch fails after successful location`() =
            runTest {
                val useCase =
                    GetWeatherUseCase(
                        repository =
                            FakeWeatherRepository(
                                fetchResult =
                                    Result.failure(
                                        WeatherException(WeatherError.NetworkFailure("timeout")),
                                    ),
                            ),
                        locationService = FakeLocationService(),
                    )

                val result = useCase()

                assertTrue(result.isFailure)
                val error = (result.exceptionOrNull() as? WeatherException)?.error
                assertTrue(error is WeatherError.NetworkFailure)
            }

        @Test
        @DisplayName("天気取得失敗時は LocationService は 1 回だけ呼ばれている")
        fun `location service is called exactly once even when fetch fails`() =
            runTest {
                val fakeLocationService = FakeLocationService()
                val useCase =
                    GetWeatherUseCase(
                        repository =
                            FakeWeatherRepository(
                                fetchResult =
                                    Result.failure(
                                        WeatherException(WeatherError.NetworkFailure("error")),
                                    ),
                            ),
                        locationService = fakeLocationService,
                    )

                useCase()

                // 位置情報は1回だけ取得される（リトライしない）
                assertEquals(1, fakeLocationService.callCount)
            }
    }

    // ---- operator invoke ----

    @Nested
    @DisplayName("operator invoke 構文")
    inner class OperatorInvoke {
        @Test
        @DisplayName("useCase() と useCase.invoke() は同じ結果を返す")
        fun `useCase() and useCase invoke() produce same result`() =
            runTest {
                val useCase =
                    GetWeatherUseCase(
                        repository = FakeWeatherRepository(),
                        locationService = FakeLocationService(),
                    )

                // Kotlin の operator fun invoke() の動作確認
                val resultA = useCase()
                // キャッシュの影響を避けるため別インスタンスで確認
                val useCaseB =
                    GetWeatherUseCase(
                        repository = FakeWeatherRepository(),
                        locationService = FakeLocationService(),
                    )
                val resultB = useCaseB.invoke()

                assertTrue(resultA.isSuccess)
                assertTrue(resultB.isSuccess)
            }
    }
}
