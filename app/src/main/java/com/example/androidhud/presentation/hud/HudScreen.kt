package com.example.androidhud.presentation.hud

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.view.WindowManager
import androidx.compose.animation.animateFloatAsState
import androidx.compose.animation.core.EaseOutSine
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.androidhud.util.Constants.CONTROL_HIDE_DELAY_MS
import com.example.androidhud.util.Constants.ONE_SECOND_IN_MS
import compose.icons.FontAwesomeIcons
import compose.icons.vectorIcons.FontAwesome
import compose.icons.vectorIcons.fontawesome.Solid as FaSolid
import kotlin.math.roundToInt

@Composable
fun HudScreen(
    viewModel: HudViewModel = viewModel()
) {
    val uiState by viewModel.hudUiState.collectAsState()
    val context = LocalContext.current

    // Handle Screen Keep Awake
    val activity = LocalContext.current.findActivity()
    activity?.window?.let { window ->
        if (uiState.isLocationEnabled || uiState.currentSpeedKph > 0) { // Keep screen on when app is active/tracking
            WindowCompat.setKeepScreenOn(window, true)
        } else {
            WindowCompat.setKeepScreenOn(window, false)
        }
    }


    // --- Speed Digit Animation ---
    val animatedSpeed by animateFloatAsState(
        targetValue = uiState.currentSpeedKph,
        animationSpec = tween(durationMillis = 300, easing = EaseOutSine), // Smoother animation
        label = "animatedSpeed"
    )

    // --- UI Layout ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Strict black background for AMOLED
            .pointerInput(Unit) {
                detectTapGestures {
                    viewModel.userInteracted() // Show controls on tap
                }
            }
            .graphicsLayer {
                // HUD Mirroring Transform
                if (uiState.isHUDRotated) {
                    scaleX = -1f
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Display Current Speed
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            // Speed Display
            Text(
                text = "%.0f".format(animatedSpeed),
                color = Color(0xFF00EEEE), // Neon Cyan
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Units (km/h or mph)
            Text(
                text = if (uiState.isMetric) "km/h" else "mph",
                color = Color(0xFF00FF66), // Neon Green
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }

        // Controls overlay, visible when user interacts
        if (uiState.controlsVisible) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.0f)) // Transparent background for controls
                    .padding(16.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Row for additional stats (Max Speed, Avg Speed, Distance)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Max Speed
                    MetricDisplay(
                        label = "Max",
                        value = uiState.maxSpeedKph.roundToInt(),
                        unit = if (uiState.isMetric) "km/h" else "mph"
                    )

                    // Average Speed
                    MetricDisplay(
                        label = "Avg",
                        value = uiState.averageSpeedKph.roundToInt(),
                        unit = if (uiState.isMetric) "km/h" else "mph"
                    )

                    // Total Distance
                    MetricDisplay(
                        label = "Dist",
                        value = uiState.totalDistanceKm.roundToInt(), // Display distance in km rounded
                        unit = "km" // Always show km for now, conversion to miles can be added
                    )
                }

                // Action Buttons Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reset Button
                    ControlIconButton(
                        icon = FontAwesomeIcons.Solid.Redo,
                        contentDescription = "Reset Session",
                        onClick = { viewModel.resetSession() }
                    )

                    // Unit Toggle Button
                    ControlIconButton(
                        icon = if (uiState.isMetric) FontAwesomeIcons.Solid.CarBattery else FontAwesomeIcons.Solid.Bolt, // Example icons for km/h vs mph
                        contentDescription = "Toggle Units",
                        onClick = { viewModel.toggleUnits() }
                    )

                    // HUD Mode Toggle Button
                    ControlIconButton(
                        icon = if (uiState.isHUDRotated) FontAwesomeIcons.Solid.SyncAlt else FontAwesomeIcons.Solid.ExchangeAlt, // Example icons for mirroring
                        contentDescription = "Toggle HUD Mode",
                        onClick = { viewModel.toggleHUDMode() }
                    )
                }
            }
        }
    }

    // Check and request permissions on initial composition if not granted
    if (!hasLocationPermission(context)) {
        PermissionRequester(context = context, viewModel = viewModel)
    }
}

@Composable
fun MetricDisplay(label: String, value: Int, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = Color(0xFF00FF66), // Neon Green
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
        Text(
            text = "$value",
            color = Color(0xFF00EEEE), // Neon Cyan
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
        Text(
            text = unit,
            color = Color(0xFF00FF66), // Neon Green
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
    }
}

@Composable
fun ControlIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = Color(0xFF00EEEE), // Neon Cyan
        modifier = Modifier
            .size(40.dp)
            .clickable(onClick = onClick)
            .background(Color.Black.copy(alpha = 0.0f)) // Ensure tap detection on icon itself
            .padding(8.dp)
    )
}

// Helper to find Activity from Context
fun Context.findActivity(): Activity? {
    var context = this
    // Added null check for baseContext and loop condition for safety
    while (context is ContextWrapper && context !is Activity) {
        context = context.baseContext ?: return null // Exit loop if baseContext is null
    }
    return if (context is Activity) context else null
}


// Permission handling composable
@Composable
fun PermissionRequester(context: Context, viewModel: HudViewModel) {
    val activity = LocalContext.current.findActivity() ?: return

    // Request permission only if it's not already granted
    LaunchedEffect(Unit) {
        if (!hasLocationPermission(context)) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                101 // Request code
            )
        }
    }

    // You might want to display a message or a button here if permission is denied permanently,
    // or if the user needs to grant it manually.
    // For simplicity, we're assuming the user will grant it when prompted.
    // The ViewModel will be updated via the collectAsState when permission is granted and updates start.
}

// Helper function to check location permission
fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}
