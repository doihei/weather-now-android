package com.doihei.weathernow.feature.weather.mvi

// iOS の Effect（副作用）のうち「一度きりの出来事」を表す型
// StateFlow に入れると画面回転のたびに再発火する → Channel で流す
//
// 使い分けの判断基準：
//   「今の画面状態として持ち続けるべきか？」→ YES なら State（StateFlow）
//   「一度だけ通知して終わりか？」           → YES なら SideEffect（Channel）
//
// 例：
//   エラーを画面に表示し続ける → State.Error（StateFlow）
//   スナックバーを一度だけ表示 → SideEffect.ShowSnackbar（Channel）
//   別画面に遷移する          → SideEffect.NavigateTo（Channel）
sealed interface CurrentWeatherSideEffect {
    // スナックバーで一時的なメッセージを表示する
    // iOS の .send(.showAlert(message)) に対応
    // StateFlow に入れると画面回転のたびにスナックバーが再表示されるバグになる
    data class ShowSnackBar(
        val message: String,
    ) : CurrentWeatherSideEffect

    // 将来の拡張例（Phase 5 で使う可能性あり）
    // data class NavigateToDetail(val cityId: Int) : CurrentWeatherSideEffect
}
