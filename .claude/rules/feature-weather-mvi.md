---
paths:
  - "feature/weather-mvi/**"
---

# :feature:weather-mvi 層のルール

## パッケージ・ディレクトリ構造

```
feature/weather-mvi/src/main/kotlin/.../feature/weather/mvi/
└── currentweather/                          # 画面単位のサブパッケージ（画面追加時もここに並べる）
    ├── CurrentWeatherMviScreen.kt           # @Composable Screen（UI のみ。Intent 経由でのみ ViewModel に指示）
    ├── CurrentWeatherMviViewModel.kt        # @HiltViewModel
    ├── CurrentWeatherState.kt               # data class（ViewState をネストした sealed interface で持つ）
    ├── CurrentWeatherIntent.kt              # sealed interface（ユーザー意図のみ。内部イベントは含めない）
    └── CurrentWeatherSideEffect.kt          # sealed interface（Channel で流す一度きりのイベント）

feature/weather-mvi/src/main/res/
└── values/
    └── strings.xml                          # 画面固有の文字列（screen_title_weather_mvi 等）

feature/weather-mvi/src/test/kotlin/.../feature/weather/mvi/
└── currentweather/                          # main 側と同一サブパッケージ構造
    └── CurrentWeatherMviViewModelTest.kt
```

**サブパッケージのルール：**
- 画面を追加するときは `mvi/<screen名>/` サブパッケージを切る（フラットに並べない）
- State・Intent・SideEffect は必ずそれぞれ独立したファイルに定義する（1 ファイルにまとめない）

## 状態設計

`CurrentWeatherState` は `data class`、内側の `ViewState` は `sealed interface` のネストで定義する。

```kotlin
data class CurrentWeatherState(
    val viewState: ViewState = ViewState.Idle,
) {
    sealed interface ViewState {
        data object Idle    : ViewState
        data object Loading : ViewState
        data class Loaded(val weather: Weather) : ViewState
        data class Error(val message: String)   : ViewState
    }
}
```

- MVVM の `WeatherViewState` と同じ 4 状態だが、MVI では `State` のネストとして定義する
- iOS の TCA `State` に対応。`data class` の `copy()` で不変更新する

## Intent 設計

`CurrentWeatherIntent` は「ユーザーが起こせる意図」のみを定義する `sealed interface`。

```kotlin
sealed interface CurrentWeatherIntent {
    data object OnAppear : CurrentWeatherIntent
    data object Refresh  : CurrentWeatherIntent
    data object Retry    : CurrentWeatherIntent
}
```

- TCA の Action は内部イベント（API 応答）も含むが、自前 MVI ではユーザー意図のみを Intent とする
- 内部イベント（API 応答等）は ViewModel 内で直接処理する

## SideEffect 設計

「一度きりの出来事」は `Channel` で流す。`StateFlow` に入れると画面回転で再発火するバグになる。

```kotlin
sealed interface CurrentWeatherSideEffect {
    data class ShowSnackbar(val message: String) : CurrentWeatherSideEffect
}

// ViewModel 内
private val _sideEffect = Channel<CurrentWeatherSideEffect>(Channel.BUFFERED)
val sideEffect = _sideEffect.receiveAsFlow()
```

**State（StateFlow）vs SideEffect（Channel）の判断基準：**

| 問い | YES なら |
|---|---|
| 今の画面状態として持ち続けるべきか？ | `State`（StateFlow） |
| 一度だけ通知して終わりか？ | `SideEffect`（Channel） |

例：エラー表示を画面に持ち続ける → `State.Error`、スナックバーを一時表示 → `SideEffect.ShowSnackbar`

## ViewModel 設計

### 基本パターン

```kotlin
@HiltViewModel
class CurrentWeatherMviViewModel @Inject constructor(
    private val getWeatherUseCase: GetWeatherUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(CurrentWeatherState())
    val state: StateFlow<CurrentWeatherState> = _state.asStateFlow()

    private val _sideEffect = Channel<CurrentWeatherSideEffect>(Channel.BUFFERED)
    val sideEffect = _sideEffect.receiveAsFlow()
}
```

### Intent の振り分け

```kotlin
fun onIntent(intent: CurrentWeatherIntent) {
    when (intent) {
        is CurrentWeatherIntent.OnAppear -> { ... }
        is CurrentWeatherIntent.Refresh  -> { ... }
        is CurrentWeatherIntent.Retry    -> { ... }
    }
}
```

- iOS の `Store.send(_ action:)` に対応
- TCA の reducer が全 Action を `switch` で処理するのと同じ構造

### OnAppear の二重ロード防止

```kotlin
is CurrentWeatherIntent.OnAppear -> {
    val vs = _state.value.viewState
    if (vs is CurrentWeatherState.ViewState.Loading || vs is CurrentWeatherState.ViewState.Loaded) return
    loadWeather()
}
```

- Loading 中は二重発火しない
- 既に Loaded なら再ロードしない（OnAppear を複数回受け取っても安全）
- **MVVM の `load()` と違い、Loaded も早期 return の対象**

### Refresh は常に実行

```kotlin
is CurrentWeatherIntent.Refresh -> loadWeather()
```

- ユーザーが明示的に引っ張った場合は Loading 中でも強制リフレッシュ
- MVVM の `refresh()` が Idle にリセットしてから `load()` するのと同等の意図

## 依存宣言のルール

`:core:domain` は `:core:model` を `implementation` で持つため自動伝播しない。

```kotlin
dependencies {
    implementation(project(":core:model"))   // Weather 型を直接参照するため必要
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))
    testImplementation(libs.bundles.test.unit)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

## テスト設計

MVVM のテスト設計（`.claude/rules/feature-weather-mvvm.md`）を基本として、MVI 固有の差分を記す。

### state と sideEffect の同時検証

`sideEffect`（Channel）を `state`（StateFlow）と同時に収集するには `turbineScope` + `testIn` を使う。

```kotlin
turbineScope {
    val stateTurbine = viewModel.state.testIn(this)
    val sideEffectTurbine = viewModel.sideEffect.testIn(this)

    stateTurbine.awaitItem() // Idle を消費
    viewModel.onIntent(CurrentWeatherIntent.OnAppear)
    stateTurbine.awaitItem() // Loading
    stateTurbine.awaitItem() // Error

    val sideEffect = sideEffectTurbine.awaitItem()
    assertTrue(sideEffect is CurrentWeatherSideEffect.ShowSnackbar)

    stateTurbine.cancelAndConsumeRemainingEvents()
    sideEffectTurbine.cancelAndConsumeRemainingEvents()
}
```

- `sideEffect` のみを単独検証する場合は `sideEffect.test { }` で良い
- `state` と `sideEffect` を同一テストで検証する場合は `turbineScope` を使う（一方が他方をブロックしないため）

### SideEffect のメッセージ検証

`ShowSnackbar.message` には `WeatherError.userMessage` が入る。
`NetworkFailure.message`（生の文字列）ではなく `userMessage`（`"通信エラー: ..."` 形式）で比較すること。

```kotlin
// 正
assertEquals(expectedError.userMessage, sideEffect.message)

// 誤（NetworkFailure の val message フィールドは生文字列なので一致しない）
assertEquals(expectedError.message, sideEffect.message)
```

### StandardTestDispatcher と並行 Intent のテスト方針

`StandardTestDispatcher` では suspend mock が即時返却するため、`OnAppear` の Job が
Loading → Loaded まで一気に完走する。そのため「Loading 中に Refresh」のような
State 遷移の順序に依存したテストは書けない。

代わりに **`advanceUntilIdle()` + `coVerify` で UseCase の呼び出し回数を検証**する。

```kotlin
// 「Refresh は Loading 中でも実行される」の検証
viewModel.onIntent(CurrentWeatherIntent.OnAppear)
viewModel.onIntent(CurrentWeatherIntent.Refresh)

testDispatcher.scheduler.advanceUntilIdle()

coVerify(exactly = 2) { mockUseCase() }  // 両方が UseCase を呼んだ
assertTrue(viewModel.state.value.viewState is CurrentWeatherState.ViewState.Loaded)
```

### OnAppear の二重ロード防止テスト

`Loading` と `Loaded` の両方から `OnAppear` を送っても UseCase が呼ばれないことを確認する
（MVVM の `load()` は Loading のみガード。MVI の `OnAppear` は Loaded もガード対象）。
