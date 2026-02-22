package com.example.eventually.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.eventually.ui.theme.ClockAccent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private fun formatTimerTime(ms: Long): String {
    if (ms <= 0) return "00:00:00"
    val totalSeconds = (ms / 1000).toInt()
    val seconds = totalSeconds % 60
    val totalMinutes = totalSeconds / 60
    val minutes = totalMinutes % 60
    val hours = totalMinutes / 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}

@Composable
private fun ScrollableNumberPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = value.coerceIn(range)
    )
    val scope = rememberCoroutineScope()

    LaunchedEffect(value) {
        if (value != listState.firstVisibleItemIndex && value in range) {
            listState.animateScrollToItem(value)
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo }
            .distinctUntilChanged()
            .collect { layoutInfo ->
                val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset
                val center = layoutInfo.viewportStartOffset + viewportHeight / 2
                val selectedItem = layoutInfo.visibleItemsInfo.minByOrNull { item ->
                    val itemCenter = item.offset + item.size / 2
                    kotlin.math.abs(itemCenter - center)
                }
                selectedItem?.index?.let { index ->
                    if (index in range && index != value) {
                        onValueChange(index)
                    }
                }
            }
    }

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 40.dp)
        ) {
            itemsIndexed(range.toList()) { index, _ ->
                Text(
                    text = "%02d".format(index),
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (index == value) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun TimerScreen() {
    var hours by remember { mutableIntStateOf(0) }
    var minutes by remember { mutableIntStateOf(0) }
    var seconds by remember { mutableIntStateOf(0) }
    var remainingMs by remember { mutableLongStateOf(0L) }
    var isRunning by remember { mutableStateOf(false) }
    var startTimestamp by remember { mutableLongStateOf(0L) }
    var pausedRemaining by remember { mutableLongStateOf(0L) }
    var showPicker by remember { mutableStateOf(true) }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            startTimestamp = System.currentTimeMillis()
            pausedRemaining = remainingMs
        }
    }

    LaunchedEffect(isRunning) {
        while (isRunning) {
            delay(100)
            remainingMs = (pausedRemaining - (System.currentTimeMillis() - startTimestamp)).coerceAtLeast(0)
            if (remainingMs <= 0) {
                isRunning = false
                showPicker = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        if (showPicker) {
            // Picker mode
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ScrollableNumberPicker(
                            value = hours,
                            range = 0..23,
                            onValueChange = { hours = it },
                            modifier = Modifier.size(width = 80.dp, height = 120.dp)
                        )
                        Text(
                            text = "HOUR",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ScrollableNumberPicker(
                            value = minutes,
                            range = 0..59,
                            onValueChange = { minutes = it },
                            modifier = Modifier.size(width = 80.dp, height = 120.dp)
                        )
                        Text(
                            text = "MIN",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ScrollableNumberPicker(
                            value = seconds,
                            range = 0..59,
                            onValueChange = { seconds = it },
                            modifier = Modifier.size(width = 80.dp, height = 120.dp)
                        )
                        Text(
                            text = "SEC",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Start button
            Button(
                onClick = {
                    val totalMs = (hours * 3600L + minutes * 60L + seconds) * 1000
                    if (totalMs > 0) {
                        remainingMs = totalMs
                        isRunning = true
                        showPicker = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ClockAccent,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Start")
            }
        } else {
            // Countdown mode
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = formatTimerTime(remainingMs),
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Stop + Delete (when running) or Resume + Delete (when stopped)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isRunning) {
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
                } else {
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
                }
                Button(
                    onClick = {
                        isRunning = false
                        remainingMs = 0L
                        pausedRemaining = 0L
                        showPicker = true
                        hours = 0
                        minutes = 0
                        seconds = 0
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("Delete")
                }
            }
        }
    }
}
