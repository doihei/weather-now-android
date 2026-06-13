---
paths:
  - "core/network/**"
---

# :core:network 層のルール

## パッケージ・ディレクトリ構造

```
core/network/src/main/kotlin/.../core/network/
├── api/        # Retrofit インターフェースと固定クエリ定数
├── dto/        # @Serializable DTO（API レスポンスの型）
├── mapper/     # DTO → Domain モデル変換
└── di/         # Hilt Module と Qualifier
```

## DTO の設計

- `@Serializable` data class で定義する
- 同一 API レスポンスを構成するネストした型は **1 ファイルにまとめる**
  （例：`ForecastResponseDto.kt` に `ForecastCurrentDto` / `ForecastHourlyDto` 等を同居）
- snake_case → camelCase の変換は `@SerialName` で行う
- `Json{}` の設定（`ignoreUnknownKeys = true` / `coerceInputValues = true`）は
  `NetworkModule` で一元管理済み。各 DTO に個別設定しない

## Retrofit インターフェースの設計

- メソッドは必ず `suspend fun` で定義する
- Retrofit の `interface` はデフォルト引数を `@Query` と組み合わせできない。
  固定クエリ値は `OpenMeteoQueryDefaults` に定数として切り出し、リポジトリ層から渡す
- API 仕様上やむを得ず引数が 6 個を超える場合は `@Suppress("LongParameterList")` を付与する
  （detekt の `LongParameterList` は Retrofit `@Query` に誤検知するため）

## マッパーの設計

- DTO → Domain 変換は **extension 関数**として `mapper/` に定義する
  （iOS の `extension ForecastResponse { func toWeather() }` に対応）
- DTO を上位層（domain / feature / ui）に持ち出さない。変換はこの層で完結させる
- 変換ヘルパー（`toHourlyForecasts()` 等）は `private` で定義して外部に公開しない

## DI（Hilt）の設計

- 同じ型（例：`Retrofit`）のインスタンスが複数ある場合は `@Qualifier` で区別する
- Qualifier は `NetworkQualifiers.kt` に集約して定義する
- OkHttpClient / Retrofit / OpenMeteoApi はすべて `@Singleton` + `SingletonComponent`

## 依存関係の制約

- `:core:network` が参照してよいのは `:core:model` のみ
- `:core:ui` / `:feature:*` / `:core:domain` を参照しない

## Lint 設定

- `core/network/lint.xml` で `kotlinx.serialization.InternalSerializationApi` の opt-in を設定済み
- `@Serializable` 由来の `UnsafeOptInUsageError` は Lint の誤検知のため個別対応不要

## テスト設計

Mapper は DTO を受け取って Domain モデルを返す純粋関数なので、**モック不要・外部依存なし**で完結する。

### フィクスチャヘルパーでテストデータを一元管理する

デフォルト値を持つ `make*Dto()` ヘルパーを用意し、テストごとに変えたいフィールドだけ上書きする。

```kotlin
private fun makeCurrentDto(
    temperature2m: Double = 20.0,
    weatherCode: Int = 0,
    // ...
) = ForecastCurrentDto(...)

// 使う側：関心のあるフィールドだけ指定
val dto = makeResponseDto(current = makeCurrentDto(weatherCode = 61))
```

- ヘルパーは `private fun` に限定する（テスト外部に公開しない）
- iOS の `makeCurrentResponse()` ファクトリに対応

### @Nested でマッピング対象ごとに分類する

変換先（CurrentWeather / HourlyForecast / DailyForecast）ごとに `@Nested` inner class を切る。

```kotlin
@Nested
@DisplayName("HourlyForecast リストへの変換")
inner class HourlyForecastConversion {
    @Test fun `valid time strings are converted`() { ... }
    @Test fun `invalid time string entries are skipped`() { ... }
}
```

- 1 Nested = 1 変換対象。複数の変換をフラットに並べない
- iOS の入れ子 `struct` + `@Suite` に対応

### 境界値テストを必ず書く

Mapper の堅牢性はエッジケースで決まる。以下は必須で検証する。

| ケース | 検証内容 |
|---|---|
| null フィールド | `country = null` → `.orEmpty()` で空文字 |
| 空リスト | `results = emptyList()` → 空リストを返す |
| null リスト | `results = null` → 空リストを返す（`?: emptyList()`） |
| 不正な日時文字列 | `"not-a-date"` → `mapNotNull` でスキップされる |
| 不正な日付フォーマット | `"20240116"`（ISO 形式でない）→ スキップされる |
| 未知の WMO コード | `weatherCode = 9999` → `WeatherCode.UNKNOWN` |

これらは iOS の `compactMap { guard let ... else { return nil } }` に対応するフォールバック挙動の検証。
