package com.doihei.weathernow.core.model.city

data class GeocodingResult(
    val id: Int,
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
) {
    // iOS の func toCity() -> City に対応
    fun toCity(): City =
        City(
            id = id,
            name = name,
            country = country,
            latitude = latitude,
            longitude = longitude,
        )
}
