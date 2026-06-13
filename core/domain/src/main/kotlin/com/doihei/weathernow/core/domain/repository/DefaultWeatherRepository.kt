package com.doihei.weathernow.core.domain.repository

import com.doihei.weathernow.core.domain.exception.WeatherException
import com.doihei.weathernow.core.model.city.GeocodingResult
import com.doihei.weathernow.core.model.error.WeatherError
import com.doihei.weathernow.core.model.weather.Weather
import com.doihei.weathernow.core.network.api.OpenMeteoApi
import com.doihei.weathernow.core.network.api.OpenMeteoQueryDefaults
import com.doihei.weathernow.core.network.mapper.toResults
import com.doihei.weathernow.core.network.mapper.toWeather
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

// iOS の actor WeatherRepository: WeatherRepositoryProtocol に対応
// Kotlin に actor はないため Mutex で排他を手書きする
// architecture.md の最重要ルール：withLock の中で suspend を呼ばない
class DefaultWeatherRepository
    @Inject
    constructor(
        private val api: OpenMeteoApi,
    ) : WeatherRepository {
        // iOS の cache: [String: (weather: Weather, cachedAt: Date)] に対応
        // mutableMapOf は共有可変状態なので Mutex で保護する
        private val mutex = Mutex()
        private val cache = mutableMapOf<String, CachedEntry>()

        companion object {
            private const val CACHE_DURATION_MINUTES = 10L
        }

        override suspend fun fetchWeather(
            latitude: Double,
            longitude: Double,
        ): Result<Weather> {
            val key = cacheKey(latitude, longitude)

            // Step 1：キャッシュ確認（ロック区間を最小化）
            // iOS の actor ではこの読み取りが暗黙的に保護されていたが、
            // Kotlin では withLock を明示的に書く
            mutex.withLock {
                cache[key]?.let {
                    if (it.isValid(CACHE_DURATION_MINUTES)) return Result.success(it.weather)
                }
            }

            // Step 2：ネットワーク取得（ロックの外で実行）
            // ここが最重要：withLock の中で suspend（ネットワーク）を呼ばない
            // 理由：Mutex は再入不可なので、ロック中に suspend すると
            // 別コルーチンが永遠に待ち続けるデッドロック的状況になりうる
            val result =
                runCatching {
                    api
                        .forecast(
                            latitude = latitude,
                            longitude = longitude,
                            current = OpenMeteoQueryDefaults.CURRENT,
                            hourly = OpenMeteoQueryDefaults.HOURLY,
                            daily = OpenMeteoQueryDefaults.DAILY,
                            timezone = OpenMeteoQueryDefaults.TIMEZONE,
                            forecastDays = OpenMeteoQueryDefaults.FORECAST_DAYS,
                        ).toWeather()
                }.mapFailure { it.toWeatherError() }

            // Step 3：取得成功時のみキャッシュ書き込み（再びロック）
            result.onSuccess { weather ->
                mutex.withLock {
                    cache[key] = CachedEntry(weather = weather, cachedAt = Instant.now())
                }
            }
            return result
        }

        override suspend fun searchCities(name: String): Result<List<GeocodingResult>> =
            runCatching {
                api
                    .searchCities(
                        name = name,
                        count = OpenMeteoQueryDefaults.GEOCODING_COUNT,
                        language = OpenMeteoQueryDefaults.GEOCODING_LANGUAGE,
                    ).toResults()
            }.mapFailure { it.toWeatherError() }

        // ---- キャッシュキー生成 ----

        // iOS の func cacheKey(latitude:longitude:) -> String に対応
        // 小数点以下4桁で丸めることで近隣座標のキャッシュヒット率を上げる
        private fun cacheKey(
            latitude: Double,
            longitude: Double,
        ): String {
            val lat = "%.4f".format(latitude)
            val lon = "%.4f".format(longitude)
            return "${lat}_$lon"
        }

        // ---- キャッシュエントリ ----

        // iOS の (weather: Weather, cachedAt: Date) タプルに対応
        // Kotlin ではデータクラスで表現する
        private data class CachedEntry(
            val weather: Weather,
            val cachedAt: Instant,
        ) {
            fun isValid(durationMinutes: Long): Boolean =
                cachedAt
                    .plus(durationMinutes, ChronoUnit.MINUTES)
                    .isAfter(Instant.now())
        }
    }

// ---- 拡張関数：Throwable → WeatherError 変換 ----

// iOS の catch let error の分岐ロジックに対応
// ネットワーク例外を WeatherError.NetworkFailure に変換する
private fun Throwable.toWeatherError(): WeatherError =
    WeatherError.NetworkFailure(message = this.message ?: "Unknown error")

// Result<T> の mapFailure（標準ライブラリにないため自前定義）
// iOS の .mapError { } に対応
private fun <T> Result<T>.mapFailure(transform: (Throwable) -> WeatherError): Result<T> =
    fold(
        onSuccess = { Result.success(it) },
        onFailure = { Result.failure(WeatherException(transform(it))) },
    )
