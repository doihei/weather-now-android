package com.doihei.weathernow.core.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// iOS の AppTypography / Font.system(size:weight:design:) に対応
//
// FontFamily.Default = Roboto（Android システムフォント）
//   iOS の SF Pro（システムフォント）と同じ「ライセンスフリーのシステムフォントを使う」戦略
//   カスタムフォント（Google Fonts 等）を導入するときはここだけ変更する
//
// 現在使用しているスタイルのみ明示定義し、それ以外は Material3 デフォルトに委ねる
object WeatherNowTypography {
    // 気温などの主要数値（WeatherLoadedView の displayLarge）
    // iOS の Font.system(.largeTitle, weight: .thin) に近い
    val displayLarge =
        TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Light,
            fontSize = 57.sp,
            lineHeight = 64.sp,
            letterSpacing = (-0.25).sp,
        )

    // エラー画面の絵文字アイコン（WeatherErrorView の displayMedium）
    val displayMedium =
        TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 45.sp,
            lineHeight = 52.sp,
            letterSpacing = 0.sp,
        )

    // 天気コード説明文（WeatherLoadedView の titleLarge）
    // iOS の Font.system(.title, weight: .regular) に近い
    val titleLarge =
        TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp,
        )

    // セクション見出し（「1時間ごとの気温」「7日間の予報」）
    // iOS の Font.system(.headline, weight: .medium) に近い
    val titleMedium =
        TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp,
        )

    // エラーメッセージ本文
    // iOS の Font.system(.body) に対応
    val bodyLarge =
        TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp,
        )

    // 曜日・気温範囲・ラベル値
    // iOS の Font.system(.subheadline) に近い
    val bodyMedium =
        TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp,
        )

    // 予報の天気説明（細かい補足情報）
    // iOS の Font.system(.caption) に対応
    val bodySmall =
        TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.4.sp,
        )

    // 湿度・風速のラベル
    // iOS の Font.system(.caption2) に対応
    val labelSmall =
        TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
        )
}
