package com.doihei.weathernow.core.domain.location

// iOS の protocol LocationServiceProtocol に対応
interface LocationService {
    // iOS の func currentLocation() async throws -> CLLocationCoordinate2D に対応
    suspend fun currentLocation(): Result<Location>
}

// iOS の CLLocationCoordinate2D に対応するシンプルな値型
// :core:model には Android 依存が入らないのでここで定義する
data class Location(
    val latitude: Double,
    val longitude: Double,
)
