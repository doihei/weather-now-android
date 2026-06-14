package com.doihei.weathernow.feature.weather.mvi

import com.doihei.weathernow.core.model.weather.Weather

// iOS の struct CurrentWeatherState: Equatable に対応
// data class にすることで equals/hashCode/copy が自動生成される
// TCA の State と同様に「画面に表示する値の集合」だけを持つ
// 副作用の結果（エラーメッセージ等）も State に含めて良いが、
// 「一度きりのイベント」（スナックバー表示など）は SideEffect で分離する
data class CurrentWeatherState(
    // iOS の var viewState: ViewState = .idle に対応
    val viewState: ViewState = ViewState.Idle,
) {
    // ViewState は State のネストとして定義する
    // TCA では State に enum を直接持たせる設計が多い
    // iOS の enum ViewState に対応
    sealed interface ViewState {
        data object Idle : ViewState

        data object Loading : ViewState

        data class Loaded(
            val weather: Weather,
        ) : ViewState

        // MVI では Error メッセージを State に持たせる
        // ただし「表示した後にクリアしたい」なら SideEffect にする選択肢もある
        // ここでは State に持たせるシンプルな設計を選ぶ
        data class Error(
            val message: String,
        ) : ViewState
    }
}
