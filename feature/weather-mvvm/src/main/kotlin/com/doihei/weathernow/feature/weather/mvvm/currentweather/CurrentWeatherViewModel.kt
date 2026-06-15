package com.doihei.weathernow.feature.weather.mvvm.currentweather

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doihei.weathernow.core.domain.exception.WeatherException
import com.doihei.weathernow.core.domain.usecase.GetWeatherUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// iOS の @Observable final class CurrentWeatherViewModel に対応
// @HiltViewModel：Hilt が GetWeatherUseCase を自動注入する
// ViewModel を継承することで configuration change（画面回転）をまたいで状態が保持される
// iOS では SwiftUI が暗黙的に管理していた部分を Android では明示的に担う
@HiltViewModel
class CurrentWeatherViewModel
    @Inject
    constructor(
        private val getWeatherUseCase: GetWeatherUseCase,
    ) : ViewModel() {
        // iOS の @Observable var viewState: ViewState = .idle に対応
        // MutableStateFlow：値の変更を Flow として流す。最新値を常に保持する（リプレイ = 1）
        // 外部には読み取り専用の StateFlow として公開し、書き込みは ViewModel 内に閉じる
        // iOS の private(set) var viewState に対応する可視性の分離
        private val _viewState = MutableStateFlow<WeatherViewState>(WeatherViewState.Idle)
        val viewState: StateFlow<WeatherViewState> = _viewState.asStateFlow()

        // iOS の func onAppear() async に対応
        // viewModelScope：ViewModel が破棄されたとき自動でキャンセルされるスコープ
        // iOS では Task { await onAppear() } として明示的にタスクを起動していたのに対応
        fun load() {
            // 既に Loading 中なら二重リクエストを防ぐ
            // iOS の guard viewState != .loading else { return } に対応
            if (_viewState.value is WeatherViewState.Loading) return

            viewModelScope.launch {
                // Step 1: Loading 状態に遷移
                // iOS の viewState = .loading に対応
                _viewState.value = WeatherViewState.Loading

                // Step 2: UseCase を呼ぶ（suspend 関数なので launch の中で直接 await できる）
                // iOS の let weather = try await getWeatherUseCase() に対応
                val result = getWeatherUseCase()

                // Step 3: 結果に応じて状態を遷移
                // iOS の do { viewState = .loaded } catch { viewState = .error } に対応
                // Kotlin では Result の fold で成功/失敗を処理する
                _viewState.value =
                    result.fold(
                        onSuccess = { weather ->
                            WeatherViewState.Loaded(weather)
                        },
                        onFailure = { throwable ->
                            // WeatherException を取り出してユーザー向けメッセージを使う
                            // iOS の error.localizedDescription に対応
                            val message =
                                (throwable as? WeatherException)?.error?.userMessage
                                    ?: throwable.message
                                    ?: "不明なエラーが発生しました"

                            WeatherViewState.Error(message)
                        },
                    )
            }
        }

        // iOS の func refresh() に対応
        // Loading 中でも強制リフレッシュできるようにする
        fun refresh() {
            _viewState.value = WeatherViewState.Idle
            load()
        }
    }
