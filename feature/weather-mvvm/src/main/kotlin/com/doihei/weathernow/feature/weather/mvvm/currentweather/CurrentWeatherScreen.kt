package com.doihei.weathernow.feature.weather.mvvm.currentweather

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.doihei.weathernow.core.ui.component.WeatherErrorView
import com.doihei.weathernow.core.ui.component.WeatherLoadedView
import com.doihei.weathernow.core.ui.component.WeatherLoadingView
import com.doihei.weathernow.feature.weather.mvvm.R
import com.doihei.weathernow.core.ui.R as CoreUiR

// iOS の CurrentWeatherView（MVVM 版）に対応
// @Observable ViewModel の監視を collectAsStateWithLifecycle() で行う
// ライフサイクルを意識した購読がiOSとの最大の差分
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentWeatherScreen(
    onNavigateToMvi: () -> Unit,
    // hiltViewModel()：Hilt が ViewModel を生成して注入する
    // iOS の @StateObject var viewModel = CurrentWeatherViewModel() に対応
    viewModel: CurrentWeatherViewModel = hiltViewModel(),
) {
    // collectAsStateWithLifecycle()：
    //   アプリがバックグラウンド（STARTED 未満）にいる間は収集を停止する
    //   iOS の @Observable が常に監視し続けるのと違い、ライフサイクルを意識する
    //   collectAsState() の代わりにこちらを使うのが 2024 年以降の推奨
    // iOS の @State / @Observable 自動監視に対応
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    // iOS の CLLocationManager.requestWhenInUseAuthorization() に対応
    // 結果コールバック → granted なら load()、拒否なら onPermissionDenied() で即エラー状態へ
    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            if (permissions.values.any { it }) {
                viewModel.load()
            } else {
                viewModel.onPermissionDenied()
            }
        }

    // iOS の .onAppear { viewModel.load() } に対応
    // パーミッション付与済みなら即ロード、未付与ならシステムダイアログを表示してから判断する
    LaunchedEffect(Unit) {
        val granted =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            viewModel.load()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_title_weather_mvvm)) },
                actions = {
                    // MVI版への切り替えボタン（比較用）
                    TextButton(onClick = onNavigateToMvi) {
                        Text(stringResource(R.string.action_go_to_mvi))
                    }
                    // リフレッシュ
                    // iOS の .toolbar { Button { viewModel.refresh() } } に対応
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(CoreUiR.string.cd_refresh))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            // iOS の switch viewModel.viewState { ... } に対応する when 式
            // sealed interface なので網羅チェックが効く
            when (val state = viewState) {
                // Idle は load() 前の瞬間だけ。実質 Loading とほぼ同時に遷移するため
                // ここでは Loading と同じ表示にする
                is WeatherViewState.Idle -> WeatherLoadingView(modifier = Modifier.fillMaxSize())

                is WeatherViewState.Loading -> WeatherLoadingView(modifier = Modifier.fillMaxSize())

                is WeatherViewState.Loaded ->
                    WeatherLoadedView(
                        weather = state.weather,
                    )

                is WeatherViewState.Error ->
                    WeatherErrorView(
                        message = state.message,
                        // iOS の Button("再試行") { viewModel.load() } に対応
                        onRetry = { viewModel.load() },
                    )
            }
        }
    }
}
