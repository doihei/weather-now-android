package com.doihei.weathernow.feature.weather.mvvm.currentweather

import com.doihei.weathernow.core.model.weather.Weather

// iOS の enum ViewState に対応
// sealed interface にすることで when の網羅チェックが効く
// （iOS の switch で全 case を書かないとコンパイルエラーになるのと同じ安全性）
sealed interface WeatherViewState {
    // iOS の case idle に対応
    // 初期状態。画面が表示される前
    data object Idle : WeatherViewState

    // iOS の case loading に対応
    data object Loading : WeatherViewState

    // iOS の case loaded(Weather) に対応
    // data class にすることで weather フィールドを持てる
    data class Loaded(
        val weather: Weather,
    ) : WeatherViewState

    // iOS の case error(String) に対応
    data class Error(
        val message: String,
    ) : WeatherViewState
}
