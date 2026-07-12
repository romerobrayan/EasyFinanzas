package dev.romerobrayan.tinto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dagger.hilt.android.AndroidEntryPoint
import dev.romerobrayan.tinto.core.designsystem.theme.LocalTintoTypography
import dev.romerobrayan.tinto.core.designsystem.theme.TintoTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TintoTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = LocalTintoTypography.current.screenTitle,
                    )
                }
            }
        }
    }
}
