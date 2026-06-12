package com.doihei.weathernow.core.network.api

// iOS の OpenMeteoEndpoint enum の固定クエリ定義に対応
// Retrofit の interface はデフォルト引数を @Query と組み合わせできないため、
// 定数として切り出してリポジトリ層から渡す
object OpenMeteoQueryDefaults {
    const val CURRENT = "temperature_2m,apparent_temperature,relativehumidity_2m,weathercode,windspeed_10m"
    const val HOURLY = "temperature_2m,precipitation,weathercode"
    const val DAILY = "temperature_2m_max,temperature_2m_min,precipitation_probability_max,weathercode"
    const val TIMEZONE = "auto"
    const val FORECAST_DAYS = 7
    const val GEOCODING_COUNT = 10
    const val GEOCODING_LANGUAGE = "ja"
}
