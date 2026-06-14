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
- `viewState` の遷移検証は **Turbine** を使う
- UseCase は `FakeGetWeatherUseCase`（コンストラクタに `Result<Weather>` を渡す）でモックする
- `@TestInstallIn` は不要：ユニットテストはコンストラクタに fake を直接渡す

```kotlin
@DisplayName("CurrentWeatherViewModel")
class CurrentWeatherViewModelTest {

    @Test
    @DisplayName("load() を呼ぶと Loading → Loaded に遷移する")
    fun `load transitions to Loaded on success`() = runTest {
        val viewModel = CurrentWeatherViewModel(
            getWeatherUseCase = FakeGetWeatherUseCase(Result.success(makeWeather())),
        )
        viewModel.viewState.test {
            assertEquals(WeatherViewState.Idle, awaitItem())
            viewModel.load()
            assertEquals(WeatherViewState.Loading, awaitItem())
            assertEquals(WeatherViewState.Loaded(makeWeather()), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("load() を呼ぶと Loading → Error に遷移する")
    fun `load transitions to Error on failure`() = runTest {
        val viewModel = CurrentWeatherViewModel(
            getWeatherUseCase = FakeGetWeatherUseCase(
                Result.failure(WeatherException(WeatherError.NetworkFailure("timeout"))),
            ),
        )
        viewModel.viewState.test {
            assertEquals(WeatherViewState.Idle, awaitItem())
            viewModel.load()
            assertEquals(WeatherViewState.Loading, awaitItem())
            val error = awaitItem() as WeatherViewState.Error
            assertTrue(error.message.isNotEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    @DisplayName("Loading 中に load() を呼んでも二重リクエストにならない")
    fun `load is idempotent while Loading`() = runTest {
        // ...
    }
}
```
