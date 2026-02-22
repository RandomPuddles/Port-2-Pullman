package com.example.eventually.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.eventually.ui.theme.ClockAccent

data class ClockNavItem(
    val route: ClockRoute,
    val label: String,
    val icon: ImageVector
)

val clockNavItems = listOf(
    ClockNavItem(ClockRoute.Alarm, "Alarm", Icons.Default.Alarm),
    ClockNavItem(ClockRoute.WorldClock, "World clock", Icons.Default.Public),
    ClockNavItem(ClockRoute.Stopwatch, "Stopwatch", Icons.Default.Timer),
    ClockNavItem(ClockRoute.Timer, "Timer", Icons.Default.HourglassEmpty),
    ClockNavItem(ClockRoute.Event, "Event", Icons.Default.Event)
)

@Composable
fun ClockBottomNav(
    currentRoute: String?,
    onItemClick: (ClockRoute) -> Unit
) {
    NavigationBar {
        clockNavItems.forEach { item ->
            val selected = currentRoute == item.route.route
            NavigationBarItem(
                selected = selected,
                onClick = { onItemClick(item.route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ClockAccent,
                    selectedTextColor = ClockAccent,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            )
        }
    }
}
