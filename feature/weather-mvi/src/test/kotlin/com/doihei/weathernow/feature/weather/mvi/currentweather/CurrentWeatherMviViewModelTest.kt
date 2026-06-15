package com.doihei.weathernow.feature.weather.mvi.currentweather

import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.doihei.weathernow.core.domain.exception.WeatherException
import com.doihei.weathernow.core.domain.usecase.GetWeatherUseCase
import com.doihei.weathernow.core.model.error.WeatherError
import com.doihei.weathernow.core.model.weather.CurrentWeather
import com.doihei.weathernow.core.model.weather.Weather
import com.doihei.weathernow.core.model.weather.WeatherCode
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("CurrentWeatherMviViewModel")
class CurrentWeatherMviViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockUseCase: GetWeatherUseCase
    private lateinit var viewModel: CurrentWeatherMviViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockUseCase = mockk()
        viewModel = CurrentWeatherMviViewModel(getWeatherUseCase = mockUseCase)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makeWeather(temperature: Double = 20.0) =
        Weather(
            current =
                CurrentWeather(
                    temperature = temperature,
                    feelsLike = 18.0,
                    humidity = 60,
                    windSpeed = 10.0,
                    code = WeatherCode.CLEAR_SKY,
                ),
            hourly = emptyList(),
            daily = emptyList(),
        )

    // ---- 初期状態 ----

    @Nested
    @DisplayName("初期状態")
    inner class InitialState {
        @Test
        @DisplayName("OnAppear を送ると Idle → Loading → Loaded の順で State が遷移する")
        fun `OnAppear transitions state through Idle to Loading to Loaded`() =
            runTest {
                coEvery { mockUseCase() } returns Result.success(makeWeather(temperature = 22.0))

                viewModel.state.test {
                    assertEquals(
                        CurrentWeatherState.ViewState.Idle,
                        awaitItem().viewState, // 初期値
                    )

                    viewModel.onIntent(CurrentWeatherIntent.OnAppear)

                    assertEquals(
                        CurrentWeatherState.ViewState.Loading,
                        awaitItem().viewState, // Loading
                    )

                    val loaded = awaitItem().viewState
                    assertTrue(loaded is CurrentWeatherState.ViewState.Loaded)
                    assertEquals(
                        22.0,
                        (loaded as CurrentWeatherState.ViewState.Loaded).weather.current.temperature,
                    )

                    cancelAndConsumeRemainingEvents()
                }
            }

        @Test
        @DisplayName("Loading 中に OnAppear を送っても UseCase は 1 回しか呼ばれない")
        fun `OnAppear during loading does not trigger another request`() =
            runTest {
                coEvery { mockUseCase() } returns Result.success(makeWeather())

                viewModel.state.test {
                    awaitItem() // Idle を消費

                    viewModel.onIntent(CurrentWeatherIntent.OnAppear)
                    awaitItem() // Loading

                    // Loading 中に再度 OnAppear
                    viewModel.onIntent(CurrentWeatherIntent.OnAppear)

                    awaitItem() // Loaded まで待つ

                    coVerify(exactly = 1) { mockUseCase() }
                    cancelAndIgnoreRemainingEvents()
                }
            }

        @Test
        @DisplayName("Loaded 状態で OnAppear を送っても再ロードしない")
        fun `OnAppear when already Loaded does not reload`() =
            runTest {
                // iOS の guard !state.isLoaded else { return } に対応
                // 一度ロード済みなら OnAppear で再取得しない設計の検証
                coEvery { mockUseCase() } returns Result.success(makeWeather())

                viewModel.state.test {
                    awaitItem() // Idle

                    viewModel.onIntent(CurrentWeatherIntent.OnAppear)
                    awaitItem() // Loading
                    awaitItem() // Loaded

                    // Loaded 状態で OnAppear を送る
                    viewModel.onIntent(CurrentWeatherIntent.OnAppear)

                    // 新しい State が流れてこないことを確認
                    // expectNoEvents：次のイベントが来ないことを検証する
                    expectNoEvents()

                    // UseCase は最初の 1 回だけ
                    coVerify(exactly = 1) { mockUseCase() }

                    cancelAndIgnoreRemainingEvents()
                }
            }
    }

    // ---- Refresh Intent ----

    @Nested
    @DisplayName("Refresh Intent")
    inner class RefreshIntent {
        @Test
        @DisplayName("Refresh は Loaded 状態からでも再ロードする")
        fun `Refresh reloads even when already Loaded`() =
            runTest {
                // MVVM の refresh() との違い：Idle リセットをしない
                // Loading → Loaded → (Refresh) → Loading → Loaded の遷移
                coEvery { mockUseCase() } returns Result.success(makeWeather())

                viewModel.state.test {
                    awaitItem() // Idle

                    // 最初のロード
                    viewModel.onIntent(CurrentWeatherIntent.OnAppear)
                    awaitItem() // Loading
                    awaitItem() // Loaded

                    // Refresh
                    viewModel.onIntent(CurrentWeatherIntent.Refresh)
                    awaitItem() // Loading（再び）
                    awaitItem() // Loaded（再び）

                    coVerify(exactly = 2) { mockUseCase() }
                    cancelAndIgnoreRemainingEvents()
                }
            }

        @Test
        @DisplayName("Refresh は Loading 中でも実行される（強制リフレッシュ）")
        fun `Refresh executes even during Loading`() =
            runTest {
                // StandardTestDispatcher では suspend mock が即時返却するため
                // OnAppear の Job が Loading → Loaded まで一気に完走する。
                // State 遷移の順序はスケジューラ依存になるため、
                // 「UseCase が 2 回呼ばれた」という事実で OnAppear と Refresh の両方が
                // 実行されたことを検証する。
                coEvery { mockUseCase() } returns Result.success(makeWeather())

                viewModel.onIntent(CurrentWeatherIntent.OnAppear)
                viewModel.onIntent(CurrentWeatherIntent.Refresh)

                testDispatcher.scheduler.advanceUntilIdle()

                coVerify(exactly = 2) { mockUseCase() }
                assertTrue(viewModel.state.value.viewState is CurrentWeatherState.ViewState.Loaded)
            }
    }

    // ---- Retry Intent ----

    @Nested
    @DisplayName("Retry Intent")
    inner class RetryIntent {
        @Test
        @DisplayName("Retry は Error 状態から再ロードする")
        fun `Retry reloads from Error state`() =
            runTest {
                // 1回目：失敗、2回目：成功
                coEvery { mockUseCase() } returns
                    Result.failure(WeatherException(WeatherError.NetworkFailure("timeout"))) andThen
                    Result.success(makeWeather())

                viewModel.state.test {
                    awaitItem() // Idle

                    viewModel.onIntent(CurrentWeatherIntent.OnAppear)
                    awaitItem() // Loading
                    awaitItem() // Error（1回目失敗）

                    // Retry
                    viewModel.onIntent(CurrentWeatherIntent.Retry)
                    awaitItem() // Loading（再び）

                    val loaded = awaitItem().viewState
                    assertTrue(loaded is CurrentWeatherState.ViewState.Loaded)

                    cancelAndIgnoreRemainingEvents()
                }
            }
    }

    // ---- SideEffect（Channel）検証 ----
    // ここが MVVM テストとの最大の違い

    @Nested
    @DisplayName("SideEffect — Channel one-off 検証")
    inner class SideEffectTests {
        @Test
        @DisplayName("失敗時に ShowSnackbar SideEffect が 1 回だけ流れる")
        fun `failure emits ShowSnackbar SideEffect exactly once`() =
            runTest {
                // Channel の one-off 性を検証する
                // StateFlow なら画面回転後に再発火するが、Channel は一度しか流れない
                val expectedError = WeatherError.NetworkFailure("タイムアウト")
                coEvery { mockUseCase() } returns Result.failure(WeatherException(expectedError))

                // turbineScope で StateFlow と Channel を同時に収集する
                // iOS の Task グループで複数の非同期処理を並行実行するのに対応
                turbineScope {
                    val stateTurbine = viewModel.state.testIn(this)
                    val sideEffectTurbine = viewModel.sideEffect.testIn(this)

                    stateTurbine.awaitItem() // Idleを消費

                    viewModel.onIntent(CurrentWeatherIntent.OnAppear)

                    stateTurbine.awaitItem() // Loading
                    stateTurbine.awaitItem() // Error

                    val sideEffect = sideEffectTurbine.awaitItem()
                    assertTrue(sideEffect is CurrentWeatherSideEffect.ShowSnackbar)
                    assertEquals(
                        expectedError.userMessage,
                        (sideEffect as CurrentWeatherSideEffect.ShowSnackbar).message,
                    )

                    stateTurbine.cancelAndConsumeRemainingEvents()
                    sideEffectTurbine.cancelAndConsumeRemainingEvents()
                }
            }

        @Test
        @DisplayName("成功時は ShowSnackbar SideEffect が流れない")
        fun `success does not emit ShowSnackbar SideEffect`() =
            runTest {
                coEvery { mockUseCase() } returns Result.success(makeWeather())

                turbineScope {
                    val stateTurbine = viewModel.state.testIn(this)
                    val sideEffectTurbine = viewModel.sideEffect.testIn(this)

                    stateTurbine.awaitItem() // Idle

                    viewModel.onIntent(CurrentWeatherIntent.OnAppear)
                    stateTurbine.awaitItem() // Loading
                    stateTurbine.awaitItem() // Loaded

                    // 成功時は SideEffect が来ないことを確認
                    sideEffectTurbine.expectNoEvents()

                    stateTurbine.cancelAndIgnoreRemainingEvents()
                    sideEffectTurbine.cancelAndIgnoreRemainingEvents()
                }
            }

        @Test
        @DisplayName("2 回失敗すると ShowSnackbar が 2 回流れる")
        fun `two failures emit ShowSnackbar twice`() =
            runTest {
                coEvery { mockUseCase() } returns
                    Result.failure(WeatherException(WeatherError.NetworkFailure("1回目"))) andThen
                    Result.failure(WeatherException(WeatherError.NetworkFailure("2回目")))

                turbineScope {
                    val stateTurbine = viewModel.state.testIn(this)
                    val sideEffectTurbine = viewModel.sideEffect.testIn(this)

                    stateTurbine.awaitItem() // Idle

                    // 1回目
                    viewModel.onIntent(CurrentWeatherIntent.OnAppear)
                    stateTurbine.awaitItem() // Loading
                    stateTurbine.awaitItem() // Error

                    val first = sideEffectTurbine.awaitItem() as CurrentWeatherSideEffect.ShowSnackbar
                    assertTrue(first.message.contains("1回目"))

                    // 2回目（Retry）
                    viewModel.onIntent(CurrentWeatherIntent.Retry)
                    stateTurbine.awaitItem() // Loading
                    stateTurbine.awaitItem() // Error

                    val second = sideEffectTurbine.awaitItem() as CurrentWeatherSideEffect.ShowSnackbar
                    assertTrue(second.message.contains("2回目"))

                    stateTurbine.cancelAndIgnoreRemainingEvents()
                    sideEffectTurbine.cancelAndIgnoreRemainingEvents()
                }
            }
    }

    // ---- State の構造検証 ----

    @Nested
    @DisplayName("State 構造")
    inner class StateStructure {
        @Test
        @DisplayName("State は data class なので copy で部分更新できる")
        fun `state is data class and supports copy`() =
            runTest {
                // data class の copy が正しく動くことを単体で確認
                val initial = CurrentWeatherState()
                val loading = initial.copy(viewState = CurrentWeatherState.ViewState.Loading)

                assertTrue(initial.viewState is CurrentWeatherState.ViewState.Idle)
                assertTrue(loading.viewState is CurrentWeatherState.ViewState.Loading)

                // 元のインスタンスは変更されない（イミュータビリティの確認）
                assertTrue(initial.viewState is CurrentWeatherState.ViewState.Idle)
            }

        @Test
        @DisplayName("Loaded → Refresh → Loading の State 遷移で weather フィールドが引き継がれない")
        fun `refreshing clears weather from state`() =
            runTest {
                coEvery { mockUseCase() } returns Result.success(makeWeather(temperature = 99.0))

                viewModel.state.test {
                    awaitItem() // Idle

                    viewModel.onIntent(CurrentWeatherIntent.OnAppear)
                    awaitItem() // Loading
                    val loaded = awaitItem() // Loaded（99.0）
                    assertEquals(
                        99.0,
                        (loaded.viewState as CurrentWeatherState.ViewState.Loaded)
                            .weather.current.temperature,
                    )

                    // Refresh すると Loading に戻り weather は消える
                    viewModel.onIntent(CurrentWeatherIntent.Refresh)
                    val refreshLoading = awaitItem()
                    assertTrue(refreshLoading.viewState is CurrentWeatherState.ViewState.Loading)

                    cancelAndIgnoreRemainingEvents()
                }
            }
    }
}
