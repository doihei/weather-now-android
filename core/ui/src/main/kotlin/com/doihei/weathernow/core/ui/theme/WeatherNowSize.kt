package com.doihei.weathernow.core.ui.theme

import androidx.compose.ui.unit.dp

// iOS の AppSize / AppIconSize 相当
// コンポーネントの「大きさ」に関する定数をスペーシングと分離して管理する
//
// 分離する理由：
//   Spacing = 「要素間の距離」（余白・パディング）
//   Size    = 「要素自身の大きさ」（高さ・幅・アイコンサイズ）
//   iOS の AppSpacing vs AppIconSize / AppComponentSize の分離と同じ
object WeatherNowSize {
    // グラフ
    val chartHourlyHeight = 200.dp // HourlyForecastChart の高さ

    // アイコン（将来 TopAppBar のアイコンサイズ統一に使う）
    val iconMd = 24.dp
    val iconLg = 32.dp
}
