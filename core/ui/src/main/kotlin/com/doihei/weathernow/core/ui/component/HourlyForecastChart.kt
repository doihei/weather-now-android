package com.doihei.weathernow.core.ui.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.doihei.weathernow.core.model.weather.HourlyForecast
import com.doihei.weathernow.core.ui.R
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
import java.time.format.DateTimeFormatter

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

    // X 軸のフォーマッター：インデックス → 時刻文字列
    // iOS の .chartXAxis { AxisMarks(values: .stride(by: .hour, count: 6)) } に対応
    val xAxisFormatter =
        CartesianValueFormatter { _, x, _ ->
            hourlyForecasts
                .getOrNull(x.toInt())
                ?.time
                ?.format(DateTimeFormatter.ofPattern(timeFormatPattern))
                ?: ""
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
                // X 軸（下側）。6 時間おきにラベルを表示する
                // iOS の .chartXAxis { AxisMarks(values: .stride(by: .hour, count: 6)) }
                bottomAxis =
                    HorizontalAxis.rememberBottom(
                        valueFormatter = xAxisFormatter,
                        // 6件ごとにラベル（24時間 × 7日 ÷ 6 = 28ラベル）
                        itemPlacer = HorizontalAxis.ItemPlacer.segmented(),
                    ),
            ),
        modelProducer = modelProducer,
        modifier =
            modifier
                .fillMaxSize()
                .height(dimensionResource(R.dimen.chart_hourly_height))
                .padding(vertical = dimensionResource(R.dimen.spacing_sm)),
        // 横スクロールを有効化（24時間×7日分は横幅に収まらないため）
        // iOS の .chartScrollableAxes(.horizontal) に対応
        scrollState = rememberVicoScrollState(scrollEnabled = true),
    )
}
