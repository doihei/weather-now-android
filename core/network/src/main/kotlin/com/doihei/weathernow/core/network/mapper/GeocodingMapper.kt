package com.doihei.weathernow.core.network.mapper

import com.doihei.weathernow.core.model.city.GeocodingResult
import com.doihei.weathernow.core.network.dto.GeocodingResponseDto

// iOS の extension GeocodingResponse { func toResults() -> [GeocodingResult] } に対応
fun GeocodingResponseDto.toResults(): List<GeocodingResult> =
    // iOS の (results ?? []).map { ... } に対応
    (results ?: emptyList()).map { item ->
        GeocodingResult(
            id = item.id,
            name = item.name,
            // iOS の item.country ?? "" に対応
            country = item.country.orEmpty(),
            latitude = item.latitude,
            longitude = item.longitude,
        )
    }
