package com.doihei.weathernow.feature.weather.mvi.currentweather

// iOS の enum Action に対応
// TCA の Action は「ユーザー操作」と「内部イベント（API 応答など）」を両方含むが、
// 自前 MVI では「ユーザーが起こせる意図」のみを Intent として定義する
// 内部イベントは ViewModel の中で直接処理する
sealed interface CurrentWeatherIntent {
    // iOS の case onAppear に対応
    // 画面が表示されたときに天気を読み込む
    data object OnAppear : CurrentWeatherIntent

    // iOS の case refreshButtonTapped に対応
    data object Refresh : CurrentWeatherIntent

    // iOS の case retryButtonTapped に対応
    // Error 状態から再試行する（OnAppear と同じ処理だが意図を明示する）
    data object Retry : CurrentWeatherIntent
}
