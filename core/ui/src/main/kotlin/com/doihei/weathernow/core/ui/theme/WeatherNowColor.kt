@file:Suppress("MagicNumber")

package com.doihei.weathernow.core.ui.theme

import androidx.compose.ui.graphics.Color

// ──────────────────────────────────────────────────────────────
// WeatherNow カラーパレット
//
// iOS の extension Color { static let skyBlue = ... } に対応
// ただし Material3 のルール：
//   - ライト / ダーク それぞれのトークンを定義する
//   - `on*` は対応する色の上に載せるテキスト・アイコンの色
//   - `*Container` は低強調の背景色
//   - `on*Container` はコンテナの上に載せるテキスト色
//
// 天気アプリのコンセプト：
//   Primary   = 空の青（Sky Blue）  ← 晴れの代表色
//   Secondary = インディゴ（Indigo） ← 夜空・グラフ線
//   Tertiary  = アンバー（Amber）   ← 日の出・日照
//   Error     = Material 標準の赤をそのまま使用
// ──────────────────────────────────────────────────────────────

// ---- ライトテーマ ----
// iOS の Color(.systemBackground) = 白系の前提でチューニング

internal val Sky40 = Color(0xFF0A84FF) // iOS systemBlue に近い明るい青
internal val Sky10 = Color(0xFF002D6B) // ダーク版 onPrimary 用の濃紺
internal val Sky90 = Color(0xFFD6E4FF) // primaryContainer（薄い空色）
internal val Sky100 = Color(0xFF001B41) // onPrimaryContainer

internal val Indigo40 = Color(0xFF5856D6) // iOS systemIndigo
internal val Indigo10 = Color(0xFF14005C)
internal val Indigo90 = Color(0xFFE4DFFF) // secondaryContainer
internal val Indigo100 = Color(0xFF200068)

internal val Amber40 = Color(0xFFA05C00) // iOS systemOrange 寄り
internal val Amber10 = Color(0xFF321200)
internal val Amber90 = Color(0xFFFFDCBE) // tertiaryContainer（温かみ）
internal val Amber100 = Color(0xFF4D1C00)

// ---- ダークテーマ ----
// iOS の Color(.systemBackground) = 黒系の前提

internal val SkyDark80 = Color(0xFFAAC8FF) // ダークモードの primary（薄くして視認性確保）
internal val SkyDark20 = Color(0xFF00438A) // ダークモードの onPrimary
internal val SkyDark30 = Color(0xFF0061B8) // ダークモードの primaryContainer
internal val SkyDark90 = Color(0xFFD6E4FF) // ダークモードの onPrimaryContainer

internal val IndigoDark80 = Color(0xFFC5BFFF)
internal val IndigoDark20 = Color(0xFF2C0093)
internal val IndigoDark30 = Color(0xFF4238C9)
internal val IndigoDark90 = Color(0xFFE4DFFF)

internal val AmberDark80 = Color(0xFFFFB870)
internal val AmberDark20 = Color(0xFF5A2900)
internal val AmberDark30 = Color(0xFF7B3F00)
internal val AmberDark90 = Color(0xFFFFDCBE)

// ---- ニュートラル（背景・サーフェス）----
// iOS の systemGroupedBackground / secondarySystemBackground 相当

internal val Neutral99 = Color(0xFFFCFCFF) // ライトモード背景
internal val Neutral10 = Color(0xFF1A1C1E) // ダークモード背景
internal val Neutral90 = Color(0xFFE2E2E6) // ライト surfaceVariant
internal val Neutral30 = Color(0xFF46464A) // ダーク surfaceVariant
