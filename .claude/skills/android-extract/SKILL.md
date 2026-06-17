---
name: android-extract
description: Android コードのリソース化・定数化スキル。「ハードコードを整理して」「リソース化して」「マジックナンバーを定数にして」「文字列を外だしして」「抽出して」「/android-extract」などの場面で積極的に使うこと。git diff から変更ファイルを自動検出し、ハードコード文字列・dp/sp 値・マジックナンバー・ログタグを適切な層（strings.xml / dimens.xml / 定数 / TAG）に書き換える。実装完了後の品質チェックとして呼び出される。
---

# Android Extract

git diff で変更を検出し、ハードコード文字列・マジックナンバーを適切なリソース・定数に置き換える。

---

## Step 1: 走査対象をユーザーと確認する

まず以下を確認する：

```bash
git diff HEAD --name-only
git diff --cached --name-only
```

**結果に応じて、走査方針をユーザーに選んでもらう：**

```
走査対象の確認:

[未コミットの変更がある場合]
git diff に N件の変更ファイルが見つかりました。
  - core/ui/.../WeatherLoadedView.kt
  - feature/.../CurrentWeatherScreen.kt

① git 差分のファイルをそのまま走査する（推奨）
② 走査するファイルを自分で指定する

どちらにしますか？

---

[未コミットの変更がない場合]
現在コミット済みです。直近のコミットに含まれるファイルを候補として提案します：

  - HEAD~1（chore: ...）: core/ui/.../WeatherLoadedView.kt
  - HEAD~2（feat: ...）: HourlyForecastChart.kt, dimens.xml

① 直近コミットのファイルを走査する（上記から選択）
② 走査するファイルを自分で指定する

どちらにしますか？
```

ユーザーの回答に従い、走査対象ファイルを確定する。

---

## Step 2: 各ファイルの内容を読み込んで検出する

走査対象ファイルを読み込み、以下の5カテゴリを全て検出する。

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

**置き換え先の優先順位（重要）:**

1. **Kotlin Token を優先する** — `WeatherNowSpacing` / `WeatherNowSize` の既存値に合うならそちらを使う
   - 余白・パディング → `WeatherNowSpacing`（xs=4, sm=8, md=12, lg=16, xl=24, card=20）
   - コンポーネントサイズ・アイコン → `WeatherNowSize`（iconMd=24, iconLg=32, chartHourlyHeight=200）
2. **dimens.xml は例外のみ** — 端末サイズ修飾子（`-sw600dp` 等）が必要な値だけ `dimens.xml` に追加する

**除外**:
- `0.dp`, `1.dp`（ゼロや境界線はそのまま）
- すでに Token クラスを使っている（`WeatherNowSpacing.lg` 等）

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
  → WeatherNowSpacing.lg（16dp に一致）

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

### dp / sp 値の配置

**原則: Kotlin Token を使う（dimens.xml は例外）**

まず `WeatherNowSpacing` / `WeatherNowSize` の既存値と照合する：

| Token | 値 | 用途 |
|---|---|---|
| `WeatherNowSpacing.xs` | 4dp | 最小余白 |
| `WeatherNowSpacing.sm` | 8dp | 小さな余白 |
| `WeatherNowSpacing.md` | 12dp | リスト間・区切り線 |
| `WeatherNowSpacing.lg` | 16dp | 画面端マージン |
| `WeatherNowSpacing.xl` | 24dp | 大セクション余白 |
| `WeatherNowSpacing.card` | 20dp | Card 内パディング |
| `WeatherNowSize.iconMd` | 24dp | アイコン（中） |
| `WeatherNowSize.iconLg` | 32dp | アイコン（大） |
| `WeatherNowSize.chartHourlyHeight` | 200dp | 時間帯グラフ高さ |

- 一致する Token があれば迷わずそちらを使う
- Token にない値は新しい Token プロパティを追加することを提案する
- **`dimens.xml` を使うのは端末サイズ修飾子（`-sw600dp` 等）が必要な場合だけ**

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

### B. dp 値の書き換え（Kotlin Token）

```kotlin
// Before: Modifier.padding(16.dp)
Modifier.padding(WeatherNowSpacing.lg)

// Before: Modifier.size(24.dp)
Modifier.size(WeatherNowSize.iconMd)
```

Token にない新しい値を追加する場合は、該当 Token ファイルにプロパティを追記してから使う：

```kotlin
// WeatherNowSize.kt に追記
val someNewSize = 40.dp

// 使う側
Modifier.size(WeatherNowSize.someNewSize)
```

`dimens.xml` を使うのは端末サイズ修飾子（`-sw600dp` 等）が必要な値だけ。その場合のみ `dimensionResource(R.dimen.xxx)` を使う。

### C. Compose コードの書き換え（文字列）

```kotlin
// UI 文字列
// Before: Text("天気予報")
Text(stringResource(R.string.weather_screen_title))

// feature モジュールから core:ui のリソースを参照するときは CoreUiR alias
import com.doihei.weathernow.core.ui.R as CoreUiR
Icon(contentDescription = stringResource(CoreUiR.string.cd_refresh))
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

## Step 5.5: 不要になった import を削除する

dp 値をすべて `dimensionResource()` に置き換えたファイルについて、以下を確認する：

```kotlin
import androidx.compose.ui.unit.dp
```

ファイル内に `.dp` の使用箇所が残っていなければ、この import は不要なので削除する。
同様に `import androidx.compose.ui.unit.sp` も `sp` が残っていなければ削除する。

**確認手順：**
1. 書き換え後のファイルを読み込む
2. `.dp` / `.sp` のリテラルが残存していないかチェック
3. 残存がなければ該当 import 行を削除する

---

## Step 6: 変更後のサマリーを出力する

```
変更完了:

strings.xml に追加: 3件
  - core/ui: screen_title_weather, label_temperature
  - feature/weather-mvvm: action_go_to_mvi

dp 値を Token に置き換え: 3件
  - WeatherLoadedView.kt:78  16.dp → WeatherNowSpacing.lg
  - WeatherLoadedView.kt:92   8.dp → WeatherNowSpacing.sm
  - HourlyForecastChart.kt:34 24.dp → WeatherNowSize.iconMd

Token に新規追加: 1件
  - WeatherNowSize.kt: val someNewSize = 40.dp

定数を追加: 2件
  - WeatherRepository.kt: CACHE_EXPIRY_MS
  - NetworkConstants.kt: UNIT_METRIC

ログタグを定数化: 1件
  - CurrentWeatherViewModel.kt: TAG

import を削除: 1件
  - WeatherLoadedView.kt: import androidx.compose.ui.unit.dp（.dp 使用箇所なし）
```

import 漏れや R クラスの参照ミスが起きやすい箇所を指摘する。

---

## 注意事項

- **翻訳不要な文字列**（ログ・API パラメータ・フォーマット記号）は `strings.xml` に入れない
- `CoreUiR` alias が必要になるのは feature モジュールから `:core:ui` のリソースを参照するときのみ
- detekt の `MagicNumber` ルールは enum のコード値に限り `@Suppress("MagicNumber")` で対応する（定数化しない）
- **dp 値は `WeatherNowSpacing` / `WeatherNowSize` Token が第一選択**。`dimens.xml` は端末サイズ修飾子が必要な場合だけ使う
- Token に存在しない dp 値が出てきたら、新しいプロパティを Token ファイルに追加してから使う（`dimens.xml` に追加しない）
- 一度に多くのファイルを変更するとレビューしにくくなるため、ファイル単位で段階的に作業するよう心がける
