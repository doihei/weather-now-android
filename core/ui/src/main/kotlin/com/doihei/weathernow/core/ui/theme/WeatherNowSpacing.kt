package com.doihei.weathernow.core.ui.theme

import androidx.compose.ui.unit.dp

// iOS の enum AppSpacing { static let xs: CGFloat = 4 } に対応
// Compose では object + Dp プロパティとして定義する
//
// dimens.xml と値は同じだが Token を優先する理由：
//   - Kotlin コードだけで完結し Context が不要（テスト・Preview ともに動く）
//   - IDE の補完で名前が出る（R.dimen.spacing_lg より発見しやすい）
//   - 将来 CompositionLocal でオーバーライドできる拡張性がある
//
// dimens.xml は端末サイズ修飾子（-sw600dp 等）が必要なときだけ使う
object WeatherNowSpacing {
    // iOS の AppSpacing.xxxSmall に対応
    val xs = 4.dp // 最小余白（気温と体感温度の間など）
    val sm = 8.dp // 小さな余白・アイコン周辺
    val md = 12.dp // リストアイテム間・区切り線前後
    val lg = 16.dp // 画面端マージン・セクション間（最もよく使う）
    val xl = 24.dp // 大きなセクション余白・エラー画面
    val card = 20.dp // Card 内パディング専用
}
