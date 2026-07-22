package com.example.androidhud.presentation.hud

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.os.CountDownTimer
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.androidhud.data.location.LocationRepositoryImpl
import com.example.androidhud.domain.repository.LocationRepository
import com.example.androidhud.util.Constants.CONTROL_HIDE_DELAY_MS
import com.example.androidhud.util.Constants.INITIAL_DISTANCE_THRESHOLD_KM
import com.example.androidhud.util.Constants.INITIAL_SPEED_THRESHOLD_KPH
import com.example.androidhud.util.Constants.ONE_SECOND_IN_MS
import com.example.androidhud.util.Constants.SECOND_IN_DAY_MS
import com.example.androidhud.util.Constants.TWO_SECONDS_IN_MS
import com.example.androidhud.util.Constants.AVG_SPEED_THRESHOLD_RESET_MS
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.location.Location
import javax.inject.Inject
import kotlin.math.roundToInt

data class HudUiState(
    val currentSpeedKph: Float = 0f,
    val maxSpeedKph: Float = 0f,
    val averageSpeedKph: Float = 0f,
    val totalDistanceKm: Float = 0f,
    val isMetric: Boolean = true, // true for km/h, false for mph
    val isHUDRotated: Boolean = false,
    val controlsVisible: Boolean = true,
    val isLocationEnabled: Boolean = false
)

@HiltViewModel
class HudViewModel @Inject constructor(
    private val locationRepository: LocationRepository, // Use the interface
) : ViewModel() {

    private val _hudUiState = MutableStateFlow(HudUiState())
    val hudUiState: StateFlow<HudUiState> = _hudUiState.asStateFlow()

    private var currentLocation: Location? = null
    private var lastLocationTimestamp: Long = 0L
    private var totalDistanceMeters: Double = 0.0
    private var totalDistanceTimestamp: Long = 0L
    private var totalDistanceUpdates: Int = 0
    private var maxSpeedKph: Float = 0f
    private var totalTravelledTimeSeconds: Long = 0L
    private val speedsInSecond = mutableListOf<Float>()

    private var controlsHideTimer: CountDownTimer? = null

    init {
        startLocationUpdates()
        startSpeedAndDistanceCalculation()
        startControlsVisibilityTimer()
    }

    private fun startLocationUpdates() {
        viewModelScope.launch {
            locationRepository.getLocationUpdates()
                .catch { e ->
                    // Handle error, e.g., set isLocationEnabled to false
                    _hudUiState.update { it.copy(isLocationEnabled = false) }
                    println("Error getting location updates: ${e.message}")
                }
                .collect { location ->
                    _hudUiState.update { it.copy(isLocationEnabled = true) }
                    val currentTime = System.currentTimeMillis()

                    // Update current speed
                    val currentSpeed = calculateSpeed(location, currentTime)
                    _hudUiState.update { it.copy(currentSpeedKph = currentSpeed) }

                    // Update max speed
                    if (currentSpeed > maxSpeedKph) {
                        maxSpeedKph = currentSpeed
                        _hudUiState.update { it.copy(maxSpeedKph = maxSpeedKph) }
                    }

                    // Update total distance
                    updateTotalDistance(location, currentTime)

                    currentLocation = location
                    lastLocationTimestamp = currentTime
                }
        }
    }

    private fun startSpeedAndDistanceCalculation() {
        viewModelScope.launch {
            while (true) {
                // Re-calculate average speed based on collected data if needed
                // Or simply update based on current values
                val currentAvgSpeed = calculateAverageSpeed()
                _hudUiState.update { it.copy(averageSpeedKph = currentAvgSpeed) }

                delay(ONE_SECOND_IN_MS) // Check every second
            }
        }
    }

    private fun startControlsVisibilityTimer() {
        controlsHideTimer = object : CountDownTimer(CONTROL_HIDE_DELAY_MS, ONE_SECOND_IN_MS) {
            override fun onTick(millisUntilFinished: Long) {
                // Optional: Log timer tick if needed for debugging
            }

            override fun onFinish() {
                _hudUiState.update { it.copy(controlsVisible = false) }
            }
        }.also { it.start() }
    }

    private fun calculateSpeed(newLocation: Location, currentTime: Long): Float {
        val timeDelta = currentTime - lastLocationTimestamp
        if (currentLocation != null && timeDelta > 0) {
            val distanceMeters = newLocation.distanceTo(currentLocation).toDouble()
            // Speed in m/s
            val speedMs = distanceMeters / timeDelta.millisecondsToSeconds()

            // Convert to km/h if metric, mph if not
            val speedKph = (speedMs * 3.6f).toFloat()

            // Add to list for average speed calculation if speed is significant
            if (speedKph > INITIAL_SPEED_THRESHOLD_KPH) {
                speedsInSecond.add(speedKph)
            }

            return speedKph
        } else if (currentLocation == null) {
            // Handle first location
            lastLocationTimestamp = currentTime // Initialize timestamp
            return 0f
        }
        return 0f // Default if calculation fails
    }

    private fun updateTotalDistance(newLocation: Location, currentTime: Long) {
        if (currentLocation != null) {
            val distanceMeters = newLocation.distanceTo(currentLocation).toDouble()
            totalDistanceMeters += distanceMeters

            // Update timestamp and count for average speed calculation over time
            if (totalDistanceTimestamp == 0L) { // First time calculating distance
                totalDistanceTimestamp = currentTime
            }
            totalDistanceUpdates++
            totalTravelledTimeSeconds = (currentTime - totalDistanceTimestamp) / ONE_SECOND_IN_MS
        }
    }

    private fun calculateAverageSpeed(): Float {
        // Calculate average speed based on distance and time travelled OR collected speeds
        val timeInSeconds = (System.currentTimeMillis() - totalDistanceTimestamp) / ONE_SECOND_IN_MS
        return if (timeInSeconds > 0 && totalDistanceMeters > INITIAL_DISTANCE_THRESHOLD_KM * 1000) {
            val avgKph = (totalDistanceMeters / timeInSeconds.millisecondsToSeconds() * 3.6).toFloat()
            avgKph
        } else {
            0f
        }
    }

    fun toggleUnits() {
        _hudUiState.update {
            it.copy(isMetric = !it.isMetric)
        }
        // Convert current, max, average speeds and reset distance if needed
        // For now, just toggling the flag. Actual conversion needs to be done where values are displayed.
    }

    fun toggleHUDMode() {
        _hudUiState.update {
            it.copy(isHUDRotated = !it.isHUDRotated)
        }
        // Reset controls visibility to ensure they are visible when HUD mode is toggled
        resetControlsVisibilityTimer()
    }

    fun resetSession() {
        currentLocation = null
        lastLocationTimestamp = 0L
        totalDistanceMeters = 0.0
        totalDistanceTimestamp = 0L
        totalDistanceUpdates = 0
        maxSpeedKph = 0f
        totalTravelledTimeSeconds = 0L
        speedsInSecond.clear()

        _hudUiState.update {
            HudUiState(
                isMetric = it.isMetric, // Keep current unit setting
                isHUDRotated = it.isHUDRotated,
                controlsVisible = true, // Reset controls to visible
                isLocationEnabled = it.isLocationEnabled // Keep location status
            )
        }
        // Restart timer after reset
        resetControlsVisibilityTimer()
    }

    // User taps screen, controls should become visible
    fun userInteracted() {
        resetControlsVisibilityTimer() // Restart timer
        _hudUiState.update { it.copy(controlsVisible = true) }
    }

    private fun resetControlsVisibilityTimer() {
        controlsHideTimer?.cancel()
        controlsHideTimer = object : CountDownTimer(CONTROL_HIDE_DELAY_MS, ONE_SECOND_IN_MS) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                _hudUiState.update { it.copy(controlsVisible = false) }
            }
        }.also { it.start() }
    }

    // Helper extension function
    private fun Long.millisecondsToSeconds(): Double {
        return this / 1000.0
    }

    override fun onCleared() {
        super.onCleared()
        controlsHideTimer?.cancel()
    }
}

object Constants {
    const val CONTROL_HIDE_DELAY_MS = 3000L
    const val ONE_SECOND_IN_MS = 1000L
    const val AVG_SPEED_THRESHOLD_RESET_MS = 5000L // Not used currently, but for reference
    const val INITIAL_SPEED_THRESHOLD_KPH = 5f // Minimum speed to consider for avg calculations
    const val INITIAL_DISTANCE_THRESHOLD_KM = 0.1f // Minimum distance to consider for avg calculations
    const val SECOND_IN_DAY_MS = 86400000L
    const val TWO_SECONDS_IN_MS = 2000L
}