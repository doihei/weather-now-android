---
paths:
  - "core/model/**"
---

# :core:model 層のルール

## パッケージ・ディレクトリ構造

```
core/model/src/main/kotlin/com/doihei/weathernow/core/model/
├── city/       # City・GeocodingResult（都市検索ドメイン）
├── error/      # WeatherError（sealed interface によるエラー表現）
├── settings/   # AppSettings（単位・テーマ等のユーザー設定）
└── weather/    # Weather・CurrentWeather・HourlyForecast・DailyForecast・WeatherCode
```

ファイルは必ずパッケージ宣言と一致するフルパスに置くこと。

- 正: `core/model/src/main/kotlin/com/doihei/weathernow/core/model/weather/WeatherCode.kt`
- 誤: `core/model/src/main/kotlin/weather/WeatherCode.kt`

新しいドメイン概念を追加する場合は既存のサブパッケージに収まらなければ新しいサブパッケージを切ること。
`core/model/` 直下にファイルを置かない。

## Android 依存を持ち込まない

Context・R・android.* は `:core:model` に入れない。
文字列リソース（@StringRes）や Drawable は `:core:ui` に置く。

## 値型は data class、エラー型は sealed interface

- 不変値モデル → `data class`（equals/hashCode/copy が自動生成される）
- エラー表現 → `sealed interface`（後で Throwable と組み合わせる柔軟性のため）

## ローカライズ文字列のハードコード禁止

enum の `description` 等に日本語を直書きしない。
表示文字列は `:core:ui` の `strings.xml` + `@StringRes` 拡張プロパティで管理する。
→ `WeatherCode.labelResId`（`core/ui/.../WeatherCodeRes.kt`）が参照実装。

## MagicNumber の扱い

- 変換係数（9.0/5.0/32.0 等）→ companion object に名前付き `private const val` で抽出
- enum のコード値（WMO コード等）→ エントリ値そのものなので `@Suppress("MagicNumber")` を付与

## テスト設計

`:core:model` は Android 依存を持たない純粋 Kotlin 層なので、**モック不要・JVM テストのみ**で完結する。

### @ParameterizedTest で値テーブルを網羅する

enum の変換ロジック（`WeatherCode.from(code)` 等）は代表値を一括で検証する。

```kotlin
companion object {
    @JvmStatic                           // @MethodSource から呼ぶため必須
    fun knownWmoCodes(): Stream<Array<Any>> = Stream.of(
        arrayOf(0, WeatherCode.CLEAR_SKY),
        arrayOf(61, WeatherCode.LIGHT_RAIN),
        // ...
    )
}

@ParameterizedTest(name = "WMOコード {0} → {1}")
@MethodSource("knownWmoCodes")
@DisplayName("既知の WMO コードが正しく WeatherCode に変換される")
fun `known WMO code maps to correct WeatherCode`(code: Int, expected: WeatherCode) {
    assertEquals(expected, WeatherCode.from(code))
}
```

- `@JvmStatic` なしだと `@MethodSource` がデータプロバイダを見つけられない
- テスト名テンプレート `{0}`, `{1}` でレポートの可読性を上げる

### フォールバック・境界値を必ず独立したテストにする

```kotlin
@Test
@DisplayName("未知の WMO コードは UNKNOWN にフォールバックする")
fun `unknown WMO code falls back to UNKNOWN`() { ... }

@Test
@DisplayName("WMO コード -1 は UNKNOWN エントリ自身にマッチする")
fun `wmo code minus one matches UNKNOWN entry itself`() { ... }
```

フォールバックパスは `@ParameterizedTest` に混ぜず、意図を明示した独立テストにする。
