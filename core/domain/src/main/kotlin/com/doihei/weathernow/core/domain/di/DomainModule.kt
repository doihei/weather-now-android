package com.doihei.weathernow.core.domain.di

import com.doihei.weathernow.core.domain.location.DefaultLocationService
import com.doihei.weathernow.core.domain.location.LocationService
import com.doihei.weathernow.core.domain.repository.DefaultWeatherRepository
import com.doihei.weathernow.core.domain.repository.WeatherRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// iOS の DependencyKey.liveValue 登録に対応
// @Binds は実装クラスを interface に束縛する（@Provides より軽量で Hilt 推奨）
@Module
@InstallIn(SingletonComponent::class)
abstract class DomainModule {
    // iOS の WeatherRepositoryKey.liveValue = DefaultWeatherRepository() に対応
    @Binds
    @Singleton
    abstract fun bindWeatherRepository(impl: DefaultWeatherRepository): WeatherRepository

    // iOS の LocationServiceKey.liveValue = DefaultLocationService() に対応
    @Binds
    @Singleton
    abstract fun bindLocationService(impl: DefaultLocationService): LocationService
}
