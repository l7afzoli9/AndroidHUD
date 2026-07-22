package com.example.androidhud

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import com.example.androidhud.presentation.hud.HudScreen
import com.example.androidhud.ui.theme.AndroidHUDTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ensure the content is displayed edge-to-edge by default
        // This is often handled by WindowCompat.setDecorFitsSystemWindows(window, false)
        // but Jetpack Compose's MaterialTheme Surface might also handle it.
        // For explicit control, we can ensure it.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            AndroidHUDTheme { // Assuming AndroidHUDTheme is your MaterialTheme wrapper
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background // Use theme's background
                ) {
                    HudScreen() // Load the HudScreen composable
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    AndroidHUDTheme {
        // Provide a basic mock state for preview if needed, or just the screen
        // For actual preview, HudScreen might require injected dependencies if not using a ViewModel factory.
        // Since we are using Hilt and ViewModel, a simple call should work if the preview is run in an environment with Hilt setup.
        // If not, a MockViewModel or a simplified state might be needed here.
        // For now, assume it works or will be handled by the IDE preview.
        HudScreen()
    }
}
