package com.doihei.weathernow.core.network.mapper

import com.doihei.weathernow.core.network.dto.GeocodingResponseDto
import com.doihei.weathernow.core.network.dto.GeocodingResultItemDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("GeocodingMapper")
class GeocodingMapperTest {
    // ---- 正常系 ----

    @Nested
    @DisplayName("GeocodingResult リストへの変換")
    inner class ToResultsConversion {
        @Test
        @DisplayName("results が存在するとき全件 GeocodingResult に変換される")
        fun `results present converts all items`() {
            val dto =
                GeocodingResponseDto(
                    results =
                        listOf(
                            GeocodingResultItemDto(
                                id = 1850147,
                                name = "Tokyo",
                                country = "Japan",
                                latitude = 35.6895,
                                longitude = 139.6917,
                            ),
                            GeocodingResultItemDto(
                                id = 2147714,
                                name = "Sydney",
                                country = "Australia",
                                latitude = -33.8688,
                                longitude = 151.2093,
                            ),
                        ),
                )

            val results = dto.toResults()

            assertEquals(2, results.size)
            assertEquals(1850147, results[0].id)
            assertEquals("Tokyo", results[0].name)
            assertEquals("Japan", results[0].country)
            assertEquals(35.6895, results[0].latitude)
            assertEquals(139.6917, results[0].longitude)
        }

        @Test
        @DisplayName("results が null のとき空リストを返す")
        fun `null results returns empty list`() {
            // iOS の results ?? [] に対応
            val dto = GeocodingResponseDto(results = null)

            val results = dto.toResults()

            assertEquals(0, results.size)
        }

        @Test
        @DisplayName("results が空リストのとき空リストを返す")
        fun `empty results returns empty list`() {
            val dto = GeocodingResponseDto(results = emptyList())

            val results = dto.toResults()

            assertEquals(0, results.size)
        }
    }

    // ---- null フォールバック ----

    @Nested
    @DisplayName("null フィールドのフォールバック")
    inner class NullFallback {
        @Test
        @DisplayName("country が null のとき空文字にフォールバックする")
        fun `null country falls back to empty string`() {
            // iOS の item.country ?? "" に対応
            // Kotlin の .orEmpty() の動作を確認する
            val dto =
                GeocodingResponseDto(
                    results =
                        listOf(
                            GeocodingResultItemDto(
                                id = 1,
                                name = "Unknown City",
                                country = null, // ← null を明示的にテスト
                                latitude = 0.0,
                                longitude = 0.0,
                            ),
                        ),
                )

            val results = dto.toResults()

            assertEquals("", results[0].country)
        }

        @Test
        @DisplayName("country が存在するとき値がそのまま使われる")
        fun `present country is preserved`() {
            val dto =
                GeocodingResponseDto(
                    results =
                        listOf(
                            GeocodingResultItemDto(
                                id = 1,
                                name = "Tokyo",
                                country = "Japan",
                                latitude = 35.6895,
                                longitude = 139.6917,
                            ),
                        ),
                )

            val results = dto.toResults()

            assertEquals("Japan", results[0].country)
        }
    }

    // ---- GeocodingResult → City 変換の連鎖 ----

    @Nested
    @DisplayName("GeocodingResult.toCity() との連鎖")
    inner class ToCityChain {
        @Test
        @DisplayName("toResults().map { toCity() } で City リストが得られる")
        fun `toResults then toCity produces city list`() {
            val dto =
                GeocodingResponseDto(
                    results =
                        listOf(
                            GeocodingResultItemDto(
                                id = 1850147,
                                name = "Tokyo",
                                country = "Japan",
                                latitude = 35.6895,
                                longitude = 139.6917,
                            ),
                        ),
                )

            val cities = dto.toResults().map { it.toCity() }

            assertEquals(1, cities.size)
            assertEquals(1850147, cities[0].id)
            assertEquals("Tokyo", cities[0].name)
        }
    }
}
