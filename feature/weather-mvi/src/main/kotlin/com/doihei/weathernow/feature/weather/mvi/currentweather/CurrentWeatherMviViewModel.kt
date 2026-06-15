package com.doihei.weathernow.feature.weather.mvi.currentweather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doihei.weathernow.core.domain.exception.WeatherException
import com.doihei.weathernow.core.domain.usecase.GetWeatherUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// iOS の @Reducer struct CurrentWeatherFeature に対応
// TCA の Store が State を所有し Action を受け取るのと同様に、
// ViewModel が State を所有し Intent を受け取る
@HiltViewModel
class CurrentWeatherMviViewModel
    @Inject
    constructor(
        private val getWeatherUseCase: GetWeatherUseCase,
    ) : ViewModel() {
        // iOS の @ObservableState var state = State() に対応
        // State 全体を1つの StateFlow で管理する（TCA に近い設計）
        private val _state = MutableStateFlow(CurrentWeatherState())
        val state: StateFlow<CurrentWeatherState> = _state.asStateFlow()

        // iOS の TCA Effect で「一度きりの副作用」を Channel で流す
        // StateFlow ではなく Channel にする理由：
        //   StateFlow: 最新値をリプレイする → 画面回転後に再購読すると再発火する
        //   Channel:   一度読んだら消える  → 画面回転後も再発火しない（one-shot）
        // CAPACITY = 0（Rendezvous）は送信者が受信者を待つが、
        // Channel.BUFFERED にすることで送信者がブロックされない
        private val _sideEffect = Channel<CurrentWeatherSideEffect>(Channel.BUFFERED)
        val sideEffect = _sideEffect.receiveAsFlow()

        // iOS の Store.send(_ action:) に対応
        // すべての Intent をここで受け取り、対応する処理に振り分ける
        // TCA の reducer がすべての Action を switch で処理するのと同じ構造
        fun onIntent(intent: CurrentWeatherIntent) {
            when (intent) {
                // iOS の case .onAppear: に対応
                is CurrentWeatherIntent.OnAppear -> {
                    // Loading 中の二重発火防止 ＋ 既に Loaded なら再ロード不要
                    // iOS の guard !state.isLoaded else { return } に対応
                    val vs = _state.value.viewState
                    if (vs is CurrentWeatherState.ViewState.Loading ||
                        vs is CurrentWeatherState.ViewState.Loaded
                    ) {
                        return
                    }
                    loadWeather()
                }
                // iOS の case .refreshButtonTapped: に対応
                is CurrentWeatherIntent.Refresh -> {
                    // Refresh は常に実行（Loading 中でも強制リフレッシュ）
                    loadWeather()
                }
                // iOS の case .retryButtonTapped: に対応
                is CurrentWeatherIntent.Retry -> {
                    loadWeather()
                }
            }
        }

        // ---- 内部処理 ----

        // iOS の func loadWeather() { ... } に対応
        // Intent から呼ばれる private な実行単位
        // TCA では Effect.run { } でラップするが、自前 MVI では viewModelScope.launch を直接使う
        private fun loadWeather() {
            viewModelScope.launch {
                // State を更新する（TCA の return .run { } 前に state を更新するのと同じ）
                // .update は現在値を受け取って新しい State を返す atomic な更新
                // iOS の state.viewState = .loading に対応
                _state.update { it.copy(viewState = CurrentWeatherState.ViewState.Loading) }

                // UseCase を実行（TCA の Effect.run { await useCase() } に対応）
                val result = getWeatherUseCase()

                // 結果を State に反映（TCA の Action.weatherResponse(.success(weather)) を self で処理）
                result.fold(
                    onSuccess = { weather ->
                        // iOS の state.viewState = .loaded(weather) に対応
                        _state.update {
                            it.copy(viewState = CurrentWeatherState.ViewState.Loaded(weather))
                        }
                    },
                    onFailure = { throwable ->
                        val message =
                            (throwable as? WeatherException)?.error?.userMessage
                                ?: throwable.message
                                ?: "不明なエラーが発生しました"

                        // 永続的なエラー表示は State に持たせる
                        _state.update {
                            it.copy(viewState = CurrentWeatherState.ViewState.Error(message))
                        }

                        // 一時的なスナックバー通知は SideEffect（Channel）で流す
                        // TCA の .send(.showAlert) に対応
                        // iOS なら Effect.send(.showAlert(message)) と書くところ
                        _sideEffect.send(CurrentWeatherSideEffect.ShowSnackbar(message))
                    },
                )
            }
        }
    }
