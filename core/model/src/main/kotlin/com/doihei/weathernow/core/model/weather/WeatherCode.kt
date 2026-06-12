package com.doihei.weathernow.core.model.weather

@Suppress("MagicNumber")
enum class WeatherCode(
    val wmoCode: Int,
) {
    CLEAR_SKY(0),
    MAINLY_CLEAR(1),
    PARTLY_CLOUDY(2),
    OVERCAST(3),
    FOG(45),
    RIME_FOG(48),
    LIGHT_DRIZZLE(51),
    MODERATE_DRIZZLE(53),
    DENSE_DRIZZLE(55),
    LIGHT_RAIN(61),
    MODERATE_RAIN(63),
    HEAVY_RAIN(65),
    LIGHT_SNOW(71),
    MODERATE_SNOW(73),
    HEAVY_SNOW(75),
    SNOW_GRAINS(77),
    LIGHT_RAIN_SHOWER(80),
    MODERATE_RAIN_SHOWER(81),
    VIOLENT_RAIN_SHOWER(82),
    LIGHT_SNOW_SHOWER(85),
    HEAVY_SNOW_SHOWER(86),
    THUNDERSTORM(95),
    THUNDERSTORM_WITH_HAIL(96),
    THUNDERSTORM_WITH_HEAVY_HAIL(99),
    UNKNOWN(-1),
    ;

    companion object {
        fun from(code: Int): WeatherCode = entries.firstOrNull { it.wmoCode == code } ?: UNKNOWN
    }
}
