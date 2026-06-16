---
name: android-extract
description: Android コードのリソース化・定数化スキル。「ハードコードを整理して」「リソース化して」「マジックナンバーを定数にして」「文字列を外だしして」「抽出して」「/android-extract」などの場面で積極的に使うこと。git diff から変更ファイルを自動検出し、ハードコード文字列・dp/sp 値・マジックナンバー・ログタグを適切な層（strings.xml / dimens.xml / 定数 / TAG）に書き換える。実装完了後の品質チェックとして呼び出される。
---

# Android Extract

git diff で変更を検出し、ハードコード文字列・マジックナンバーを適切なリソース・定数に置き換える。

---

## Step 1: 変更ファイルを収集する

```bash
git diff HEAD --name-only
git diff --cached --name-only
```

変更ファイルがなければ、ユーザーに対象ファイルを確認する。

---

## Step 2: 各ファイルの内容を読み込んで検出する

変更ファイルを読み込み、以下の5カテゴリを全て検出する。

### カテゴリ A — UI 文字列（Compose）

**対象**: `feature/*/`, `core/ui/` 配下の `.kt` ファイル

検出パターン：
- `Text("文字列")` / `Text(text = "文字列")`
- `contentDescription = "文字列"`
- `placeholder = "文字列"`, `label = "文字列"`
- `title = "文字列"`, `hint = "文字列"`

**除外**（`strings.xml` に入れない）：
- フォーマットテンプレートとして使っているが値が "%1$s" のような場合
- ログ・デバッグ用の文字列
- 空文字 `""`

### カテゴリ B — dp / sp 値

**対象**: 全モジュールの Compose ファイル

検出パターン：
- `padding(16.dp)`, `Modifier.size(48.dp)` などの数値 dp
- `fontSize = 14.sp` などの数値 sp
- `height(2.dp)`, `width(1.dp)` など

**除外**:
- `0.dp`, `1.dp`（ゼロや境界線はそのまま）
- デザイントークンや既存の Token クラス（`WeatherNowSpacing.medium` 等）を使えば OK

### カテゴリ C — ネットワーク層の文字列定数

**対象**: `core/network/` 配下の `.kt` ファイル

検出パターン：
- クエリパラメータのデフォルト値（`"metric"`, `"en"` 等）
- `defaultValue = "..."` のような文字列

### カテゴリ D — マジックナンバー（数値定数）

**対象**: `core/domain/`, `core/network/` 配下の `.kt` ファイル

検出パターン：
- `timeoutMillis = 30_000`, `5000L`
- キャッシュ有効期限（`300_000L`, `3600 * 1000` 等）
- リトライ回数（`maxRetries = 3`）
- 変換係数（`9.0 / 5.0`, `32.0` 等）

**除外**:
- `0`, `1`, `-1`（汎用すぎて定数化の意味がない）
- enum のコード値（`@Suppress("MagicNumber")` を付与するだけでよい）

### カテゴリ E — ログタグ

**対象**: 全モジュールの `.kt` ファイル

検出パターン：
- `Log.d("ClassName", ...)`, `Log.e("ClassName", ...)` 等
- `TAG` という名前の定数がすでに定義されていれば **スキップ**

---

## Step 3: 検出結果をユーザーに提示して確認を取る

変更前に必ずユーザーに一覧を見せてから進む。以下の形式で提示する：

```
検出されたハードコード（N件）:

[A: UI 文字列]
- CurrentWeatherScreen.kt:42 — Text("天気予報")
  → strings.xml: <string name="screen_title_weather">天気予報</string>
  → stringResource(R.string.screen_title_weather)

[B: dp 値]
- WeatherLoadedView.kt:78 — Modifier.padding(16.dp)
  → dimens.xml: <dimen name="spacing_medium">16dp</dimen>
  → dimensionResource(R.dimen.spacing_medium)

[D: マジックナンバー]
- WeatherRepository.kt:55 — cacheExpiryMs = 300_000L
  → companion object: private const val CACHE_EXPIRY_MS = 300_000L

[E: ログタグ]
- CurrentWeatherViewModel.kt:12 — Log.d("CurrentWeatherViewModel", ...)
  → private const val TAG = "CurrentWeatherViewModel"

---
これらを書き換えてよいですか？曖昧なものがあれば個別に確認します。
```

**曖昧なケースは個別確認する：**
- 翻訳不要かもしれない文字列（内部 ID・デバッグ文字列）→ 定数にすべきか `strings.xml` か
- dp 値の命名（`16.dp` → `spacing_medium` か `padding_content` か）→ 候補を提示して選んでもらう
- 既存の strings.xml に近い文字列がある → 流用か新規追加か

---

## Step 4: リソース・定数の配置先を決める

### strings.xml の配置ルール

| 文字列の種類 | 配置先モジュール |
|---|---|
| 複数 feature で使う共通文字列（`cd_*`, `label_*`, `format_*`, 天気コード名） | `core/ui/src/main/res/values/strings.xml` |
| 特定画面のタイトル・ボタン（`screen_title_*`, `action_*`） | `feature/<name>/src/main/res/values/strings.xml` |

### strings.xml のキー命名規則（既存スタイルに合わせる）

| prefix | 用途 |
|---|---|
| `screen_title_` | 画面タイトル |
| `action_` | ボタン・アクション |
| `cd_` | contentDescription |
| `label_` | ラベル |
| `format_` | フォーマット文字列（`%1$s` 形式を使う） |
| `weather_` | WeatherCode の表示名 |

### dimens.xml の配置

`core/ui/src/main/res/values/dimens.xml` に追加する（なければ新規作成）。

既存のデザイントークン（`WeatherNowSpacing`, `WeatherNowTypography` 等）が `:core:ui` に存在する場合は dimens.xml より **Token を優先する**。

### 定数の配置

| 層 | 配置先 |
|---|---|
| `core/network/` | 該当クラスの companion object、または `NetworkConstants.kt` |
| `core/domain/` | 該当クラスの companion object |
| `core/model/` | 変換係数は companion object の `private const val`。enum コード値は `@Suppress("MagicNumber")` |

---

## Step 5: ファイルを書き換える

### A. strings.xml への追記

既存ファイルに追記する（重複チェック必須）。フォーマット文字列は `%1$s`, `%2$s` 形式で。

```xml
<string name="weather_screen_title">天気予報</string>
<string name="format_humidity">%1$d%%</string>
```

### B. dimens.xml への追記

```xml
<dimen name="spacing_medium">16dp</dimen>
<dimen name="card_icon_size">48dp</dimen>
```

### C. Compose コードの書き換え

```kotlin
// UI 文字列
// Before: Text("天気予報")
Text(stringResource(R.string.weather_screen_title))

// feature モジュールから core:ui のリソースを参照するときは CoreUiR alias
import com.doihei.weathernow.core.ui.R as CoreUiR
Icon(contentDescription = stringResource(CoreUiR.string.cd_refresh))

// dp 値（dimens.xml を使う場合）
// Before: Modifier.padding(16.dp)
Modifier.padding(dimensionResource(R.dimen.spacing_medium))
```

### D. 定数の追加

```kotlin
// companion object に追加
companion object {
    private const val CACHE_EXPIRY_MS = 300_000L
    private const val TIMEOUT_SECONDS = 30L
    private const val UNIT_METRIC = "metric"
}
```

### E. ログタグの定数化

クラス定義の直後（companion object がない場合はクラス外のトップレベル）に追加する：

```kotlin
private const val TAG = "CurrentWeatherViewModel"
```

---

## Step 6: 変更後のサマリーを出力する

```
変更完了:

strings.xml に追加: 3件
  - core/ui: screen_title_weather, label_temperature
  - feature/weather-mvvm: action_go_to_mvi

dimens.xml に追加: 2件
  - spacing_medium (16dp), card_icon_size (48dp)

定数を追加: 2件
  - WeatherRepository.kt: CACHE_EXPIRY_MS
  - NetworkConstants.kt: UNIT_METRIC

ログタグを定数化: 1件
  - CurrentWeatherViewModel.kt: TAG

注意: dimensionResource() の import 追加が必要なファイル:
  - WeatherLoadedView.kt
```

import 漏れや R クラスの参照ミスが起きやすい箇所を指摘する。

---

## 注意事項

- **翻訳不要な文字列**（ログ・API パラメータ・フォーマット記号）は `strings.xml` に入れない
- `CoreUiR` alias が必要になるのは feature モジュールから `:core:ui` のリソースを参照するときのみ
- detekt の `MagicNumber` ルールは enum のコード値に限り `@Suppress("MagicNumber")` で対応する（定数化しない）
- 一度に多くのファイルを変更するとレビューしにくくなるため、ファイル単位で段階的に作業するよう心がける
