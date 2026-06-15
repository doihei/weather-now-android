package com.doihei.weathernow.feature.weather.mvi.currentweather

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.doihei.weathernow.core.ui.component.WeatherErrorView
import com.doihei.weathernow.core.ui.component.WeatherLoadedView
import com.doihei.weathernow.core.ui.component.WeatherLoadingView
import com.doihei.weathernow.feature.weather.mvi.R
import com.doihei.weathernow.core.ui.R as CoreUiR

// iOS の CurrentWeatherView（TCA 版）に対応
// MVVM 版との差分：
//   1. onIntent() 単一エントリポイント（store.send() に対応）
//   2. SideEffect（Channel）を LaunchedEffect で収集して Snackbar を表示
//   3. state.viewState のネストを辿る
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentWeatherMviScreen(
    onBack: () -> Unit,
    viewModel: CurrentWeatherMviViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // SnackbarHostState：スナックバーの表示を制御する状態
    // remember で Composable のライフタイムを通じて保持する
    val snackbarHostState = remember { SnackbarHostState() }

    // SideEffect（Channel）の収集
    // LaunchedEffect(Unit)：画面が表示されたとき1回だけ起動するコルーチン
    // viewModel.sideEffect は Channel.receiveAsFlow() なので
    // collect で受け取り続ける（iOS の .onReceive に対応）
    //
    // 重要：LaunchedEffect はこの Composable がツリーから外れると
    // 自動でキャンセルされる。手動で cancel する必要はない。
    // iOS の .task { } のキャンセル自動処理と同じ
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                // iOS の .alert(isPresented:) / .toast に対応
                // Channel の one-shot 性により画面回転後に再表示されない
                is CurrentWeatherSideEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = effect.message,
                        // 既存のスナックバーがある場合は置き換える
                        withDismissAction = true,
                    )
                }
            }
        }
    }

    val context = LocalContext.current
    // iOS の CLLocationManager.requestWhenInUseAuthorization() に対応
    // 付与 → OnAppear、拒否 → PermissionDenied を送ることで ViewModel が状態を決定する
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val intent = if (permissions.values.any { it }) {
            CurrentWeatherIntent.OnAppear
        } else {
            CurrentWeatherIntent.PermissionDenied
        }
        viewModel.onIntent(intent)
    }

    // iOS の .onAppear { store.send(.onAppear) } に対応
    // パーミッション付与済みなら即 OnAppear、未付与ならシステムダイアログを表示してから判断する
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) viewModel.onIntent(CurrentWeatherIntent.OnAppear)
        else permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_title_weather_mvi)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(CoreUiR.string.cd_back),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            // iOS の store.send(.refreshButtonTapped) に対応
                            viewModel.onIntent(CurrentWeatherIntent.Refresh)
                        },
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(CoreUiR.string.cd_refresh))
                    }
                },
            )
        },
        // SnackbarHost：Scaffold 内でスナックバーを表示する場所を指定
        // iOS の .overlay { ToastView } に対応
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        ) {
            // MVI 版は state.viewState のネストを辿る点が MVVM 版と異なる
            when (val viewState = state.viewState) {
                is CurrentWeatherState.ViewState.Idle -> WeatherLoadingView()

                is CurrentWeatherState.ViewState.Loading -> WeatherLoadingView()

                is CurrentWeatherState.ViewState.Loaded ->
                    WeatherLoadedView(
                        weather = viewState.weather,
                    )

                is CurrentWeatherState.ViewState.Error ->
                    WeatherErrorView(
                        message = viewState.message,
                        onRetry = {
                            // iOS の store.send(.retryButtonTapped) に対応
                            viewModel.onIntent(CurrentWeatherIntent.Retry)
                        },
                    )
            }
        }
    }
}
