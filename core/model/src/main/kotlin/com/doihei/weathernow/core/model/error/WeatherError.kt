package com.doihei.weathernow.core.model.error

// iOS の enum WeatherError: Error, Sendable, Equatable に対応
// sealed interface にすると data object / data class で各 case を表現できる
// sealed class ではなく sealed interface にする理由は、
// 後で Exception と組み合わせたいケースでも多重継承できる柔軟性があるからです。
// WeatherNow の規模では差はほぼありませんが、2026年の慣習として sealed interface を選びます。
sealed interface WeatherError {
    data object LocationDenied : WeatherError

    data object LocationUnavailable : WeatherError

    data class NetworkFailure(
        val message: String,
    ) : WeatherError

    data object DecodingFailure : WeatherError

    data object CityLimitReached : WeatherError

    val userMessage: String
        get() =
            when (this) {
                is LocationDenied -> "位置情報の使用が許可されていません。設定アプリから許可してください。"
                is LocationUnavailable -> "位置情報を取得できませんでした。"
                is NetworkFailure -> "通信エラー: ${this.message}"
                is DecodingFailure -> "データの読み込みに失敗しました。"
                is CityLimitReached -> "登録できる都市は最大10件です。"
            }

    val isRetryable: Boolean
        get() =
            when (this) {
                is LocationDenied -> false
                is LocationUnavailable -> true
                is NetworkFailure -> true
                is DecodingFailure -> false
                is CityLimitReached -> false
            }
}
