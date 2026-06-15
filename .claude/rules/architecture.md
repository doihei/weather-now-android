# アーキテクチャ設計原則

## 依存方向

- 依存は下向きのみ：`:feature` → `:core:domain` → `:core:model`
- `:feature` は `:core:network` を直接参照しない

## Gradle 依存の伝播ルール（重要）

`:core:domain` は `:core:model` を `implementation` で宣言しているため、`:feature` モジュールに自動伝播しない。
`:feature` で `Weather` など `:core:model` の型を直接参照する場合は `build.gradle.kts` に明示する。

```kotlin
// feature/weather-mvvm/build.gradle.kts
dependencies {
    implementation(project(":core:model"))   // 自動伝播しないため明示必要
    implementation(project(":core:domain"))
}
```

- `api()` への変更は `:core:domain` の内部実装を全 feature に漏洩させるため行わない
- `:feature` 側の明示宣言が正しいアプローチ

## インターフェース境界

- Repository・LocationService は `interface` で定義し、実装を Hilt で注入する
- DTO（`@Serializable`）を UI まで持ち回らない。必ず Domain モデルに変換する

## Actor の不在（最重要）

- Kotlin に Actor はない。共有可変状態には **`Mutex`** を使う
- `mutex.withLock` の中で `suspend`（ネットワーク等）を呼ばない。ロック区間を最小化する
- `@Synchronized` は使わない（コルーチンをスレッドブロックする）

## 状態 vs イベント

- 画面状態（loading / loaded / error）→ `StateFlow`
- 一度きりの出来事（スナックバー・画面遷移）→ `Channel.receiveAsFlow()`
- 混同すると画面回転のたびにダイアログが再表示されるバグになる

## MVVM と MVI の並存

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
| Swift Testing `@Test("日本語意図")` | JUnit `@DisplayName` + `@Test` |
| `TestClock.advance(by:)` | `runTest` + `advanceTimeBy()` |
| スナップショットテスト | Roborazzi |

---

## ビルド・開発ルール

- アノテーション処理は **KSP のみ**。Kapt は追加しない
- DI は **Hilt**。Koin は使わない（KMP 検討フェーズまで保留）
- Navigation は **Navigation 3**。Nav2（文字列ルート）は使わない
- convention plugin を先に作ること。モジュールが増えてからでは遅い
- Kotlin バージョンは **KSP バージョンに合わせる**。KSP `2.3.9` は Kotlin `2.3.x` のみ対応

## Linter 設定

- **ktlint**: `.editorconfig` でルール設定
  - `ktlint_function_naming_ignore_when_annotated_with = Composable`
    （`@Composable` 関数は PascalCase が公式規約。ktlint のデフォルトと衝突するため除外）
- **detekt**: `config/detekt/detekt.yml` でカスタムルール管理。ルート `build.gradle.kts` の `subprojects {}` ブロックで全モジュールに自動適用
  - `FunctionNaming`: `@Composable` を除外（ktlint と同じ理由）
  - `LongMethod`: しきい値 80（Compose の Scaffold + 状態収集パターンは構造上行数が増えるため緩和）
  - `UnusedPrivateMember`: `@Preview` を除外（Android Studio からのみ参照されるため）

---

## テスト方針

- 1 テスト 1 関心事（iOS の設計をそのまま継承）
- `StateFlow` の検証は **Turbine** を使う
- 非同期テストは **`runTest`**（仮想時間で debounce 検証可）
- 統合テストは `@TestInstallIn` で本番 Hilt モジュールを差し替える
- スクショ回帰は **Roborazzi**
