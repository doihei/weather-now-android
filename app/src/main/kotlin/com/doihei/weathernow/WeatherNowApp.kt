package com.doihei.weathernow

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// Hilt の DI グラフルート
// iOS の @main struct WeatherNowApp: App に対応する「アプリの起点」
// @HiltAndroidApp をつけることで Application クラスが Hilt の
// コンポーネント階層の頂点（ApplicationComponent）になる
// AndroidManifest.xml の android:name に登録が必要
@HiltAndroidApp
class WeatherNowApp : Application()
