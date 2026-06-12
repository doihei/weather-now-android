package com.doihei.weathernow.core.network.di

import com.doihei.weathernow.core.network.api.OpenMeteoApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

// iOS の DependencyKey.liveValue として LiveWeatherAPIClient を生成していた部分に対応
// Hilt @Module で Retrofit のインスタンスを SingletonComponent に登録する
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    // iOS の「ベース URL をクライアント実装に直書きしない」規約に対応
    // Hilt で一元管理することで全モジュールから参照できる
    private const val FORECAST_BASE_URL = "https://api.open-meteo.com/"
    private const val GEOCODING_BASE_URL = "https://geocoding-api.open-meteo.com/"

    // kotlinx.serialization の設定
    // iOS の JSONDecoder() に対応。ignoreUnknownKeys = true は
    // API が将来フィールドを追加しても壊れないようにする（iOS の Decodable デフォルト動作と同じ）
    private val json =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true // null → デフォルト値への強制変換
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient
            .Builder()
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    // デバッグビルドのみログを出す（本番ビルドで自動的に NONE になる）
                    level = HttpLoggingInterceptor.Level.BODY
                },
            ).build()

    // 天気 API 用の Retrofit インスタンス（baseUrl が異なるため2つ用意）
    @Provides
    @Singleton
    @ForecastRetrofit // 後述の Qualifier
    fun provideForecastRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(FORECAST_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    // 都市検索 API 用の Retrofit インスタンス
    @Provides
    @Singleton
    @GeocodingRetrofit // 後述の Qualifier
    fun provideGeocodingRetrofit(okHttpClient: OkHttpClient): Retrofit =
        Retrofit
            .Builder()
            .baseUrl(GEOCODING_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    // OpenMeteoApi（天気）の実装を生成して提供
    @Provides
    @Singleton
    fun provideForecastApi(
        @ForecastRetrofit retrofit: Retrofit,
    ): OpenMeteoApi = retrofit.create(OpenMeteoApi::class.java)
}
