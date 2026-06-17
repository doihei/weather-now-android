package com.doihei.weathernow.core.ui.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.doihei.weathernow.core.model.weather.HourlyForecast
import com.doihei.weathernow.core.model.weather.WeatherCode
import com.doihei.weathernow.core.ui.R
import com.doihei.weathernow.core.ui.theme.WeatherNowSize
import com.doihei.weathernow.core.ui.theme.WeatherNowSpacing
import com.doihei.weathernow.core.ui.theme.WeatherNowTheme
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.Fill
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.sin

private object HourlyForecastChartDefaults {
    const val AREA_ALPHA = 0.15f
}

// iOS の Chart { ForEach(hourly) { LineMark(...) } } に対応
// Vico v3 の設計思想：
//   CartesianChartModelProducer → データのプロデューサー（ViewModel に持たせることもできる）
//   runTransaction { lineSeries { series(...) } } → データを更新するトランザクション
//   CartesianChartHost → Composable として描画するホスト
@Composable
fun HourlyForecastChart(
    hourlyForecasts: List<HourlyForecast>,
    modifier: Modifier = Modifier,
) {
    val chartLineColor = MaterialTheme.colorScheme.primary
    val timeFormatPattern = stringResource(R.string.format_time_chart)
    val dateFormatPattern = stringResource(R.string.format_date_chart)

    // CartesianChartModelProducer：Vico のデータバインディングの核
    // remember で Composition のライフタイムを通じて保持する
    // iOS の @State var data: [Double] に対応（ただし Vico 専用の型）
    val modelProducer = remember { CartesianChartModelProducer() }

    // hourlyForecasts が変わったときにデータを更新する
    // LaunchedEffect(hourlyForecasts) のキーを hourlyForecasts にすることで
    // データが変化したとき（Refresh 後）にグラフも更新される
    // iOS の .onChange(of: hourly) { chart.reloadData() } に対応
    LaunchedEffect(hourlyForecasts) {
        if (hourlyForecasts.isEmpty()) return@LaunchedEffect
        modelProducer.runTransaction {
            // lineModel { } → LineCartesianLayer のデータ追加 DSL
            // series(y = [...]) → Y 値のリストを渡すと X は自動でインデックスになる
            // iOS の LineMark(x: .value("時間", index), y: .value("気温", temp)) に対応
            lineModel { series(y = hourlyForecasts.map { it.temperature }) }
        }
    }

    // X 軸フォーマッター：0時（日付境界）は "M/d"、それ以外は "HH:mm"
    // 7日分の折れ線では「どの日か」が分かるラベルが必要なため、
    // 日付変わり目（hour == 0）だけ日付を表示して視認性を確保する
    // iOS の .chartXAxis { AxisMarks { if $0.index % 6 == 0 { ... } } } に対応
    val xAxisFormatter =
        CartesianValueFormatter { _, x, _ ->
            val forecast = hourlyForecasts.getOrNull(x.toInt()) ?: return@CartesianValueFormatter ""
            val pattern = if (forecast.time.hour == 0) dateFormatPattern else timeFormatPattern
            forecast.time.format(DateTimeFormatter.ofPattern(pattern))
        }

    // Y 軸のフォーマッター：気温に単位を付ける
    val yAxisFormatter =
        CartesianValueFormatter { _, y, _ ->
            "%.0f°".format(y)
        }

    CartesianChartHost(
        // rememberCartesianChart：チャートの構成を記憶する
        // iOS の Chart { } のクロージャに相当するチャート定義
        chart =
            rememberCartesianChart(
                rememberLineCartesianLayer(
                    lineProvider =
                        LineCartesianLayer.LineProvider.series(
                            // rememberLine：線のスタイル定義
                            // chartLineColor は MaterialTheme から取得するためキーに渡す
                            LineCartesianLayer.rememberLine(
                                fill =
                                    remember(chartLineColor) {
                                        LineCartesianLayer.LineFill.single(
                                            Fill(chartLineColor),
                                        )
                                    },
                                // catmullRom：全点を通過する滑らかな曲線
                                // iOS の .interpolationMethod(.catmullRom) に対応
                                interpolator = LineCartesianLayer.Interpolator.catmullRom(),
                                // 線の下を薄く塗る（iOS の areaPlot 相当）
                                areaFill =
                                    remember(chartLineColor) {
                                        LineCartesianLayer.AreaFill.single(
                                            Fill(chartLineColor.copy(alpha = HourlyForecastChartDefaults.AREA_ALPHA)),
                                        )
                                    },
                            ),
                        ),
                ),
                // Y 軸（左側）
                // iOS の .chartYAxis { AxisMarks(position: .leading) } に対応
                startAxis =
                    VerticalAxis.rememberStart(
                        valueFormatter = yAxisFormatter,
                    ),
                // X 軸（下側）。6 時間ごとにラベルを配置する
                // aligned(spacing = 6) → インデックス 0, 6, 12, 18, 24... にラベルを置く
                // 0 時エントリ（hour == 0）は日付（M/d）、他は時刻（HH:mm）と組み合わせて
                // 「どの日の何時か」が一目でわかる X 軸になる
                bottomAxis =
                    HorizontalAxis.rememberBottom(
                        valueFormatter = xAxisFormatter,
                        itemPlacer = HorizontalAxis.ItemPlacer.aligned(spacing = { 6 }),
                    ),
            ),
        modelProducer = modelProducer,
        modifier =
            modifier
                .fillMaxSize()
                .height(WeatherNowSize.chartHourlyHeight)
                .padding(vertical = WeatherNowSpacing.sm),
        // 横スクロールを有効化（24時間×7日分は横幅に収まらないため）
        // iOS の .chartScrollableAxes(.horizontal) に対応
        scrollState = rememberVicoScrollState(scrollEnabled = true),
    )
}

// 48時間分のダミーデータ（2日間）
// 気温はサイン波で近似：深夜 10°・正午 26° のリアルな日変化
// sin(i * π/12 - π/2) → i=0(00:00)で最低値、i=12(12:00)で最高値
@Suppress("MagicNumber")
private fun previewHourlyForecasts(): List<HourlyForecast> {
    val base = LocalDateTime.of(2024, 6, 17, 0, 0)
    return (0..47).map { i ->
        HourlyForecast(
            time = base.plusHours(i.toLong()),
            temperature = 18.0 + 8.0 * sin(i * PI / 12.0 - PI / 2.0),
            precipitation = if (i in 10..14) 2.0 else 0.0,
            code = WeatherCode.CLEAR_SKY,
        )
    }
}

@Preview(showBackground = true, name = "Chart Light")
@Composable
private fun HourlyForecastChartLightPreview() {
    WeatherNowTheme(darkTheme = false) {
        HourlyForecastChart(hourlyForecasts = previewHourlyForecasts())
    }
}

@Preview(
    showBackground = true,
    name = "Chart Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun HourlyForecastChartDarkPreview() {
    WeatherNowTheme(darkTheme = true) {
        HourlyForecastChart(hourlyForecasts = previewHourlyForecasts())
    }
}
