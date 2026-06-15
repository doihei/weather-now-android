package com.doihei.weathernow.core.domain.location

import android.annotation.SuppressLint
import android.content.Context
import com.doihei.weathernow.core.domain.exception.WeatherException
import com.doihei.weathernow.core.model.error.WeatherError
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

// iOS の actor LocationService: LocationServiceProtocol に対応
// FusedLocationProviderClient のコールバックを suspendCancellableCoroutine で suspend 化する
// これが Kotlin における「コールバック → suspend 変換」の最重要イディオム
class DefaultLocationService
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : LocationService {
        private val fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(context)

        // iOS の func currentLocation() async throws -> CLLocationCoordinate2D に対応
        // @SuppressLint：Hilt + テストでパーミッションを管理するため実装側では抑制
        // @SuppressLint：パーミッション確認は Screen 層の責務。実装側では抑制
        @SuppressLint("MissingPermission")
        override suspend fun currentLocation(): Result<Location> =
            suspendCancellableCoroutine { continuation ->
                try {
                    // iOS の CLLocationManager の requestLocation() + delegate コールバックに対応
                    // FusedLocationProviderClient は現在地を1回だけ取得する getCurrentLocation を使う
                    fusedLocationClient
                        .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                        .addOnSuccessListener { androidLocation ->
                            if (androidLocation != null) {
                                // iOS の continuation.resume(returning:) に対応
                                continuation.resume(
                                    Result.success(
                                        Location(
                                            latitude = androidLocation.latitude,
                                            longitude = androidLocation.longitude,
                                        ),
                                    ),
                                )
                            } else {
                                continuation.resume(
                                    Result.failure(
                                        WeatherException(WeatherError.LocationUnavailable),
                                    ),
                                )
                            }
                        }.addOnFailureListener { exception ->
                            // iOS の continuation.resume(throwing:) に対応
                            continuation.resume(Result.failure(exception))
                        }
                } catch (e: SecurityException) {
                    // パーミッション未付与のまま呼ばれた場合のフォールバック
                    continuation.resume(Result.failure(WeatherException(WeatherError.LocationDenied)))
                }
            }
    }
