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
│   └── WeatherErrorView.kt
├── theme/                  # MaterialTheme カスタマイズ
│   └── WeatherNowTheme.kt
└── weather/                # domain モデルと UI の橋渡し（@StringRes extension 等）
    └── WeatherCodeRes.kt   # WeatherCode.labelResId extension

core/ui/src/main/res/
└── values/
    └── strings.xml         # 複数モジュールで使う共通文字列・フォーマット・contentDescription
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

## 依存関係の制約

- `:core:ui` が参照してよいのは `:core:model` のみ
- `:core:domain` / `:core:network` / `:feature:*` を参照しない
- `:feature:*` で `:core:ui` のリソース ID を参照するときは `CoreUiR` alias を使う：

```kotlin
import com.doihei.weathernow.core.ui.R as CoreUiR

Icon(..., contentDescription = stringResource(CoreUiR.string.cd_refresh))
```
