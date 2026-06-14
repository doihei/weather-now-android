---
paths:
  - "feature/weather-mvvm/**"
---

# :feature:weather-mvvm 層のルール

## ビューステート設計

`WeatherViewState` は `sealed interface` で定義し、4 つの状態を持つ。

```kotlin
sealed interface WeatherViewState {
    data object Idle    : WeatherViewState
    data object Loading : WeatherViewState
    data class Loaded(val weather: Weather) : WeatherViewState
    data class Error(val message: String)   : WeatherViewState
}
```

- iOS の `enum ViewState` に対応。`when` の網羅チェックが `sealed interface` で効く
- `Error` は `message: String` だけを持つ。UI 層は `WeatherError` の詳細を知らなくてよい

## ViewModel の設計

### 基本パターン

```kotlin
@HiltViewModel
class CurrentWeatherViewModel @Inject constructor(
    private val getWeatherUseCase: GetWeatherUseCase,
) : ViewModel() {
    private val _viewState = MutableStateFlow<WeatherViewState>(WeatherViewState.Idle)
    val viewState: StateFlow<WeatherViewState> = _viewState.asStateFlow()
}
```

- `@HiltViewModel` + `@Inject constructor`：Hilt が UseCase を注入する
- `MutableStateFlow` は `private`、外部には `asStateFlow()` で読み取り専用を公開
- iOS の `private(set) var viewState` に対応する可視性の分離

### 二重ロード防止

```kotlin
fun load() {
    if (_viewState.value is WeatherViewState.Loading) return
    // ...
}
```

- Loading 中に再度 `load()` が呼ばれても無視する
- iOS の `guard viewState != .loading else { return }` に対応

### 成功/失敗の分岐は `Result.fold`

```kotlin
val result = getWeatherUseCase()
_viewState.value = result.fold(
    onSuccess = { weather ->
        WeatherViewState.Loaded(weather)
    },
    onFailure = { throwable ->
        val message = (throwable as? WeatherException)?.error?.userMessage
            ?: throwable.message
            ?: "不明なエラーが発生しました"
        WeatherViewState.Error(message)
    },
)
```

- UseCase は `Result<T>` を返す（例外を投げない）
- `WeatherException` から `WeatherError.userMessage` を取り出してユーザー向けメッセージに変換

### `refresh()` は Idle にリセットしてから `load()`

```kotlin
fun refresh() {
    _viewState.value = WeatherViewState.Idle
    load()
}
```

- Loading 中でも強制リフレッシュできる
- Idle にリセットすることで `load()` の二重ロード防止チェックを回避する

## 依存宣言のルール

`:core:domain` は `:core:model` を `implementation` で持つため、自動伝播しない。
`Weather` 型を直接参照する場合は `build.gradle.kts` に明示する。

```kotlin
dependencies {
    implementation(project(":core:model"))   // Weather 型を直接参照するため必要
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))
}
```

## テスト設計

- `viewModelScope` の制御は `runTest` + `StandardTestDispatcher` を使う
- `Dispatchers.setMain(testDispatcher)` を `@BeforeEach` で設定し、`@AfterEach` で `resetMain()` する
  （iOS の `@MainActor` 自動対応と違い、Android では明示的な差し替えが必要）
- `viewState` の遷移検証は **Turbine** を使う
- UseCase は **MockK**（`mockk<GetWeatherUseCase>()` + `coEvery`）でモックする
- `@TestInstallIn` は不要：コンストラクタに mockk を直接渡す
- テストケースは `@Nested` + `@DisplayName` でグループ化する

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("CurrentWeatherViewModel")
class CurrentWeatherViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockUseCase: GetWeatherUseCase
    private lateinit var viewModel: CurrentWeatherViewModel

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockUseCase = mockk()
        viewModel = CurrentWeatherViewModel(getWeatherUseCase = mockUseCase)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Nested
    @DisplayName("load() — 正常系")
    inner class LoadSuccess {
        @Test
        @DisplayName("load() を呼ぶと Idle → Loading → Loaded の順で遷移する")
        fun `load transitions through Idle to Loading to Loaded`() = runTest {
            coEvery { mockUseCase() } returns Result.success(makeWeather())
            viewModel.viewState.test {
                assertEquals(WeatherViewState.Idle, awaitItem())
                viewModel.load()
                assertEquals(WeatherViewState.Loading, awaitItem())
                assertTrue(awaitItem() is WeatherViewState.Loaded)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("load() — エラー系")
    inner class LoadError {
        @Test
        @DisplayName("UseCase が失敗すると Loading → Error に遷移する")
        fun `load transitions to Error on failure`() = runTest {
            coEvery { mockUseCase() } returns
                Result.failure(WeatherException(WeatherError.NetworkFailure("timeout")))
            viewModel.viewState.test {
                assertEquals(WeatherViewState.Idle, awaitItem())
                viewModel.load()
                assertEquals(WeatherViewState.Loading, awaitItem())
                assertTrue(awaitItem() is WeatherViewState.Error)
                cancelAndIgnoreRemainingEvents()
            }
        }
    }

    @Nested
    @DisplayName("二重リクエスト防止")
    inner class DoubleRequestPrevention {
        @Test
        @DisplayName("Loading 中に load() を呼んでも UseCase は 1 回しか呼ばれない")
        fun `calling load during loading does not trigger another request`() = runTest {
            coEvery { mockUseCase() } returns Result.success(makeWeather())
            viewModel.viewState.test {
                awaitItem() // Idle を消費
                viewModel.load()
                awaitItem() // Loading に遷移
                viewModel.load() // 二重呼び出し
                awaitItem() // Loaded まで待つ
                coVerify(exactly = 1) { mockUseCase() }
                cancelAndIgnoreRemainingEvents()
            }
        }
    }
}
```
