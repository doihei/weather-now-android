---
paths:
  - "**/*Test.kt"
---

# テスト設計原則（全モジュール共通）

## 基本方針

- **1 テスト 1 関心事**：1 つのテストメソッドが検証するのは 1 つの振る舞いだけ
- **`@DisplayName` 必須**：クラス・メソッド両方に日本語で「何を確認するか」を書く
  （iOS の `@Test("日本語意図")` に対応する約束事）
- **テストファイルのパッケージはテスト対象と揃える**
  - 正: `core/model/src/test/.../weather/WeatherCodeTest.kt`（`weather` パッケージ）
  - 誤: `core/model/src/test/.../WeatherCodeTest.kt`（ルートに直置き）

## フレームワーク用途別一覧

| フレームワーク | 用途 | iOS 対応 |
|---|---|---|
| JUnit `@Test` + `@DisplayName` | 通常のユニットテスト | Swift Testing `@Test("意図")` |
| JUnit `@ParameterizedTest` + `@MethodSource` | 値テーブルによる網羅テスト | `zip(inputs, expected)` |
| JUnit `@Nested` | 関連テストのグループ化 | 入れ子 `struct` |
| MockK `mockk<T>()` / `every` / `verify` | 依存の差し替えと呼び出し検証 | `withDependencies` + `CallCounter` |
| Turbine `flow.test { }` | `StateFlow` / `Flow` の値検証 | `AsyncStream` の検証 |
| `runTest` | suspend 関数・仮想時間テスト | `Swift Testing` の async コンテキスト |
| `advanceTimeBy()` | debounce / delay の仮想時間操作 | `TestClock.advance(by:)` |

## モジュール別テスト設計の概観

各モジュールのテスト設計は層ごとに異なる。詳細は各モジュールのルールファイルを参照。

| モジュール | モック | Flow 検証 | 主な特徴 |
|---|---|---|---|
| `:core:model` | 不要 | 不要 | 純粋値型・`@ParameterizedTest` |
| `:core:network` | 不要 | 不要 | Mapper 純粋関数・`@Nested`・境界値 |
| `:core:domain` | MockK | Turbine + `runTest` | Repository キャッシュ・Mutex 挙動 |
| `:feature:*` | `@TestInstallIn` | Turbine + `runTest` | ViewModel StateFlow / Channel |

## GitHub Actions CI

- `lint` ジョブ（ktlint + detekt）と `test` ジョブが並列実行される
- PR に新しいプッシュが来ると古い実行は自動キャンセルされる
- テスト失敗時のみ `build/reports/tests/` が Artifacts にアップロードされる
