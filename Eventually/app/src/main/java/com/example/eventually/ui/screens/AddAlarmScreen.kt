package com.example.eventually.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.eventually.domain.model.AlarmItem
import com.example.eventually.ui.components.alarm.RecurringDaysSelector
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

private val DATE_FORMAT = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
private val DAY_ABBREVS = mapOf(
    Calendar.SUNDAY to "Sun",
    Calendar.MONDAY to "Mon",
    Calendar.TUESDAY to "Tue",
    Calendar.WEDNESDAY to "Wed",
    Calendar.THURSDAY to "Thu",
    Calendar.FRIDAY to "Fri",
    Calendar.SATURDAY to "Sat"
)

private fun formatDateLabel(calendar: Calendar): String {
    val today = Calendar.getInstance()
    return when {
        isSameDay(calendar, today) -> "Today"
        isTomorrow(calendar, today) -> "Tomorrow, ${DATE_FORMAT.format(calendar.time)}"
        else -> DATE_FORMAT.format(calendar.time)
    }
}

private fun isSameDay(c1: Calendar, c2: Calendar): Boolean =
    c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
        c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)

private fun isTomorrow(c1: Calendar, c2: Calendar): Boolean {
    val tomorrow = c2.clone() as Calendar
    tomorrow.add(Calendar.DAY_OF_YEAR, 1)
    return isSameDay(c1, tomorrow)
}

private fun formatRecurringDaysLabel(days: Set<Int>): String {
    if (days.isEmpty()) return "Never"
    if (days.size == 7) return "Every day"
    return days.toSortedSet().map { DAY_ABBREVS[it] ?: "" }.joinToString(", ")
}

private fun formatTime(hour: Int, minute: Int, is24Hour: Boolean): String {
    return if (is24Hour) {
        String.format("%02d:%02d", hour, minute)
    } else {
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val amPm = if (hour < 12) "AM" else "PM"
        String.format("%d:%02d %s", displayHour, minute, amPm)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlarmScreen(
    onSave: (AlarmItem) -> Unit,
    onCancel: () -> Unit,
) {
    val calendar = remember { Calendar.getInstance() }
    val timePickerState = rememberTimePickerState(
        initialHour = calendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = calendar.get(Calendar.MINUTE),
        is24Hour = false
    )

    var isRecurring by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDays by remember { mutableStateOf(setOf<Int>()) }
    var showDatePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("Add alarm") },
            navigationIcon = {
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        )

        // Time picker
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TimePicker(
                state = timePickerState,
                modifier = Modifier.padding(8.dp),
                colors = TimePickerDefaults.colors(
                    selectorColor = MaterialTheme.colorScheme.primary,
                    timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }

        // Date / Recurring section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRecurring) "Repeat" else "Date",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = { isRecurring = !isRecurring }) {
                        Text(
                            text = if (isRecurring) "One-time" else "Recurring",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (isRecurring) {
                    RecurringDaysSelector(
                        selectedDays = selectedDays,
                        onDayToggle = { day ->
                            selectedDays = if (day in selectedDays) {
                                selectedDays - day
                            } else {
                                selectedDays + day
                            }
                        }
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.padding(horizontal = 12.dp))
                        Text(
                            text = formatDateLabel(selectedDate),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = selectedDate.timeInMillis
            )
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { ms ->
                                selectedDate = Calendar.getInstance().apply { timeInMillis = ms }
                            }
                            showDatePicker = false
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Cancel / Save buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
            TextButton(
                onClick = {
                    val hour = timePickerState.hour
                    val minute = timePickerState.minute
                    val timeString = formatTime(hour, minute, is24Hour = false)
                    val label = if (isRecurring) {
                        formatRecurringDaysLabel(selectedDays)
                    } else {
                        formatDateLabel(selectedDate)
                    }
                    onSave(
                        AlarmItem(
                            id = UUID.randomUUID().toString(),
                            time = timeString,
                            label = label,
                            isEnabled = true,
                            isRecurring = isRecurring,
                            recurringDays = selectedDays,
                            triggerDateMs = if (isRecurring) 0L else selectedDate.timeInMillis
                        )
                    )
                }
            ) {
                Text("Save")
            }
        }
    }
}
