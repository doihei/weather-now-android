package com.doihei.weathernow

import android.os.Bundle
import android.os.PersistableBundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import com.doihei.weathernow.core.ui.theme.WeatherNowTheme
import com.doihei.weathernow.navigation.WeatherNavGraph
import com.doihei.weathernow.navigation.WeatherRoute
import dagger.hilt.android.AndroidEntryPoint

// @AndroidEntryPoint：この Activity に Hilt の依存注入を有効化する
// ViewModel を @HiltViewModel で定義した画面をホストするすべての
// Activity / Fragment に必要
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // システムバーの下まで描画を拡張

        setContent {
            // iOS の @main App に .preferredColorScheme() を書くのと同じ位置付け
            // WeatherNowTheme が isSystemInDarkTheme() を呼びライト/ダークを自動判定する
            WeatherNowTheme {
                // バックスタックを状態として所有する
                // iOS の RootFeature.State の weatherPath: StackState<WeatherPath.State> に対応
                // mutableStateListOf：要素の追加/削除で Compose の再コンポーズが起きる
                // remember：configuration change（画面回転）をまたいで保持される
                val backStack = remember {
                    mutableStateListOf<WeatherRoute>(
                        WeatherRoute.CurrentWeatherMvvm // 初期画面
                    )
                }

                WeatherNavGraph(
                    backStack = backStack,
                    onNavigate = { route ->
                        // バックスタックに追加 → NavDisplay が新しい画面を描画する
                        // iOS の state.weatherPath.append(.xxx) に対応
                        backStack.add(route)
                    },
                    onBack = {
                        // バックスタックから末尾を削除 → 前の画面に戻る
                        // iOS の dismiss() / .popLast() に対応
                        if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    }
                )
            }
        }
    }
}
