package com.doihei.weathernow.core.network.dto

import kotlinx.serialization.Serializable

// iOS の GeocodingResponse（Decodable）に対応
@Serializable
data class GeocodingResponseDto(
    // iOS の let results: [ResultItem]?（null の場合は空配列扱い）に対応
    // Kotlin では null を明示するために ? を付ける
    val results: List<GeocodingResultItemDto>? = null,
)

// iOS の GeocodingResponse.ResultItem に対応
// iOS では CodingKeys が必要なかったので snake_case 変換も不要
@Serializable
data class GeocodingResultItemDto(
    val id: Int,
    val name: String,
    // iOS の let country: String?（null の場合は空文字扱い）に対応
    val country: String? = null,
    val latitude: Double,
    val longitude: Double,
)
