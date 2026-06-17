package com.doihei.weathernow.core.ui.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.doihei.weathernow.core.model.settings.AppSettings
import com.doihei.weathernow.core.model.weather.CurrentWeather
import com.doihei.weathernow.core.model.weather.DailyForecast
import com.doihei.weathernow.core.model.weather.Weather
import com.doihei.weathernow.core.model.weather.WeatherCode
import com.doihei.weathernow.core.ui.R
import com.doihei.weathernow.core.ui.theme.WeatherNowSpacing
import com.doihei.weathernow.core.ui.theme.WeatherNowTheme
import com.doihei.weathernow.core.ui.weather.labelResId
import java.time.format.TextStyle
import java.util.Locale

// iOS の WeatherLoadedView に対応
// Phase Ex でデザインシステムを適用するため、今は最低限の構成にする
@Composable
fun WeatherLoadedView(
    weather: Weather,
    // 単位変換は AppSettings から取得する（Phase Ex で設定画面を作る予定）
    temperatureUnit: AppSettings.TemperatureUnit = AppSettings.TemperatureUnit.CELSIUS,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = WeatherNowSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(WeatherNowSpacing.md),
    ) {
        item { Spacer(modifier = Modifier.height(WeatherNowSpacing.sm)) }

        // 現在の天気カード
        item {
            CurrentWeatherCard(
                weather = weather,
                temperatureUnit = temperatureUnit,
            )
        }

        // ── Vico グラフを追加 ──────────────────────────────────────
        // iOS の Chart { } セクションに対応
        // hourly が空でないときだけ表示する（API レスポンス前の safety）
        if (weather.hourly.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.forecast_hourly_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = WeatherNowSpacing.sm),
                )
            }
            item {
                // HourlyForecastChart はスクロール可能なため LazyColumn のアイテムとして配置する
                // iOS の ScrollView { Chart { } } に対応
                HourlyForecastChart(hourlyForecasts = weather.hourly)
            }
        }
        // ────────────────────────────────────────────────────────────

        // 日次予報
        item {
            Text(
                text = stringResource(R.string.forecast_7day),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = WeatherNowSpacing.sm),
            )
        }
        items(weather.daily) { daily ->
            DailyForecastRow(forecast = daily, temperatureUnit = temperatureUnit)
        }

        item { Spacer(modifier = Modifier.height(WeatherNowSpacing.lg)) }
    }
}

@Composable
private fun CurrentWeatherCard(
    weather: Weather,
    temperatureUnit: AppSettings.TemperatureUnit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(WeatherNowSpacing.card),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 天気の説明文（:core:ui の WeatherCodeRes から @StringRes で取得）
            // iOS の weatherCode.description に対応（文字列は strings.xml で管理）
            Text(
                text = stringResource(weather.current.code.labelResId),
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(WeatherNowSpacing.sm))
            // 気温
            Text(
                text =
                    stringResource(
                        R.string.format_temperature,
                        temperatureUnit.convert(weather.current.temperature),
                        temperatureUnit.symbol,
                    ),
                style = MaterialTheme.typography.displayLarge,
            )
            Spacer(modifier = Modifier.height(WeatherNowSpacing.xs))
            // 体感温度
            Text(
                text =
                    stringResource(
                        R.string.format_temperature,
                        temperatureUnit.convert(weather.current.feelsLike),
                        temperatureUnit.symbol,
                    ),
                style = MaterialTheme.typography.displayLarge,
            )
            Spacer(modifier = Modifier.height(WeatherNowSpacing.md))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(WeatherNowSpacing.md))
            // 湿度・風速
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                WeatherDetailItem(
                    label = stringResource(R.string.label_humidity),
                    value = "${weather.current.humidity}",
                )
                WeatherDetailItem(
                    label = stringResource(R.string.label_wind_speed),
                    value = stringResource(R.string.format_wind_speed, weather.current.windSpeed),
                )
            }
        }
    }
}

@Composable
private fun DailyForecastRow(
    forecast: DailyForecast,
    temperatureUnit: AppSettings.TemperatureUnit,
    modifier: Modifier = Modifier,
) {
// iOS の DailyForecastRow に対応
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 曜日
        Text(
            text = forecast.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.JAPANESE),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        // 天気
        Text(
            text = stringResource(forecast.code.labelResId),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(2f),
        )
        // 最高/最低気温
        Text(
            text =
                stringResource(
                    R.string.format_temp_range,
                    temperatureUnit.convert(forecast.maxTemp),
                    temperatureUnit.convert(forecast.minTemp),
                    temperatureUnit.symbol,
                ),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun WeatherDetailItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun previewWeather() =
    Weather(
        current =
            CurrentWeather(
                temperature = 20.0,
                feelsLike = 18.0,
                humidity = 60,
                windSpeed = 10.0,
                code = WeatherCode.CLEAR_SKY,
            ),
        hourly = emptyList(),
        daily = emptyList(),
    )

@Preview(showBackground = true, name = "Loaded Light")
@Composable
private fun WeatherLoadedViewLightPreview() {
    WeatherNowTheme(darkTheme = false) {
        WeatherLoadedView(weather = previewWeather())
    }
}

@Preview(
    showBackground = true,
    name = "Loaded Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun WeatherLoadedViewDarkPreview() {
    WeatherNowTheme(darkTheme = true) {
        WeatherLoadedView(weather = previewWeather())
    }
}
