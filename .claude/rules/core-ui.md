---
paths:
  - "core/ui/**"
---

# :core:ui 層のルール

## パッケージ・ディレクトリ構造

```
core/ui/src/main/kotlin/.../core/ui/
├── component/              # 複数 feature から再利用する UI コンポーネント
│   ├── WeatherLoadedView.kt
│   ├── WeatherLoadingView.kt
│   ├── WeatherErrorView.kt
│   └── HourlyForecastChart.kt
├── theme/                  # MaterialTheme カスタマイズ・デザイントークン
│   ├── WeatherNowColor.kt      # カラーパレット定義（light / dark トークン）
│   ├── WeatherNowSize.kt       # サイズトークン（コンポーネントの高さ・アイコンサイズ）
│   ├── WeatherNowSpacing.kt    # スペーシングトークン（余白・パディング）
│   ├── WeatherNowTypography.kt # タイポグラフィトークン（TextStyle 定義）
│   └── WeatherNowTheme.kt      # MaterialTheme カスタマイズ（colorScheme / typography を配線）
└── weather/                # domain モデルと UI の橋渡し（@StringRes extension 等）
    └── WeatherCodeRes.kt   # WeatherCode.labelResId extension

core/ui/src/main/res/
└── values/
    ├── strings.xml         # 複数モジュールで使う共通文字列・フォーマット・contentDescription
    └── dimens.xml          # コンポーネント固有のサイズ・間隔
```

## コンポーネント設計

### @Composable 関数の基本パターン

```kotlin
@Composable
fun WeatherLoadedView(
    weather: Weather,
    modifier: Modifier = Modifier,   // modifier は必ず最後の前に置き、デフォルト Modifier を持つ
) { ... }
```

- `modifier` 引数はデフォルト `Modifier` を持ち、呼び出し元がレイアウトを制御できるようにする
- ロジックを持たない「表示専用」コンポーネントに徹する
- `:core:domain` / `:core:network` を参照しない。データは引数で受け取る

### @Preview の追加ルール

```kotlin
@Preview(showBackground = true)
@Composable
private fun WeatherLoadingViewPreview() {
    WeatherNowTheme {
        WeatherLoadingView(modifier = Modifier.fillMaxSize())
    }
}
```

- すべての公開コンポーネントに `@Preview` を追加する
- Preview 関数は `private` にする（detekt の `UnusedPrivateMember` 除外済み）
- サイズが必要なコンポーネントは `Modifier.fillMaxSize()` を渡す（空白 Preview を防ぐ）
- Preview では必ず `WeatherNowTheme` でラップしてテーマを適用する

### WeatherLoadingView の呼び出しルール

`WeatherLoadingView` を `Column` の中で使う場合は **必ず `Modifier.fillMaxSize()` を渡す**。

```kotlin
is WeatherViewState.Loading -> WeatherLoadingView(modifier = Modifier.fillMaxSize())
```

- `WeatherLoadingView` 内部は `Box(contentAlignment = Center)` で中央配置している
- `Box` は親から与えられたサイズに従って配置を決める。`fillMaxSize()` がないと `Box` がインジケーターと同じサイズに縮み、`contentAlignment = Center` が効かない
- 結果として `CircularProgressIndicator` が左上に偏って表示されるため、呼び出し元での指定が必須

### build.gradle.kts の依存宣言

```kotlin
// Preview ツール（デバッグビルドのみ）
debugImplementation(libs.compose.ui.tooling)
debugImplementation(libs.compose.ui.test.manifest)   // Preview が含まれる APK 用
```

- `ui-tooling` は `debugImplementation` のみ。`implementation` にしない

## 文字列リソースの管理

### core:ui に置くもの

- 複数の feature モジュールで使う contentDescription（`cd_refresh`, `cd_back`）
- 複数コンポーネントで共通のラベル文字列（`label_humidity`, `label_wind_speed` 等）
- 数値フォーマット文字列（`format_temperature`, `format_temp_range` 等）
- WeatherCode の表示文字列（`weather_clear_sky` 等）

### feature モジュールに置くもの

- 画面タイトル（`screen_title_weather_mvvm` 等）
- その画面だけで使うボタン文字列（`action_go_to_mvi` 等）

### フォーマット文字列のルール

```xml
<!-- 複数引数のフォーマットは必ずポジショナル形式（%1$s, %2$s）を使う -->
<string name="format_temperature">%1$.1f%2$s</string>
<string name="format_temp_range">%1$.0f / %2$.0f%3$s</string>

<!-- 非ポジショナル（%.1f%s）は非 positional フォーマット警告が出るため使わない -->
```

- `%1$s` 形式にすることで、翻訳時に引数の順序を変えられる
- `stringResource(R.string.format_temperature, value, unit)` で展開する

## WeatherCode → 表示文字列のマッピング

`WeatherCode` の表示ラベルは `WeatherCodeRes.kt` の extension property で管理する。

```kotlin
// core/ui/weather/WeatherCodeRes.kt
val WeatherCode.labelResId: Int
    @StringRes get() = when (this) {
        WeatherCode.CLEAR_SKY -> R.string.weather_clear_sky
        // ...
    }

// 使う側
Text(stringResource(weather.currentWeather.code.labelResId))
```

- `WeatherCode` 自体（`:core:model`）に文字列 ID を持たせない（Android 依存を禁止するため）
- この extension が `:core:model` と `:core:ui` の唯一の橋渡し

## 寸法リソースの管理

### 基本方針：Kotlin Token を使う（dimens.xml は例外）

dp 値は `WeatherNowSpacing` / `WeatherNowSize` の Kotlin Token が第一選択。
`dimens.xml` は端末サイズ修飾子（`-sw600dp` 等）で値を切り替えたい場合のみ使う。

**Token を優先する理由：**
- Kotlin コードだけで完結し、`Context` が不要（テスト・Preview ともに動く）
- IDE の補完で名前が出る（`R.dimen.spacing_lg` より発見しやすい）
- 将来 `CompositionLocal` でオーバーライドできる拡張性がある

### WeatherNowSpacing（余白・パディング）

```kotlin
// core/ui/theme/WeatherNowSpacing.kt
object WeatherNowSpacing {
    val xs   = 4.dp   // 最小余白（気温と体感温度の間など）
    val sm   = 8.dp   // 小さな余白・アイコン周辺
    val md   = 12.dp  // リストアイテム間・区切り線前後
    val lg   = 16.dp  // 画面端マージン・セクション間（最もよく使う）
    val xl   = 24.dp  // 大きなセクション余白・エラー画面
    val card = 20.dp  // Card 内パディング専用
}
```

### WeatherNowSize（コンポーネントの大きさ）

```kotlin
// core/ui/theme/WeatherNowSize.kt
object WeatherNowSize {
    val iconMd            = 24.dp  // アイコン（中）
    val iconLg            = 32.dp  // アイコン（大）
    val chartHourlyHeight = 200.dp // HourlyForecastChart の高さ
}
```

### WeatherNowTypography（フォントスタイル）

```kotlin
// core/ui/theme/WeatherNowTypography.kt
object WeatherNowTypography {
    val displayLarge  = TextStyle(fontSize = 57.sp, fontWeight = FontWeight.Light, ...)  // 気温
    val displayMedium = TextStyle(fontSize = 45.sp, ...)                                 // エラー絵文字
    val titleLarge    = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium, ...) // 天気コード
    val titleMedium   = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, ...) // セクション見出し
    val bodyLarge     = TextStyle(fontSize = 16.sp, ...)                                 // エラーメッセージ
    val bodyMedium    = TextStyle(fontSize = 14.sp, ...)                                 // 曜日・気温範囲
    val bodySmall     = TextStyle(fontSize = 12.sp, ...)                                 // 予報の補足
    val labelSmall    = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, ...) // 湿度・風速ラベル
}
```

`WeatherNowTheme.kt` で `Typography(...)` に変換して `MaterialTheme` に渡すため、
コンポーネント側は `MaterialTheme.typography.displayLarge` のまま変更不要。

**コンポーネントでの使い方（iOS との対応）:**

| iOS | Compose |
|---|---|
| `Font.system(.largeTitle, weight: .thin)` | `MaterialTheme.typography.displayLarge` |
| `Font.system(.title, weight: .regular)` | `MaterialTheme.typography.titleLarge` |
| `Font.system(.headline, weight: .medium)` | `MaterialTheme.typography.titleMedium` |
| `Font.system(.body)` | `MaterialTheme.typography.bodyLarge` |
| `Font.system(.subheadline)` | `MaterialTheme.typography.bodyMedium` |
| `Font.system(.caption)` | `MaterialTheme.typography.bodySmall` |
| `Font.system(.caption2)` | `MaterialTheme.typography.labelSmall` |

### Token にない値が出てきたら

既存 Token と同じ dp 値なら流用する。新しい用途・値なら該当 Token ファイルにプロパティを追記してから使う（`dimens.xml` に追加しない）。

```kotlin
// 例：WeatherNowSize.kt に追記
val someNewSize = 40.dp
```

### dimens.xml を使う例外ケース

端末幅によってレイアウトを変えたい場合は `res/values-sw600dp/dimens.xml` 等と組み合わせて使う。
その場合のみ `dimensionResource(R.dimen.xxx)` を使用する。

## 依存関係の制約

- `:core:ui` が参照してよいのは `:core:model` のみ
- `:core:domain` / `:core:network` / `:feature:*` を参照しない
- `:feature:*` で `:core:ui` のリソース ID を参照するときは `CoreUiR` alias を使う：

```kotlin
import com.doihei.weathernow.core.ui.R as CoreUiR

Icon(..., contentDescription = stringResource(CoreUiR.string.cd_refresh))
```
