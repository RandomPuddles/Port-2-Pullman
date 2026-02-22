package com.example.eventually.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.eventually.ui.theme.ClockAccent
import kotlinx.coroutines.delay

private data class LapRecord(
    val lapNumber: Int,
    val lapTimeMs: Long,
    val overallTimeMs: Long
)

private fun formatStopwatchTime(ms: Long): String {
    val totalCentiseconds = ms / 10
    val centiseconds = (totalCentiseconds % 100).toInt()
    val totalSeconds = totalCentiseconds / 100
    val seconds = (totalSeconds % 60).toInt()
    val minutes = (totalSeconds / 60).toInt()
    return "%02d:%02d.%02d".format(minutes, seconds, centiseconds)
}

@Composable
fun StopwatchScreen() {
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var isRunning by remember { mutableStateOf(false) }
    var startTimestamp by remember { mutableLongStateOf(0L) }
    var pausedElapsed by remember { mutableLongStateOf(0L) }
    val laps = remember { mutableStateListOf<LapRecord>() }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            startTimestamp = System.currentTimeMillis()
            pausedElapsed = elapsedMs
        }
    }

    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(10)
            elapsedMs = pausedElapsed + (System.currentTimeMillis() - startTimestamp)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        // Time display - centered
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.4f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = formatStopwatchTime(elapsedMs),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Lap list - scrollable
        Column(
            modifier = Modifier
                .weight(0.4f)
                .verticalScroll(rememberScrollState())
        ) {
            if (laps.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Lap",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Text(
                            text = "Lap time",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Overall",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            laps.forEach { lap ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Lap ${lap.lapNumber}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Text(
                            text = formatStopwatchTime(lap.lapTimeMs),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = formatStopwatchTime(lap.overallTimeMs),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.2f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                isRunning -> {
                    // Lap + Stop
                    Button(
                        onClick = {
                            val lastOverall = laps.lastOrNull()?.overallTimeMs ?: 0L
                            laps.add(
                                LapRecord(
                                    lapNumber = laps.size + 1,
                                    lapTimeMs = elapsedMs - lastOverall,
                                    overallTimeMs = elapsedMs
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text("Lap")
                    }
                    Button(
                        onClick = { isRunning = false },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text("Stop")
                    }
                }
                elapsedMs > 0 -> {
                    // Resume + Reset (stopped state)
                    Button(
                        onClick = { isRunning = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ClockAccent,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Resume")
                    }
                    Button(
                        onClick = {
                            elapsedMs = 0L
                            pausedElapsed = 0L
                            laps.clear()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text("Reset")
                    }
                }
                else -> {
                    // Initial: Lap (disabled) + Start
                    Button(
                        onClick = { },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        enabled = false,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Lap")
                    }
                    Button(
                        onClick = { isRunning = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ClockAccent,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Start")
                    }
                }
            }
        }
    }
}
