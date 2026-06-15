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

---

## Phase ロードマップ

進捗は README.md で一元管理しています。

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

## ルール参照

詳細なアーキテクチャルール・コーディング規約は `.claude/rules/` を参照。

- [アーキテクチャ設計原則](.claude/rules/architecture.md) — 依存方向・Mutex・状態 vs イベント・ビルドルール・テスト方針・iOS→Android マッピング
- [:core:model 層のルール](.claude/rules/core-model.md) — パッケージ構造・Android 依存禁止・ローカライズ文字列管理・MagicNumber
- [:core:network 層のルール](.claude/rules/core-network.md) — DTO設計・Retrofitインターフェース・マッパー・Hilt DI配線
- [:core:domain 層のルール](.claude/rules/core-domain.md) — Repository/Mutex キャッシュ・LocationService・UseCase・WeatherException 配置・@Binds vs @Provides
- [:feature:weather-mvvm 層のルール](.claude/rules/feature-weather-mvvm.md) — WeatherViewState・ViewModel パターン・二重ロード防止・Result.fold・依存宣言・テスト設計
- [:feature:weather-mvi 層のルール](.claude/rules/feature-weather-mvi.md) — State/Intent/SideEffect 設計・OnAppear 二重ロード防止・Channel vs StateFlow・テスト設計
- [:core:ui 層のルール](.claude/rules/core-ui.md) — コンポーネント設計・@Preview・文字列リソース管理・WeatherCodeRes・CoreUiR alias
