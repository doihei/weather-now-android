package com.doihei.weathernow.core.ui.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.doihei.weathernow.core.ui.R
import com.doihei.weathernow.core.ui.theme.WeatherNowSpacing
import com.doihei.weathernow.core.ui.theme.WeatherNowTheme

@Composable
fun WeatherErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(WeatherNowSpacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.error_icon),
            style = MaterialTheme.typography.displayMedium,
        )
        Spacer(modifier = Modifier.height(WeatherNowSpacing.lg))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(WeatherNowSpacing.xl))
        Button(onClick = onRetry) {
            Text(stringResource(R.string.error_retry))
        }
    }
}

@Preview(showBackground = true, name = "Error Light")
@Composable
private fun WeatherErrorViewLightPreview() {
    WeatherNowTheme(darkTheme = false) {
        WeatherErrorView(
            message = "位置情報を取得できませんでした",
            onRetry = {},
        )
    }
}

@Preview(
    showBackground = true,
    name = "Error Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun WeatherErrorViewDarkPreview() {
    WeatherNowTheme(darkTheme = true) {
        WeatherErrorView(
            message = "位置情報を取得できませんでした",
            onRetry = {},
        )
    }
}
