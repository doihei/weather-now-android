package com.doihei.weathernow.core.network.di

import javax.inject.Qualifier

// Retrofit インスタンスが2つ（天気 API / 都市検索 API）あるため
// Hilt が区別できるように Qualifier を定義する
// iOS では LiveXxxClient を2つのクラスとして分けていたのに対応

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ForecastRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class GeocodingRetrofit
