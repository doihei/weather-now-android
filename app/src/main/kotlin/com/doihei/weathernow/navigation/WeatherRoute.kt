package com.doihei.weathernow.navigation

// iOS の enum WeatherPath に対応
// Navigation 3 ではルートを sealed class / data class / data object で表現する
// Nav2 の「文字列ルート」と違い、型安全にパラメータを渡せる
// TCA の StackState<WeatherPath.State> の WeatherPath に対応
sealed interface WeatherRoute {

    // メイン画面（現在地の天気）
    // iOS の case currentWeather に対応
    // MVVM 実装を表示する
    data object CurrentWeatherMvvm : WeatherRoute

    // メイン画面（MVI 実装版）
    // iOS の TCA Feature 版と同様の比較実装
    data object CurrentWeatherMvi : WeatherRoute

    // 将来の拡張例（Phase 5 後半で追加予定）
    // data class WeeklyForecast(val cityId: Int) : WeatherRoute
    // data class CitySearch : WeatherRoute
}
