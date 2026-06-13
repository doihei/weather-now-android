# WeatherNow Android 移植ロードマップ（読み物）

iOS 版 WeatherNow（MVVM + TCA / 6 モジュール）を、Kotlin + Jetpack Compose でゼロから作り直すための全体像。
「書き直し」ではなく「同じ設計思想を別の語彙で表現する」プロジェクトとして捉える。

---

## 0. この移植で本当に学ぶもの

WeatherNow は「天気アプリを作る」ことが目的ではない。iOS で学んだのは次の 6 つだった。

- レイヤード設計（依存は下向きのみ）
- 境界を Protocol で切って DI で差し替える
- 副作用を Actor / Service に隔離する
- 状態を一方向に流す（特に TCA）
- テストダブルでビジネスロジックを検証する
- デザインシステムで UI の一貫性を担保する

これらは**言語非依存の設計原則**である。Android 移植で新しく覚えるのは「この原則を Kotlin/Compose の語彙でどう書くか」だけ。つまり学習対象は概念ではなく**翻訳**である。

唯一、概念レベルで新しく向き合うべきなのは 1 点だけ：**Swift の Actor に相当するものが Kotlin にはない**。ここだけは「翻訳」では済まず、Kotlin の構造化並行性（structured concurrency）という別のモデルを理解する必要がある。後述する。

---

## 1. 技術スタック（2026 年 6 月時点・確定版）

| 領域 | 採用 | 理由・iOS 版での対応 |
|---|---|---|
| 言語 | Kotlin 2.x（K2 コンパイラ） | Swift に対応。K2 で型推論・ビルドが安定 |
| ビルド | Gradle（Kotlin DSL）+ version catalog + convention plugin | SPM の `Package.swift`（path: 明示）に対応 |
| アノテーション処理 | KSP（Kapt は使わない） | Kapt 比でビルド 2 倍速。Hilt/Room はすべて KSP 対応済み |
| DI | Hilt（Google 公式推奨・Dagger ベース） | swift-dependencies に対応 |
| 非同期 | Coroutines + Flow / StateFlow | async/await + AsyncStream に対応 |
| ネットワーク | Retrofit + kotlinx.serialization | APIClient + Decodable に対応 |
| UI | Jetpack Compose 1.10.x | SwiftUI に対応 |
| ナビゲーション | **Navigation 3（1.1.2 stable / 2025-11）** | NavigationStack・TCA の StackState に対応 |
| グラフ | Vico（Compose charts） | Swift Charts に対応 |
| 位置情報 | FusedLocationProviderClient（Play Services Location） | CLLocationManager に対応 |
| ローカライズ | strings.xml + `stringResource()` | Localizable.xcstrings + L10n に対応 |
| テスト | JUnit + MockK + Turbine + `runTest` + Roborazzi（スクショ） | Swift Testing + withDependencies に対応 |

補足が 3 点ある。

**Navigation 3 が stable 化したのは大きい。** Nav2（従来の Navigation Compose）は 7 年前に Fragment 時代の発想で作られたもので、文字列ルートやバンドル引数、ライブラリ内部に隠れたバックスタックが Compose と噛み合っていなかった。Nav3 は**バックスタックを開発者が所有する状態（リスト）として持ち、NavDisplay がそのリストを監視して UI を更新する**設計に変わった。これは TCA の `RootFeature` で書く `StackState<WeatherPath.State>` とまったく同じ思想である。ただし API がまだ新しいので、Phase 5 で最新ドキュメントを確認しながら進める前提にしておく。

**Hilt か Koin か。** 2026 年でも Android ネイティブの公式推奨は Hilt。Koin は軽量で KMP（Kotlin Multiplatform）向きと言われるが、今回は Android ネイティブに集中するので Hilt で行く。将来 KMP を検討する局面が来たら Koin を再評価すればよい。

**KSP 一択。** 旧 Kapt はビルドが遅い。Hilt も Room も KSP で動かす。iOS 版でやった「ビルド最適化」の Android 版の論点だと思えばよい。

---

## 2. 設計思想の対応マッピング（詳細）

### 2.1 モデル層：`struct` → `data class`

iOS の不変値型は、ほぼそのまま `data class` になる。

```swift
// iOS: CoreModels/Weather/CurrentWeather.swift
public struct CurrentWeather: Sendable, Equatable {
    public let temperature: Double
    public let feelsLike: Double
    public let humidity: Int
    public let windSpeed: Double
    public let code: WeatherCode
}
```

```kotlin
// Android: core/model/CurrentWeather.kt
data class CurrentWeather(
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val windSpeed: Double,
    val code: WeatherCode,
)
```

`data class` は `equals`/`hashCode`/`copy`/`toString` を自動生成する。`Sendable` に相当する明示マークは不要（不変 `val` だけで構成すればスレッド間共有は安全）。`Equatable` は `data class` が自動でカバーする。

### 2.2 列挙とエラー：`enum` → `enum class` / `sealed class`

単純な列挙は `enum class`。

```kotlin
enum class WeatherCode(val wmoCode: Int) {
    CLEAR_SKY(0), MAINLY_CLEAR(1), OVERCAST(3), LIGHT_RAIN(61),
    HEAVY_RAIN(65), THUNDERSTORM(95), UNKNOWN(-1);

    companion object {
        fun from(code: Int): WeatherCode =
            entries.firstOrNull { it.wmoCode == code } ?: UNKNOWN
    }

    // 表示文字列は :core:ui の WeatherCode.labelResId (@StringRes) に移管
    // Compose 側: stringResource(weatherCode.labelResId)
}
```

一方、`WeatherError` のように **case ごとに付随値を持つ**ものは `sealed class`（または `sealed interface`）で書く。ここは Swift の associated value enum よりむしろ表現力が高い。

```swift
// iOS
public enum WeatherError: Error, Sendable, Equatable {
    case locationDenied
    case networkFailure(String)
    case decodingFailure
    case cityLimitReached
}
```

```kotlin
// Android
sealed interface WeatherError {
    data object LocationDenied : WeatherError
    data object LocationUnavailable : WeatherError
    data class NetworkFailure(val message: String) : WeatherError
    data object DecodingFailure : WeatherError
    data object CityLimitReached : WeatherError

    val userMessage: String
        get() = when (this) {
            LocationDenied -> "位置情報の使用が許可されていません。設定アプリから許可してください。"
            is NetworkFailure -> "通信エラー: $message"
            // ...
        }

    val isRetryable: Boolean
        get() = when (this) {
            LocationUnavailable, is NetworkFailure -> true
            else -> false
        }
}
```

`when` は `sealed` に対して網羅性チェックが効くので、iOS の `switch` の「全 case を書かないとコンパイルエラー」と同じ安全性が得られる。`Throwable` を継承させるかは設計判断で、TCA 的に `Result<T, WeatherError>` で持ち回るなら継承不要。

### 2.3 ネットワーク層：APIClient + Endpoint + Response 変換

iOS の `OpenMeteoEndpoint`（URL・クエリを enum に集約）と `ForecastResponse.toWeather()`（Response→Domain 変換）はそのまま移植できる。

```kotlin
// 固定クエリ定数（Retrofit interface はデフォルト引数を @Query と組み合わせできないため切り出す）
object OpenMeteoQueryDefaults {
    const val CURRENT = "temperature_2m,apparent_temperature,relativehumidity_2m,weathercode,windspeed_10m"
    const val HOURLY  = "temperature_2m,precipitation,weathercode"
    const val DAILY   = "temperature_2m_max,temperature_2m_min,precipitation_probability_max,weathercode"
    const val TIMEZONE = "auto"
    const val FORECAST_DAYS = 7
}

// Retrofit インターフェース
interface OpenMeteoApi {
    @Suppress("LongParameterList") // API仕様由来の引数数のため抑制
    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude")     latitude: Double,
        @Query("longitude")    longitude: Double,
        @Query("current")      current: String,      // OpenMeteoQueryDefaults.CURRENT を渡す
        @Query("hourly")       hourly: String,
        @Query("daily")        daily: String,
        @Query("timezone")     timezone: String,
        @Query("forecast_days") forecastDays: Int,
    ): ForecastResponseDto
}
```

```kotlin
// レスポンス DTO（kotlinx.serialization）
@Serializable
data class ForecastResponseDto(
    val current: ForecastCurrentDto,
    val hourly: ForecastHourlyDto,
    val daily: ForecastDailyDto,
)

@Serializable
data class ForecastCurrentDto(
    @SerialName("temperature_2m") val temperature2m: Double,
    @SerialName("apparent_temperature") val apparentTemperature: Double,
    // ...
)

// DTO → Domain 変換（extension 関数。iOS の toWeather() に対応）
fun ForecastResponseDto.toWeather(): Weather { /* ... */ }
```

設計上のポイントが 2 点ある。

**固定クエリの切り出し。** Retrofit の `interface` はデフォルト引数を `@Query` と組み合わせできない（Kotlin の interface 仕様上）。iOS の `OpenMeteoEndpoint` enum が固定クエリを列挙していたのと同様に、Android では `OpenMeteoQueryDefaults` object に定数として切り出し、リポジトリ層から渡す。

**DTO と Domain モデルの分離。** iOS の規約「クライアント実装にベース URL を直書きしない」は Android では Retrofit の `baseUrl` を Hilt モジュールで一元注入することで守る。DTO（`@Serializable`）と Domain モデル（`data class`）を分け、`mapper/` の extension 関数で橋渡しする——これは iOS の `Responses/` と `CoreModels/` の分離と同じ。

### 2.4 DI：swift-dependencies → Hilt

iOS の `@Dependency(\.weatherRepository)` と `liveValue`/`testValue` の差し替えは、Hilt の `@Inject` + モジュール差し替えに対応する。

```kotlin
// 本番の束縛
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    abstract fun bindWeatherRepository(impl: DefaultWeatherRepository): WeatherRepository
}

// 使う側（コンストラクタ注入）
class GetWeatherUseCase @Inject constructor(
    private val repository: WeatherRepository,
)
```

テストでは `@TestInstallIn` で本番モジュールを丸ごと差し替える。iOS の `withDependencies { $0.weatherRepository = StubWeatherRepository() }` に相当する。重要なのは思想が同じこと：**実装ではなくインターフェース（Protocol → interface）に依存し、注入点で差し替える**。

### 2.5 状態管理：@Observable → ViewModel + StateFlow / @Reducer → MVI

iOS 版の白眉だった「MVVM と TCA の比較実装」を Android でも踏襲する。同じ `:core:domain` の上に 2 つの UI 実装を載せる。

MVVM 側（`@Observable` ViewModel に対応）：

```kotlin
@HiltViewModel
class CurrentWeatherViewModel @Inject constructor(
    private val repository: WeatherRepository,
    private val locationService: LocationService,
) : ViewModel() {
    private val _state = MutableStateFlow<ViewState>(ViewState.Idle)
    val state: StateFlow<ViewState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _state.value = ViewState.Loading
            runCatching {
                val loc = locationService.currentLocation()
                repository.fetchWeather(loc.lat, loc.lon)
            }.onSuccess { _state.value = ViewState.Loaded(it) }
             .onFailure { _state.value = ViewState.Error(it.toWeatherError()) }
        }
    }
}
```

MVI 側（TCA に対応）：TCA のような巨大フレームワークは使わず、`State` / `Intent` / `SideEffect` を自前で組む素朴な UDF が 2026 年の主流。`reduce` の本質だけ取り出す。

```kotlin
data class CurrentWeatherState(
    val viewState: ViewState = ViewState.Idle,
    val cityName: String = "",
)

sealed interface CurrentWeatherIntent {
    data object OnAppear : CurrentWeatherIntent
    data object Refresh : CurrentWeatherIntent
}

sealed interface CurrentWeatherEffect {
    data class ShowError(val message: String) : CurrentWeatherEffect
}

@HiltViewModel
class CurrentWeatherMviViewModel @Inject constructor(/* deps */) : ViewModel() {
    private val _state = MutableStateFlow(CurrentWeatherState())
    val state = _state.asStateFlow()
    private val _effect = Channel<CurrentWeatherEffect>()
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: CurrentWeatherIntent) {
        when (intent) {
            is CurrentWeatherIntent.OnAppear -> { /* reduce + 副作用 */ }
            is CurrentWeatherIntent.Refresh -> { /* ... */ }
        }
    }
}
```

対応関係を頭に入れておく：

- TCA `State` → MVI の `data class State`
- TCA `Action` → MVI の `sealed interface Intent`
- TCA `Effect` / `.run { }` → `viewModelScope.launch` + `Channel`（one-off の副作用）
- TCA の delegate / 親子通知 → `SideEffect` を上位に流す or 共有 ViewModel

違いは、TCA は `reduce` が純粋関数で副作用を `Effect` に分離するのを**強制**するのに対し、自前 MVI はその規律を**自分で守る**点。だからこそ「なぜ TCA はあそこまで作り込まれているか」が逆向きに理解できる。

### 2.6 UI：SwiftUI → Compose

宣言的 UI 同士なので発想は同じ。状態を受け取って描画する。

```kotlin
@Composable
fun CurrentWeatherScreen(viewModel: CurrentWeatherViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val s = state) {
        ViewState.Idle, ViewState.Loading -> LoadingView()
        is ViewState.Loaded -> LoadedView(s.weather)
        is ViewState.Error -> ErrorView(s.error)
    }
}
```

`collectAsStateWithLifecycle()` は SwiftUI の `@State`/`@Observable` 監視に対応するが、**ライフサイクルを意識して購読する**点が Android 固有（バックグラウンドで無駄に流さない）。ここは iOS にはない Android のクセなので Phase 3 で丁寧に扱う。

---

## 3. Phase 別ロードマップ

iOS 版の Phase 構成にそのまま対応させる。各 Phase は「目的 / 学ぶ概念 / 主要コード / iOS 対応 / 落とし穴」で構成。

### Phase 0：プロジェクト基盤

- 目的：依存方向を構造的に強制する土台を作る
- 学ぶ概念：Gradle マルチモジュール、version catalog、convention plugin、KSP、Hilt 配線
- 主要：`settings.gradle.kts` でモジュール列挙、`libs.versions.toml` で依存バージョン一元管理、`:build-logic` で `build.gradle.kts` の重複排除
- iOS 対応：`Package.swift` 群（path: 明示）→ Gradle モジュールグラフ
- 落とし穴：最初に convention plugin を作らないと、モジュールが増えるたびに `build.gradle.kts` がコピペ地獄になる。大規模チーム開発では特に重要。ここを Phase 0 でやっておく

### Phase 1：`:core:model` + `:core:network`

- 目的：データの形と外界との接点を確定する
- 学ぶ概念：`data class`、`sealed`、kotlinx.serialization、Retrofit、DTO/Domain 分離
- iOS 対応：`CoreModels` + `CoreNetwork`
- 落とし穴：`@Serializable` の DTO を UI まで持ち回らないこと。必ず Domain モデルに変換して境界を切る（iOS で `Responses/` を分けたのと同じ）

### Phase 2：`:core:domain`

- 目的：ビジネスロジックと副作用を隔離する
- 学ぶ概念：Repository、`Mutex` キャッシュ、`suspend`、`Flow`、`WeatherError` sealed
- iOS 対応：`WeatherDomain`（Repository は Actor、LocationService も Actor）
- 落とし穴：**ここが本移植の最重要かつ最難関**。iOS の Actor を Kotlin の `Mutex` でどう安全に置き換えるか。第 4 章で深掘りする

### Phase 3：MVVM 実装（`:feature:weather-mvvm`）

- 目的：素直な単方向データフローを ViewModel + StateFlow で組む
- 学ぶ概念：`ViewModel`、`viewModelScope`、`StateFlow`、`collectAsStateWithLifecycle`
- iOS 対応：`@Observable` ViewModel 群
- 落とし穴：`viewModelScope` のキャンセル挙動と、画面回転（configuration change）をまたぐ状態保持。iOS にはない Android 固有の罠

### Phase 4：MVI 実装（`:feature:weather-mvi`）

- 目的：State/Intent/SideEffect の UDF を自前で組む
- 学ぶ概念：reducer 思想、`Channel` による one-off イベント、純粋な状態遷移
- iOS 対応：TCA Features
- 落とし穴：`StateFlow`（状態）と `Channel`/`SharedFlow`（一度きりのイベント）を混同しないこと。エラー表示やナビゲーションは「状態」ではなく「イベント」

### Phase 5：Compose UI 全画面 + Navigation 3 + Vico + ダークモード

- 目的：見える形にする。MVVM/MVI 両方の UI を載せる
- 学ぶ概念：Composable 設計、Navigation 3（バックスタック所有）、Vico、`isSystemInDarkTheme()`
- iOS 対応：Phase 5（全 View・Swift Charts・ダークモード）
- 落とし穴：Nav3 は API が新しい。最新ドキュメントを確認しながら、TCA の StackState の経験を頼りに進む

### Phase Ex：デザインシステム

- 目的：UI の一貫性とローカライズ
- 学ぶ概念：Compose の design tokens（`Spacing`/`Size` を `object` で定義）、`MaterialTheme` のカスタム、strings.xml
- iOS 対応：Phase Ex1（Design Tokens・AppSymbol・L10n 生成）
- 落とし穴：iOS の `AppSymbol`（SFSafeSymbols 型安全）に当たるものは、Android では Material Icons or 独自アイコンセット。型安全性の担保方法が違う

---

## 4. 横断テーマの深掘り

### 4.1 最重要：Actor の不在を Mutex で埋める

iOS 版で一番きれいだったのは `WeatherRepository` の Actor だった。

```swift
public actor WeatherRepository: WeatherRepositoryProtocol {
    private var cache: [String: (weather: Weather, cachedAt: Date)] = [:]
    public func fetchWeather(latitude: Double, longitude: Double) async throws -> Weather {
        let key = cacheKey(latitude: latitude, longitude: longitude)
        if let cached = cache[key], /* 有効期限内 */ { return cached.weather }
        let weather = try await weatherClient.fetchWeather(...)
        cache[key] = (weather, Date())   // ← Actor が排他を保証
        return weather
    }
}
```

Actor は「この型に触れるのは一度に 1 タスクだけ」を言語が保証する。だから `cache` への読み書きが競合しない。

**Kotlin に actor はない。** 代わりに `Mutex` で同じ排他を手書きする。

```kotlin
class DefaultWeatherRepository @Inject constructor(
    private val api: OpenMeteoApi,
) : WeatherRepository {
    private val mutex = Mutex()
    private val cache = mutableMapOf<String, CachedWeather>()
    private val cacheDuration = 10.minutes

    override suspend fun fetchWeather(lat: Double, lon: Double): Weather {
        val key = cacheKey(lat, lon)
        mutex.withLock {
            cache[key]?.let { if (it.isValid(cacheDuration)) return it.weather }
        }
        val weather = api.forecast(lat, lon).toWeather()  // ← ロックの外でネットワーク
        mutex.withLock { cache[key] = CachedWeather(weather, now()) }
        return weather
    }
}
```

ここで Principal として 3 つ伝えたい。

1. **ロックの中で `suspend`（ネットワーク）を呼ばない。** 上の例ではキャッシュ読み取りと書き込みだけ `withLock` で囲み、ネットワーク本体はロックの外。Swift の Actor は再入（reentrancy）を許すため `await` 中に他タスクが入り込めるが、`Mutex` は素朴にブロックするので、ロック区間を最小化するのは自分の責任。これは iOS の「Actor 再入性バグ」と表裏の関係にある論点である。

2. **`@Synchronized`（旧来の手法）は使わない。** スレッドをブロックしてコルーチンの利点を殺す。必ずコルーチン対応の `Mutex` を使う。

3. **そもそもキャッシュ層を `StateFlow` で表現する手もある。** 「最後に取得した天気を流し続ける」モデルなら排他自体が不要になる設計もありうる。Phase 2 でどちらが WeatherNow に合うか議論する価値がある。

### 4.2 単方向データフロー：TCA の経験が MVI で活きる

TCA をやった人にとって MVI は「軽い TCA」に見える。実際そう。違いは規律の強制が言語/フレームワーク側か自分側か、だけ。

`StateFlow`（状態）と `SharedFlow`/`Channel`（イベント）の使い分けが Android の肝。

- 画面に**今ある状態**（loading / loaded / error）→ `StateFlow`。何度購読しても最新が一発で取れる
- **一度きりの出来事**（スナックバー表示・画面遷移・トースト）→ `Channel` を `receiveAsFlow()` で。再購読で再発火しない

iOS の TCA だと両方 `Action`/`State` で表現できたが、Android では「状態」と「イベント」を型で分けるのが定石。ここを混同すると、画面回転のたびにエラーダイアログが再表示される等のバグになる。

### 4.3 Navigation 3：StackState の経験が直結する

TCA の `RootFeature` では典型的にこう書く——

```swift
public var weatherPath: StackState<WeatherPath.State> = .init()
// ...
state.weatherPath.append(WeatherPath.State.weeklyForecast(.init(weather: weather)))
```

Nav3 ではバックスタックを Compose state のリストとして持ち、`append`/`removeLast` で操作する発想に変わる。NavDisplay がそのリストを監視して画面を出し入れする。「ナビゲーション状態を単一の真実として所有する」という核が完全に同じ。

ただし Nav3 は stable 化したばかりで実例が少ない。Phase 5 では最新の公式ドキュメントを確認しながら、TCA で得た「StackState を親が一元管理する」感覚を頼りに進める。

### 4.4 テスト戦略：Swift Testing の 7 原則がそのまま移る

iOS で確立したテスト設計（1 テスト 1 関心事・境界値・パラメータ化・UUID 隔離・呼び出し回数カウント）は Android でも全部使える。対応表：

- `@Test("日本語で意図")` → JUnit5 `@DisplayName` + `@Test`
- パラメータ化 `zip` → JUnit5 `@ParameterizedTest` + `@MethodSource`
- actor `CallCounter` → MockK の `verify(exactly = 1)` or 自前カウンタ
- `withDependencies { $0.x = Stub }` → Hilt `@TestInstallIn` or コンストラクタに直接 fake
- StateFlow の検証 → **Turbine**（`flow.test { assertEquals(...) }`）
- 非同期 → `runTest`（仮想時間で debounce をテスト。TCA の `TestClock` に対応）
- スクショ回帰 → Roborazzi（iOS のスナップショットテスト相当）

debounce の検証は特に対応がきれい。TCA で `TestClock` を `advance(by:)` したのと同じことを、`runTest` の仮想時間 + `advanceTimeBy()` でやる。

---

## 5. 実務への接続

この移植で身につく要素は、そのまま実務装備になる。

- **マルチモジュール + convention plugin** … チーム開発でビルド時間と境界を制御する基盤
- **MVVM / MVI を両方理解して選べる** … 「うちはどっちで行くか」を根拠つきで決められる
- **Hilt の設計** … モジュール構成・スコープ（Singleton/ViewModel/Activity）の判断
- **Coroutines/Flow の並行設計** … Mutex/StateFlow の使い分け、構造化並行性
- **Nav3** … 新しい標準を早期に押さえてチームに展開できる
- **テスト文化** … iOS で築いた規律を Android チームに移植できる

---

## 6. 読み進め方と Phase 0 の準備

このドキュメントはこの順で読むとよい：

1. 第 0 章（学ぶものの正体）と第 2 章（マッピング）を読んで「翻訳作業だ」という全体感を掴む
2. 第 4.1（Actor → Mutex）だけは概念として腰を据えて読む。ここが唯一の「新しい概念」
3. 第 3 章の Phase 一覧をざっと眺めて、全体の長さ感を掴む

Phase 0 を始める前に確認しておくこと：

- Android Studio が最新か（Nav3 / Compose 1.10 対応版）
- iOS 版の `Package.swift` を手元に開いておく（Phase 0 で Gradle モジュールにマッピングする際の対照表になる）

Phase 0 は「`settings.gradle.kts` + `libs.versions.toml` を書く」ところから手を動かす。
