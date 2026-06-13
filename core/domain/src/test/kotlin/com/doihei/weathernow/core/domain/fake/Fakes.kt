package com.doihei.weathernow.core.domain.fake

import com.doihei.weathernow.core.domain.location.Location
import com.doihei.weathernow.core.domain.location.LocationService
import com.doihei.weathernow.core.domain.repository.WeatherRepository
import com.doihei.weathernow.core.model.city.GeocodingResult
import com.doihei.weathernow.core.model.weather.CurrentWeather
import com.doihei.weathernow.core.model.weather.Weather
import com.doihei.weathernow.core.model.weather.WeatherCode

// iOS の withDependencies { $0.weatherRepository = FakeRepository() } に対応
// コンストラクタに直接 fake を渡すパターン（ユニットテストでは @TestInstallIn 不要）

// ---- テスト用ドメインモデルファクトリ ----

// 最小構成の Weather を生成するヘルパー
// テストごとに関心のあるフィールドだけ変えられるようにデフォルト値を持たせる
fun makeWeather(
    temperature: Double = 20.0,
    code: WeatherCode = WeatherCode.CLEAR_SKY,
) = Weather(
    current =
        CurrentWeather(
            temperature = temperature,
            feelsLike = 18.0,
            humidity = 60,
            windSpeed = 10.0,
            code = code,
        ),
    hourly = emptyList(),
    daily = emptyList(),
)

fun makeLocation(
    latitude: Double = 35.6895,
    longitude: Double = 139.6917,
) = Location(latitude = latitude, longitude = longitude)

// ---- FakeLocationService ----

// iOS の LocationServiceKey.testValue に対応
class FakeLocationService(
    private val result: Result<Location> = Result.success(makeLocation()),
) : LocationService {
    // 呼び出し回数を記録する（iOS の CallCounter actor に対応）
    var callCount = 0
        private set

    override suspend fun currentLocation(): Result<Location> {
        callCount++
        return result
    }
}

// ---- FakeWeatherRepository ----

// iOS の WeatherRepositoryKey.testValue に対応
class FakeWeatherRepository(
    private val fetchResult: Result<Weather> = Result.success(makeWeather()),
    private val searchResult: Result<List<GeocodingResult>> = Result.success(emptyList()),
) : WeatherRepository {
    var fetchCallCount = 0
        private set
    var lastFetchLatitude: Double? = null
        private set
    var lastFetchLongitude: Double? = null
        private set

    override suspend fun fetchWeather(
        latitude: Double,
        longitude: Double,
    ): Result<Weather> {
        fetchCallCount++
        lastFetchLatitude = latitude
        lastFetchLongitude = longitude
        return fetchResult
    }

    override suspend fun searchCities(name: String): Result<List<GeocodingResult>> = searchResult
}
