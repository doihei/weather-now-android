package com.doihei.weathernow.core.domain.repository

import com.doihei.weathernow.core.model.city.GeocodingResult
import com.doihei.weathernow.core.model.weather.Weather

// iOS の protocol WeatherRepositoryProtocol に対応
// interface で定義することで Hilt のテスト差し替えが効く
// （iOS の withDependencies { $0.weatherRepository = StubRepository() } に対応）
interface WeatherRepository {
    // iOS の func fetchWeather(latitude:longitude:) async throws -> Weather に対応
    // Kotlin では throws を使わず Result<T> で返す（例外を型として表現）
    suspend fun fetchWeather(
        latitude: Double,
        longitude: Double,
    ): Result<Weather>

    // iOS の func searchCities(name:) async throws -> [GeocodingResult] に対応
    suspend fun searchCities(name: String): Result<List<GeocodingResult>>
}
