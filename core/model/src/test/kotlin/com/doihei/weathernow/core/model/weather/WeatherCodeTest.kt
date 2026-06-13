package com.doihei.weathernow.core.model.weather

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

// iOS の struct WeatherCodeTests に対応
// @ParameterizedTest + @MethodSource が iOS の zip パラメータ化に対応
@DisplayName("WeatherCode")
class WeatherCodeTest {
    companion object {
        // iOS の zip(codes, expected) に対応する @MethodSource データプロバイダ
        @JvmStatic
        fun knownWmoCodes(): Stream<Array<Any>> =
            Stream.of(
                arrayOf(0, WeatherCode.CLEAR_SKY),
                arrayOf(1, WeatherCode.MAINLY_CLEAR),
                arrayOf(2, WeatherCode.PARTLY_CLOUDY),
                arrayOf(3, WeatherCode.OVERCAST),
                arrayOf(45, WeatherCode.FOG),
                arrayOf(61, WeatherCode.LIGHT_RAIN),
                arrayOf(65, WeatherCode.HEAVY_RAIN),
                arrayOf(95, WeatherCode.THUNDERSTORM),
                arrayOf(99, WeatherCode.THUNDERSTORM_WITH_HEAVY_HAIL),
            )
    }

    @ParameterizedTest(name = "WMOコード {0} → {1}")
    @MethodSource("knownWmoCodes")
    @DisplayName("既知の WMO コードが正しく WeatherCode に変換される")
    fun `known WMO code maps to correct WeatherCode`(
        code: Int,
        expected: WeatherCode,
    ) {
        assertEquals(expected, WeatherCode.from(code))
    }

    @Test
    @DisplayName("未知の WMO コードは UNKNOWN にフォールバックする")
    fun `unknown WMO code falls back to UNKNOWN`() {
        assertEquals(WeatherCode.UNKNOWN, WeatherCode.from(9999))
        assertEquals(WeatherCode.UNKNOWN, WeatherCode.from(-999))
        assertEquals(WeatherCode.UNKNOWN, WeatherCode.from(Int.MAX_VALUE))
    }

    @Test
    @DisplayName("WMO コード -1 は UNKNOWN エントリ自身にマッチする")
    fun `wmo code minus one matches UNKNOWN entry itself`() {
        // UNKNOWN(-1) が from(-1) で自己参照されることを確認
        assertEquals(WeatherCode.UNKNOWN, WeatherCode.from(-1))
    }
}
