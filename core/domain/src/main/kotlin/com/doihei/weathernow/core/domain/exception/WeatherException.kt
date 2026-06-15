package com.doihei.weathernow.core.domain.exception

import com.doihei.weathernow.core.model.error.WeatherError

// WeatherError（model層）を Throwable として domain 層に伝搬するラッパー
// Result<T> の型引数制約上 Throwable が必要なため
class WeatherException(
    val error: WeatherError,
    cause: Throwable? = null,
) : Exception(error.userMessage, cause)
