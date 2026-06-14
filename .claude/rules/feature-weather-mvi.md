---
paths:
  - "feature/weather-mvi/**"
---

# :feature:weather-mvi 層のルール

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
    data class ShowSnackBar(val message: String) : CurrentWeatherSideEffect
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

例：エラー表示を画面に持ち続ける → `State.Error`、スナックバーを一時表示 → `SideEffect.ShowSnackBar`

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

- `state` は Turbine で検証（`state.test { ... }`）
- `sideEffect` も Turbine で検証（`sideEffect.test { ... }`）
- `onIntent()` で Intent を送り、`state` と `sideEffect` の両方を確認する
- `OnAppear` の二重ロード防止テストでは `Loaded` 状態から呼び出しても UseCase が呼ばれないことを確認する
