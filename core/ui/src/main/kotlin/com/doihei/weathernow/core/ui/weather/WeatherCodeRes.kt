package com.doihei.weathernow.core.ui.weather

import androidx.annotation.StringRes
import com.doihei.weathernow.core.model.weather.WeatherCode
import com.doihei.weathernow.core.ui.R

val WeatherCode.labelResId: Int
    @StringRes get() =
        when (this) {
            WeatherCode.CLEAR_SKY -> R.string.weather_clear_sky
            WeatherCode.MAINLY_CLEAR -> R.string.weather_mainly_clear
            WeatherCode.PARTLY_CLOUDY -> R.string.weather_partly_cloudy
            WeatherCode.OVERCAST -> R.string.weather_overcast
            WeatherCode.FOG -> R.string.weather_fog
            WeatherCode.RIME_FOG -> R.string.weather_rime_fog
            WeatherCode.LIGHT_DRIZZLE -> R.string.weather_light_drizzle
            WeatherCode.MODERATE_DRIZZLE -> R.string.weather_moderate_drizzle
            WeatherCode.DENSE_DRIZZLE -> R.string.weather_dense_drizzle
            WeatherCode.LIGHT_RAIN -> R.string.weather_light_rain
            WeatherCode.MODERATE_RAIN -> R.string.weather_moderate_rain
            WeatherCode.HEAVY_RAIN -> R.string.weather_heavy_rain
            WeatherCode.LIGHT_SNOW -> R.string.weather_light_snow
            WeatherCode.MODERATE_SNOW -> R.string.weather_moderate_snow
            WeatherCode.HEAVY_SNOW -> R.string.weather_heavy_snow
            WeatherCode.SNOW_GRAINS -> R.string.weather_snow_grains
            WeatherCode.LIGHT_RAIN_SHOWER -> R.string.weather_light_rain_shower
            WeatherCode.MODERATE_RAIN_SHOWER -> R.string.weather_moderate_rain_shower
            WeatherCode.VIOLENT_RAIN_SHOWER -> R.string.weather_violent_rain_shower
            WeatherCode.LIGHT_SNOW_SHOWER -> R.string.weather_light_snow_shower
            WeatherCode.HEAVY_SNOW_SHOWER -> R.string.weather_heavy_snow_shower
            WeatherCode.THUNDERSTORM -> R.string.weather_thunderstorm
            WeatherCode.THUNDERSTORM_WITH_HAIL -> R.string.weather_thunderstorm_with_hail
            WeatherCode.THUNDERSTORM_WITH_HEAVY_HAIL -> R.string.weather_thunderstorm_with_heavy_hail
            WeatherCode.UNKNOWN -> R.string.weather_unknown
        }
