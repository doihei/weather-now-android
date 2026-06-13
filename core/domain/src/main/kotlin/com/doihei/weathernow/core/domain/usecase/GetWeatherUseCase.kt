package com.doihei.weathernow.core.domain.usecase

import com.doihei.weathernow.core.domain.location.LocationService
import com.doihei.weathernow.core.domain.repository.WeatherRepository
import com.doihei.weathernow.core.model.weather.Weather
import javax.inject.Inject

// iOS の WeatherDomain が公開していた「現在地の天気を取得する」ユースケースに対応
// Repository + LocationService を組み合わせる調整役
class GetWeatherUseCase
    @Inject
    constructor(
        private val repository: WeatherRepository,
        private val locationService: LocationService,
    ) {
        // iOS の func execute() async throws -> Weather に対応
        // Result<T> で返すことで呼び出し側の when ハンドリングを強制する
        suspend operator fun invoke(): Result<Weather> {
            // Step 1：位置情報の取得
            val locationResult = locationService.currentLocation()
            val location = locationResult.getOrElse { return Result.failure(it) }

            // Step 2：天気の取得（位置情報が取れた場合のみ）
            return repository.fetchWeather(
                latitude = location.latitude,
                longitude = location.longitude,
            )
        }
    }
