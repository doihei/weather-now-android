package com.doihei.weathernow.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.doihei.weathernow.feature.weather.mvi.currentweather.CurrentWeatherMviScreen
import com.doihei.weathernow.feature.weather.mvvm.currentweather.CurrentWeatherScreen

// Navigation 3 のナビグラフ
// iOS の NavigationStack { ... } に対応
//
// Nav3 の設計思想：
//   バックスタック = 開発者が所有する List<Any>（状態）
//   NavDisplay  = そのリストを監視して画面を描画するコンポーネント
//   → TCA の StackState と NavigationStackStore の関係とまったく同じ
//
// @param backStack  : 現在のバックスタック（呼び出し元が所有・管理する）
// @param onNavigate : 画面遷移時のコールバック（バックスタックへの追加）
// @param onBack     : バック時のコールバック（バックスタックからの削除）
@Composable
fun WeatherNavGraph(
    backStack: List<WeatherRoute>,
    onNavigate: (WeatherRoute) -> Unit,
    onBack: () -> Unit,
) {
    // NavDisplay：バックスタックのリストを受け取り、各エントリを Composable として描画する
    // iOS の NavigationStack(path: $path) { destination in ... } に対応
    //
    // entryProvider：ルートの型に応じてどの Composable を描画するかを定義する
    NavDisplay(
        backStack = backStack,
        onBack = onBack,
        // NavEntry：1画面分の描画定義
        // key にルートオブジェクト、content に Composable を渡す
        entryProvider = { route ->
            when (route) {
                // iOS の case .currentWeather: CurrentWeatherView() に対応
                is WeatherRoute.CurrentWeatherMvvm ->
                    NavEntry(key = route) {
                        CurrentWeatherScreen(
                            onNavigateToMvi = {
                                onNavigate(WeatherRoute.CurrentWeatherMvi)
                            },
                        )
                    }

                is WeatherRoute.CurrentWeatherMvi ->
                    NavEntry(key = route) {
                        CurrentWeatherMviScreen(
                            onBack = onBack
                        )
                    }
            }
        }
    )
}
