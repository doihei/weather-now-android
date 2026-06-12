package com.doihei.weathernow.core.model.city

// iOS の struct City: Sendable, Equatable, Identifiableに対応
data class City(
    val id: Int,
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
)
