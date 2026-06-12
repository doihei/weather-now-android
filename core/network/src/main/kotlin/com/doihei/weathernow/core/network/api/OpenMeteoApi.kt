package com.doihei.weathernow.core.network.api

import com.doihei.weathernow.core.network.dto.ForecastResponseDto
import com.doihei.weathernow.core.network.dto.GeocodingResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApi {
    // iOS の OpenMeteoEndpoint.forecast(latitude:longitude:) に対応
    @Suppress("LongParameterList") // Retrofit @Query はAPI仕様由来のため抑制
    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        // iOS の Endpoint 内の固定クエリをデフォルト引数で表現
        // デフォルト引数は Kotlin の interface では使えないため、呼び出し側で渡す
        @Query("current") current: String,
        @Query("hourly") hourly: String,
        @Query("daily") daily: String,
        @Query("timezone") timezone: String,
        @Query("forecast_days") forecastDays: Int,
    ): ForecastResponseDto

    // iOS の OpenMeteoEndpoint.geocoding(name:count:) に対応
    @GET("v1/search")
    suspend fun searchCities(
        @Query("name") name: String,
        @Query("count") count: Int,
        @Query("language") language: String,
    ): GeocodingResponseDto
}
