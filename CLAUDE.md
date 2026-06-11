# WeatherNow Android — CLAUDE.md

## プロジェクト概要

iOS 版 WeatherNow（MVVM + TCA / 6 モジュール）を Kotlin + Jetpack Compose でゼロから作り直すプロジェクト。
「書き直し」ではなく「同じ設計思想を別の語彙で表現する」翻訳プロジェクトとして捉える。

---

## 技術スタック（2026 年 6 月確定版）

| 領域 | 採用技術 | iOS 版対応 |
|---|---|---|
| 言語 | Kotlin 2.x（K2 コンパイラ） | Swift |
| ビルド | Gradle Kotlin DSL + version catalog + convention plugin | SPM Package.swift |
| アノテーション処理 | KSP（Kapt は使わない） | — |
| DI | Hilt（KSP） | swift-dependencies |
| 非同期 | Coroutines + Flow / StateFlow | async/await + AsyncStream |
| ネットワーク | Retrofit + kotlinx.serialization | APIClient + Decodable |
| UI | Jetpack Compose 1.10.x | SwiftUI |
| ナビゲーション | Navigation 3（1.0 stable / 2025-11） | NavigationStack / TCA StackState |
| グラフ | Vico（Compose charts） | Swift Charts |
| 位置情報 | FusedLocationProviderClient | CLLocationManager |
| ローカライズ | strings.xml + `stringResource()` | Localizable.xcstrings + L10n |
| テスト | JUnit5 + MockK + Turbine + `runTest` + Roborazzi | Swift Testing + withDependencies |

---

## モジュール構成（目標）

```
:build-logic          # convention plugin（build.gradle.kts 重複排除）
:app                  # アプリエントリポイント・DI グラフルート
:core:model           # Domain モデル（data class / sealed）
:core:network         # Retrofit + DTO + Response→Domain 変換
:core:domain          # Repository / UseCase / LocationService
:feature:weather-mvvm # MVVM 実装（ViewModel + StateFlow）
:feature:weather-mvi  # MVI 実装（State / Intent / SideEffect）
:core:ui              # デザインシステム（Tokens / MaterialTheme カスタム）
```

Phase 0 でまず `:build-logic` + `:core:model` を立ち上げる。

---

## 設計原則

### 依存方向
- 依存は下向きのみ：`:feature` → `:core:domain` → `:core:model`
- `:feature` は `:core:network` を直接参照しない

### インターフェース境界
- Repository・LocationService は `interface` で定義し、実装を Hilt で注入する
- DTO（`@Serializable`）を UI まで持ち回らない。必ず Domain モデルに変換する

### Actor の不在（最重要）
- Kotlin に Actor はない。共有可変状態には **`Mutex`** を使う
- `mutex.withLock` の中で `suspend`（ネットワーク等）を呼ばない。ロック区間を最小化する
- `@Synchronized` は使わない（コルーチンをスレッドブロックする）

### 状態 vs イベント
- 画面状態（loading / loaded / error）→ `StateFlow`
- 一度きりの出来事（スナックバー・画面遷移）→ `Channel.receiveAsFlow()`
- 混同すると画面回転のたびにダイアログが再表示されるバグになる

### MVVM と MVI の並存
- `:core:domain` の上に両実装を載せる（iOS 版と同じ比較実装）
- MVI は自前 UDF（TCA のような外部フレームワーク不使用）
- `State/Intent/SideEffect` の責務は自分で守る

---

## iOS → Android 設計マッピング早見表

| iOS | Android |
|---|---|
| `struct` + `Equatable` | `data class` |
| `enum` with associated values | `sealed class` / `sealed interface` |
| `actor`（排他保証） | `Mutex` + `withLock` |
| `async/await` | `suspend` |
| `AsyncStream` | `Flow` / `StateFlow` |
| `@Observable` ViewModel | `ViewModel` + `StateFlow` |
| TCA `State/Action/Effect` | MVI `State/Intent/SideEffect` |
| TCA `StackState` | Navigation 3 バックスタックリスト |
| `@Dependency` / `liveValue` | Hilt `@Binds` / `@TestInstallIn` |
| `withDependencies { $0.x = Stub }` | コンストラクタに fake を直接渡す |
| Swift Testing `@Test("日本語意図")` | JUnit5 `@DisplayName` + `@Test` |
| `TestClock.advance(by:)` | `runTest` + `advanceTimeBy()` |
| スナップショットテスト | Roborazzi |

---

## Phase ロードマップ

| Phase | モジュール | 主なテーマ |
|---|---|---|
| 0 | `:build-logic`, 基盤設定 | Gradle マルチモジュール / convention plugin / KSP / Hilt 配線 |
| 1 | `:core:model`, `:core:network` | data class / sealed / Retrofit / DTO-Domain 分離 |
| 2 | `:core:domain` | Repository / Mutex キャッシュ / suspend / Flow / WeatherError |
| 3 | `:feature:weather-mvvm` | ViewModel / viewModelScope / StateFlow / ライフサイクル |
| 4 | `:feature:weather-mvi` | reducer / Channel / 純粋状態遷移 |
| 5 | `:app` 全画面 | Compose UI / Navigation 3 / Vico / ダークモード |
| Ex | `:core:ui` | デザインシステム / Tokens / strings.xml |

---

## ビルド・開発ルール

- アノテーション処理は **KSP のみ**。Kapt は追加しない
- DI は **Hilt**。Koin は使わない（KMP 検討フェーズまで保留）
- Navigation は **Navigation 3**。Nav2（文字列ルート）は使わない
- convention plugin を先に作ること。モジュールが増えてからでは遅い

---

## テスト方針

- 1 テスト 1 関心事（iOS の設計をそのまま継承）
- `StateFlow` の検証は **Turbine** を使う
- 非同期テストは **`runTest`**（仮想時間で debounce 検証可）
- 統合テストは `@TestInstallIn` で本番 Hilt モジュールを差し替える
- スクショ回帰は **Roborazzi**
