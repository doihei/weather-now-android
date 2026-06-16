package com.doihei.weathernow.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// ──────────────────────────────────────────────────────────────
// ライトカラースキーム
//
// iOS の .light トレイトコレクションに対応
// Material3 では lightColorScheme の名前付き引数で各ロールを指定する
// 指定しないロールはデフォルト値が使われる（全部定義する必要はない）
// ──────────────────────────────────────────────────────────────
private val LightColorScheme =
    lightColorScheme(
        // Primary：メインブランドカラー（ボタン・アクティブ状態・FAB）
        // iOS の Color.accentColor / tint に対応
        primary = Sky40,
        onPrimary = Sky10, // primary の上のテキスト・アイコン色
        primaryContainer = Sky90, // 低強調コンテナ（チップ等）
        onPrimaryContainer = Sky100,
        // Secondary：補助カラー（フィルターチップ等）
        secondary = Indigo40,
        onSecondary = Indigo10,
        secondaryContainer = Indigo90,
        onSecondaryContainer = Indigo100,
        // Tertiary：アクセントカラー（対比・強調）
        tertiary = Amber40,
        onTertiary = Amber10,
        tertiaryContainer = Amber90,
        onTertiaryContainer = Amber100,
        // 背景・サーフェス
        // iOS の Color(.systemBackground) / Color(.secondarySystemBackground) に対応
        background = Neutral99,
        onBackground = Neutral10,
        surface = Neutral99,
        onSurface = Neutral10,
        surfaceVariant = Neutral90,
        onSurfaceVariant = Neutral30,
    )

// ──────────────────────────────────────────────────────────────
// ダークカラースキーム
//
// iOS の .dark トレイトコレクションに対応
// ダークモードでは primary を明るくして視認性を確保する
// （iOS の Color(.label) が自動でライト/ダーク切り替えするのと同じ考え方）
// ──────────────────────────────────────────────────────────────
private val DarkColorScheme =
    darkColorScheme(
        primary = SkyDark80, // ダークモードでは薄い青で視認性確保
        onPrimary = SkyDark20,
        primaryContainer = SkyDark30,
        onPrimaryContainer = SkyDark90,
        secondary = IndigoDark80,
        onSecondary = IndigoDark20,
        secondaryContainer = IndigoDark30,
        onSecondaryContainer = IndigoDark90,
        tertiary = AmberDark80,
        onTertiary = AmberDark20,
        tertiaryContainer = AmberDark30,
        onTertiaryContainer = AmberDark90,
        background = Neutral10,
        onBackground = Neutral99,
        surface = Neutral10,
        onSurface = Neutral99,
        surfaceVariant = Neutral30,
        onSurfaceVariant = Neutral90,
    )

// ──────────────────────────────────────────────────────────────
// WeatherNowTheme
//
// iOS の .preferredColorScheme(.light/.dark) / @Environment(\.colorScheme) に対応
// isSystemInDarkTheme() がシステム設定を読む
//
// 呼び出し方：
//   WeatherNowTheme { /* コンテンツ */ }
//   WeatherNowTheme(darkTheme = true) { /* 強制ダーク（Preview 用）*/ }
// ──────────────────────────────────────────────────────────────
@Composable
fun WeatherNowTheme(
    // isSystemInDarkTheme()：iOS の @Environment(\.colorScheme) == .dark に対応
    // デフォルトはシステム設定に追従。Preview では引数で上書きできる
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // iOS の ColorScheme の条件分岐そのまま：
    //   if colorScheme == .dark { DarkColors } else { LightColors }
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        // typography / shapes はデフォルトのまま（Phase Ex で拡張予定）
        content = content,
    )
}
