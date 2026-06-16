package com.doihei.weathernow.core.ui.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.doihei.weathernow.core.ui.theme.WeatherNowTheme

// iOS の ProgressView() に対応
@Composable
fun WeatherLoadingView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Preview(showBackground = true)
@Composable
private fun WeatherLoadingViewPreview() {
    WeatherLoadingView(modifier = Modifier.fillMaxSize())
}

@Preview(showBackground = true, name = "Loading Light")
@Composable
private fun WeatherLoadingViewLightPreview() {
    WeatherNowTheme(darkTheme = false) {
        WeatherLoadingView(modifier = Modifier.fillMaxSize())
    }
}

@Preview(
    showBackground = true,
    name = "Loading Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun WeatherLoadingViewDarkPreview() {
    WeatherNowTheme(darkTheme = true) {
        WeatherLoadingView(modifier = Modifier.fillMaxSize())
    }
}
