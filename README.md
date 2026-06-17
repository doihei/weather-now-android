# WeatherNow Android

[![CI](https://github.com/doihei/weather-now-android/actions/workflows/ci.yml/badge.svg)](https://github.com/doihei/weather-now-android/actions/workflows/ci.yml)

iOS 版 WeatherNow（MVVM + TCA / 6 モジュール）を **Kotlin + Jetpack Compose** でゼロから作り直すプロジェクト。

「書き直し」ではなく「同じ設計思想を別の語彙で表現する翻訳プロジェクト」。  
iOS の設計思想（レイヤード設計・DI・UDF・テスト戦略）を Kotlin/Compose の語彙でどう表現するかを探る。

---

## 技術スタック

| 領域 | 採用 |
|---|---|
| 言語 | Kotlin 2.3.21（K2 コンパイラ / KSP 2.3.9 との互換性制約） |
| ビルド | Gradle Kotlin DSL + version catalog + convention plugin |
| DI | Hilt（KSP） |
| 非同期 | Coroutines + Flow / StateFlow |
| ネットワーク | Retrofit + kotlinx.serialization |
| UI | Jetpack Compose 1.10.x |
| ナビゲーション | Navigation 3（1.1.2 stable） |
| グラフ | Vico |
| 位置情報 | FusedLocationProviderClient |
| テスト | JUnit + MockK + Turbine + Roborazzi |

---

## アーキテクチャ

### モジュール構成

```
:build-logic             # convention plugin（ビルド設定の共通化）
:app                     # アプリエントリポイント
:core:model              # Domain モデル（data class / sealed interface）
:core:network            # Retrofit API / DTO / Response→Domain 変換
:core:domain             # Repository / UseCase / LocationService
:feature:weather-mvvm    # MVVM 実装（ViewModel + StateFlow）
:feature:weather-mvi     # MVI 実装（State / Intent / SideEffect）
:core:ui                 # デザインシステム（Tokens / MaterialTheme）
```

依存方向は下向きのみ：`:feature` → `:core:domain` → `:core:model`

### MVVM と MVI の並存

`:core:domain` の上に MVVM と MVI の 2 実装を載せる。iOS 版の「MVVM vs TCA 比較実装」に対応。

**MVVM**（`@Observable` ViewModel の翻訳）:
```kotlin
@HiltViewModel
class CurrentWeatherViewModel @Inject constructor(
    private val repository: WeatherRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<ViewState>(ViewState.Idle)
    val state: StateFlow<ViewState> = _state.asStateFlow()
}
```

**MVI**（TCA State/Action/Effect の翻訳）:
```kotlin
data class CurrentWeatherState(val viewState: ViewState = ViewState.Idle)
sealed interface CurrentWeatherIntent { data object OnAppear : CurrentWeatherIntent }
sealed interface CurrentWeatherEffect { data class ShowError(val message: String) : CurrentWeatherEffect }
```

### Actor → Mutex

iOS の `actor` による排他保証は Kotlin では `Mutex` で手書きする。  
`withLock` の中で suspend（ネットワーク等）を呼ばないのが鉄則。

```kotlin
class DefaultWeatherRepository @Inject constructor(private val api: OpenMeteoApi) : WeatherRepository {
    private val mutex = Mutex()
    private val cache = mutableMapOf<String, CachedWeather>()

    override suspend fun fetchWeather(lat: Double, lon: Double): Weather {
        mutex.withLock { cache[cacheKey(lat, lon)]?.let { if (it.isValid()) return it.weather } }
        val weather = api.forecast(lat, lon).toWeather()   // ロックの外でネットワーク
        mutex.withLock { cache[cacheKey(lat, lon)] = CachedWeather(weather) }
        return weather
    }
}
```

---

## Phase ロードマップ

| Phase | 対象 | 内容 | 状態 |
|---|---|---|---|
| **0** | `:build-logic` + 基盤 | Gradle マルチモジュール / convention plugin / KSP / Hilt 初期配線 | 完了 |
| **1** | `:core:model` + `:core:network` | data class / sealed / Retrofit / DTO-Domain 分離 | 完了 |
| **2** | `:core:domain` | Repository / Mutex キャッシュ / suspend / Flow / WeatherError | 完了 |
| **3** | `:feature:weather-mvvm` | ViewModel / viewModelScope / StateFlow / ライフサイクル | 完了 |
| **4** | `:feature:weather-mvi` | reducer / Channel one-off イベント / 純粋状態遷移 | 完了 |
| **5** | 全画面 + `:app` | Compose UI / Navigation 3 / Vico グラフ / ダークモード | 進行中（Navigation 3 配線・app エントリポイント・基本 Screen・Vico グラフ（HourlyForecastChart）実装済み。ダークモード未着手） |
| **Ex** | `:core:ui` | デザイントークン / MaterialTheme カスタム / strings.xml | 進行中（コンポーネント・テーマ・strings.xml・カラー／スペーシング／サイズトークン実装済み。タイポグラフィトークン未着手） |

---

## iOS → Android 設計マッピング

| iOS | Android |
|---|---|
| `struct` + `Equatable` | `data class` |
| `enum` with associated values | `sealed class` / `sealed interface` |
| `actor` | `Mutex` + `withLock` |
| `@Observable` ViewModel | `ViewModel` + `StateFlow` |
| TCA `State/Action/Effect` | MVI `State/Intent/SideEffect` |
| TCA `StackState` | Navigation 3 バックスタックリスト |
| `@Dependency` / `liveValue` | Hilt `@Binds` / `@TestInstallIn` |
| `TestClock.advance(by:)` | `runTest` + `advanceTimeBy()` |

---

## セットアップ

```bash
# リポジトリをクローン
git clone <repo-url>
cd weather-now-android

# ビルド（全モジュール）
make build

# テスト
make test

# よく使うコマンド一覧
make help
```

`make` が使えない場合は `./gradlew build` / `./gradlew test` でも同等。

**前提条件**
- Android Studio（Compose 1.10 / Nav3 対応版）
- JDK 17 以上

---

## ライセンス

MIT
