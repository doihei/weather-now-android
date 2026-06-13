---
paths:
  - "core/domain/**"
---

# :core:domain 層のルール

## パッケージ・ディレクトリ構造

```
core/domain/src/main/kotlin/com/doihei/weathernow/core/domain/
├── di/         # Hilt Module（@Binds による interface ↔ 実装の束縛）
├── exception/  # WeatherException（WeatherError を Throwable に包むラッパー）
├── location/   # LocationService interface + Location data class + DefaultLocationService
├── repository/ # WeatherRepository interface + DefaultWeatherRepository
└── usecase/    # GetWeatherUseCase（Repository + LocationService の調整役）
```

## 例外クラスの配置

`WeatherException` は `exception/` パッケージに置く。`repository/` や `location/` に置かない。

- `:core:model` が定義する `WeatherError`（型）を domain 層で Throwable に変換する役割
- `repository` と `location` の両方から参照するため、どちらかに置くとクロスパッケージ依存が生まれる

```
WeatherError（:core:model）
    ↓ Throwable ラッパー
WeatherException（:core:domain/exception）
    ↑ repository・location 両方からインポート
```

## Repository の設計

### interface で定義し Hilt で注入する

```kotlin
interface WeatherRepository {
    suspend fun fetchWeather(latitude: Double, longitude: Double): Result<Weather>
    suspend fun searchCities(name: String): Result<List<GeocodingResult>>
}
```

- `throws` を使わず `Result<T>` を返す（例外を型として表現）
- `@Binds` で実装を束縛することでテスト時に fake へ差し替え可能にする

### Mutex キャッシュのルール（最重要）

```kotlin
private val mutex = Mutex()

// Step 1：キャッシュ確認（withLock 内は軽量に）
mutex.withLock { cache[key]?.let { if (it.isValid()) return Result.success(it.weather) } }

// Step 2：ネットワーク取得（withLock の外で実行）
val result = runCatching { api.forecast(...).toWeather() }

// Step 3：書き込み（再びロック）
result.onSuccess { mutex.withLock { cache[key] = CachedEntry(it) } }
```

- `withLock` の中で `suspend`（ネットワーク等）を呼ばない
- 理由：Mutex は再入不可のため、ロック中に suspend すると別コルーチンがデッドロックする
- `@Synchronized` は使わない（コルーチンをスレッドブロックする）

### キャッシュ有効期間などの定数は `companion object` に置く

```kotlin
companion object {
    private const val CACHE_DURATION_MINUTES = 10L
}
```

数値リテラルの直書きは detekt の `MagicNumber` に引っかかる。名前付き定数に抽出する。

## LocationService の設計

### コールバック → suspend 変換は `suspendCancellableCoroutine` を使う

```kotlin
override suspend fun currentLocation(): Result<Location> =
    suspendCancellableCoroutine { continuation ->
        fusedLocationClient
            .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { continuation.resume(Result.success(Location(...))) }
            .addOnFailureListener { continuation.resume(Result.failure(it)) }
    }
```

- iOS の `withCheckedThrowingContinuation` に対応する Kotlin イディオム
- `CancellableContinuation` にすることでコルーチンキャンセル時に適切に後処理できる

### `@param:ApplicationContext` でアノテーションターゲットを明示する

```kotlin
class DefaultLocationService @Inject constructor(
    @param:ApplicationContext private val context: Context,
)
```

- Kotlin 2.x では `val` 付きコンストラクタ引数のアノテーションが将来的に field にも適用される
- Hilt は constructor parameter から qualifier を読むため `@param:` で明示する
- `@ApplicationContext` のみ（ターゲット未指定）だと Kotlin 2.x で警告が出る

## DI の設計（@Binds vs @Provides）

| パターン | クラス種別 | 使う場面 |
|---|---|---|
| `@Binds` | `abstract class` | `@Inject constructor` があり Hilt が自力で生成できる場合 |
| `@Provides` | `object` | サードパーティ製クラスや初期化処理が必要な場合 |

`:core:domain` の実装クラスはすべて `@Inject constructor` を持つため `@Binds` を使う。

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class DomainModule {
    @Binds @Singleton
    abstract fun bindWeatherRepository(impl: DefaultWeatherRepository): WeatherRepository

    @Binds @Singleton
    abstract fun bindLocationService(impl: DefaultLocationService): LocationService
}
```

## テスト設計

### 依存の差し替えは fake をコンストラクタに直接渡す

```kotlin
val fakeRepository = FakeWeatherRepository()
val useCase = GetWeatherUseCase(
    repository = fakeRepository,
    locationService = FakeLocationService(),
)
```

- iOS の `withDependencies { $0.weatherRepository = FakeRepository() }` に対応
- Hilt `@TestInstallIn` はモジュール全体の差し替えが必要な統合テスト向け。ユニットテストでは不要

### Repository キャッシュのテスト観点

| テストケース | 検証内容 |
|---|---|
| キャッシュヒット | 2回目の呼び出しで API が呼ばれないこと |
| キャッシュ期限切れ | 10分経過後に再度 API が呼ばれること |
| 並列リクエスト | 同時に複数呼び出されても二重リクエストにならないこと |
| ネットワーク失敗 | `Result.failure(WeatherException(...))` が返ること |

### LocationService のテスト観点

`FusedLocationProviderClient` は Android フレームワーク依存のため、
`LocationService` interface の fake で代替する。実装クラスは Robolectric が必要。

| テストケース | 検証内容 |
|---|---|
| 位置情報取得成功 | `Result.success(Location(...))` が返ること |
| 位置情報 null | `Result.failure(WeatherException(LocationUnavailable))` が返ること |
| FusedClient 失敗 | `Result.failure(exception)` が返ること |

### UseCase のテスト観点

```kotlin
@Test
@DisplayName("位置情報の取得に失敗した場合、天気取得を試みずに失敗を返す")
fun `returns failure immediately when location fails`() = runTest {
    val useCase = GetWeatherUseCase(
        repository = FakeWeatherRepository(),
        locationService = FakeLocationService(shouldFail = true),
    )
    assertTrue(useCase().isFailure)
    // FakeWeatherRepository の呼び出し回数が 0 であることも検証する
}
```

- `runTest` で suspend 関数をテストする
- 位置情報失敗時に Repository が呼ばれないことを検証する（early return の確認）
