package com.doihei.weathernow.feature.weather.mvvm

import app.cash.turbine.test
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

// ViewModel のテストに必要な重要セットアップ：
// viewModelScope は内部で Dispatchers.Main を使う。
// テスト環境には Main ディスパッチャが存在しないため、
// TestDispatcher で差し替えないと IllegalStateException になる。
// iOS では @MainActor が自動でテスト対応されていたが、Android では明示的に設定が必要。
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("CurrentWeatherViewModel")
class CurrentWeatherViewModelTest {
    // TestDispatcher：仮想時間で動くコルーチンディスパッチャ
    // runTest の仮想時間と連動することで delay や非同期処理を即座に進められる
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockUseCase: GetWeatherUseCase
    private lateinit var viewModel: CurrentWeatherViewModel

    @BeforeEach
    fun setUp() {
        // Dispatchers.Main を TestDispatcher に差し替える
        // これがないと viewModelScope.launch が動かない
        Dispatchers.setMain(testDispatcher)
        mockUseCase = mockk()
        viewModel = CurrentWeatherViewModel(getWeatherUseCase = mockUseCase)
    }

    @AfterEach
    fun tearDown() {
        // テスト後に Main ディスパッチャを元に戻す
        // 戻さないと別テストに影響が出る
        Dispatchers.resetMain()
    }

    // ---- テスト用ドメインモデルファクトリ ----

    private fun makeWeather(temperature: Double = 20.0) =
        Weather(
            current =
                CurrentWeather(
                    temperature = temperature,
                    feelsLike = 10.0,
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
        @DisplayName("ViewModel 生成直後は Idle 状態である")
        fun `initial state is Idle`() =
            runTest {
                // iOS の #expect(viewModel.viewState == .idle) に対応
                // StateFlow は生成時の初期値を即座に emit するので awaitItem() で取得できる
                viewModel.viewState.test {
                    val initial = awaitItem()
                    assertTrue(initial is WeatherViewState.Idle)
                    cancelAndConsumeRemainingEvents()
                }
            }
    }

    // ---- load() 正常系 ----

    @Nested
    @DisplayName("load() — 正常系")
    inner class LoadSuccess {
        @Test
        @DisplayName("load() を呼ぶと Idle → Loading → Loaded の順で状態が遷移する")
        fun `load transitions through Idle to Loading to Loaded`() =
            runTest {
                // iOS の ViewState 遷移テストに対応
                // Turbine の awaitItem() が iOS の for await item in stream の 1ステップに相当
                val weather = makeWeather(temperature = 25.0)
                coEvery { mockUseCase() } returns Result.success(weather)

                viewModel.viewState.test {
                    // 初期値（Idle）を消費する
                    assertEquals(WeatherViewState.Idle, awaitItem())

                    // load() を呼んで遷移を開始する
                    viewModel.load()

                    // Loading が流れてくることを確認
                    assertEquals(WeatherViewState.Loading, awaitItem())

                    // Loaded が流れてくることを確認
                    val loaded = awaitItem()
                    assertTrue(loaded is WeatherViewState.Loaded)
                    assertEquals(25.0, (loaded as WeatherViewState.Loaded).weather.current.temperature)

                    cancelAndConsumeRemainingEvents()
                }
            }

        @Test
        @DisplayName("Loaded の weather は UseCase が返した値と一致する")
        fun `loaded weather matches use case result`() =
            runTest {
                val expected = makeWeather(temperature = 30.0)
                coEvery { mockUseCase() } returns Result.success(expected)

                viewModel.viewState.test {
                    awaitItem() // Idle を消費

                    viewModel.load()
                    awaitItem() // Loading を消費

                    val loaded = awaitItem() as WeatherViewState.Loaded
                    assertEquals(30.0, loaded.weather.current.temperature)
                    assertEquals(WeatherCode.CLEAR_SKY, loaded.weather.current.code)

                    cancelAndIgnoreRemainingEvents()
                }
            }
    }

    // ---- load() エラー系 ----

    @Nested
    @DisplayName("load() — エラー系")
    inner class LoadError {
        @Test
        @DisplayName("UseCase が失敗すると Loading → Error の順で遷移する")
        fun `load transitions to Error when use case fails`() =
            runTest {
                coEvery { mockUseCase() } returns
                    Result.failure(
                        WeatherException(WeatherError.NetworkFailure("timeout")),
                    )

                viewModel.viewState.test {
                    awaitItem() // Idle を消費

                    viewModel.load()
                    awaitItem() // Loading を消費

                    val error = awaitItem()
                    assertTrue(error is WeatherViewState.Error)

                    cancelAndIgnoreRemainingEvents()
                }
            }

        @Test
        @DisplayName("WeatherException のメッセージが Error.message に反映される")
        fun `WeatherException message appears in Error state`() =
            runTest {
                // iOS の error.localizedDescription → Error(message:) の対応を検証
                val expectedError = WeatherError.NetworkFailure("通信エラー")
                coEvery { mockUseCase() } returns
                    Result.failure(
                        WeatherException(expectedError),
                    )

                viewModel.viewState.test {
                    awaitItem() // Idle を消費

                    viewModel.load()
                    awaitItem() // Loading を消費

                    val error = awaitItem() as WeatherViewState.Error
                    // WeatherError.NetworkFailure の userMessage と一致することを確認
                    assertEquals(expectedError.userMessage, error.message)

                    cancelAndIgnoreRemainingEvents()
                }
            }

        @Test
        @DisplayName("LocationDenied エラーのメッセージが正しく伝わる")
        fun `LocationDenied error message propagates correctly`() =
            runTest {
                coEvery { mockUseCase() } returns
                    Result.failure(
                        WeatherException(WeatherError.LocationDenied),
                    )

                viewModel.viewState.test {
                    awaitItem() // Idle を消費

                    viewModel.load()
                    awaitItem() // Loading を消費

                    val error = awaitItem() as WeatherViewState.Error
                    assertEquals(WeatherError.LocationDenied.userMessage, error.message)

                    cancelAndIgnoreRemainingEvents()
                }
            }
    }

    // ---- 二重リクエスト防止 ----

    @Nested
    @DisplayName("二重リクエスト防止")
    inner class DoubleRequestPrevention {
        @Test
        @DisplayName("Loading 中に load() を呼んでも UseCase は 1 回しか呼ばれない")
        fun `calling load during loading does not trigger another request`() =
            runTest {
                // iOS の guard viewState != .loading テストに対応
                // 二重タップ等で load() が連打された場合の防御を検証する
                coEvery { mockUseCase() } returns Result.success(makeWeather())

                viewModel.viewState.test {
                    awaitItem() // Idle を消費

                    viewModel.load()
                    awaitItem() // Loading に遷移

                    // Loading 中にもう一度 load() を呼ぶ
                    viewModel.load()

                    // Loaded まで待つ
                    awaitItem()

                    // UseCase は 1 回だけ呼ばれたことを検証
                    coVerify(exactly = 1) { mockUseCase() }

                    cancelAndIgnoreRemainingEvents()
                }
            }
    }

    // ---- refresh() ----

    @Nested
    @DisplayName("refresh()")
    inner class Refresh {
        @Test
        @DisplayName("refresh() は Idle にリセットしてから再ロードする")
        fun `refresh resets to Idle then reloads`() =
            runTest {
                coEvery { mockUseCase() } returns Result.success(makeWeather())

                viewModel.viewState.test {
                    awaitItem() // Idle（初期）を消費

                    // 最初のロードを完了させる
                    viewModel.load()
                    awaitItem() // Loading
                    awaitItem() // Loaded

                    // refresh() でリセット → 再ロード
                    viewModel.refresh()

                    // Idle にリセットされてから Loading → Loaded の順で流れる
                    assertEquals(WeatherViewState.Idle, awaitItem())
                    assertEquals(WeatherViewState.Loading, awaitItem())
                    assertTrue(awaitItem() is WeatherViewState.Loaded)

                    // UseCase は合計 2 回呼ばれた（最初のロード + refresh）
                    coVerify(exactly = 2) { mockUseCase() }

                    cancelAndIgnoreRemainingEvents()
                }
            }
    }
}
