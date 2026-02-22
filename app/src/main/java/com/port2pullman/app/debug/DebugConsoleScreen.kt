package com.port2pullman.app.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val TerminalBg = Color(0xFF1E1E2E)
private val TerminalText = Color(0xFFCDD6F4)
private val DebugColor = Color(0xFF89B4FA)
private val InfoColor = Color(0xFFA6E3A1)
private val WarnColor = Color(0xFFF9E2AF)
private val ErrorColor = Color(0xFFF38BA8)
private val TagColor = Color(0xFFCBA6F7)   // Purple for tags
private val TimestampColor = Color(0xFF6C7086)
private val StubColor = Color(0xFFFAB387)  // Peach for stubs

@Composable
fun DebugConsoleScreen(
    onBack: () -> Unit,
) {
    val entries by DebugLog.entries.collectAsState()
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Logs", "API Debug", "Settings")

    // Auto-scroll log to bottom on new entries
    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty() && selectedTab == 0) {
            listState.animateScrollToItem(entries.size - 1)
        }
    }

    Scaffold(
        containerColor = TerminalBg,
        topBar = {
            Column(
                modifier = Modifier
                    .background(Color(0xFF181825))
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TerminalText
                        )
                    }
                    Text(
                        "Debug Console",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = TagColor,
                        modifier = Modifier.weight(1f)
                    )
                    if (selectedTab == 0) {
                        TextButton(onClick = {
                            val text = entries.joinToString("\n") { e ->
                                "${e.timestamp} ${e.level.name} [${e.tag}] ${e.message}"
                            }
                            clipboardManager.setText(AnnotatedString(text))
                        }) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = InfoColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Copy", color = InfoColor, fontSize = 13.sp)
                        }
                        TextButton(onClick = { DebugLog.clear() }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Clear",
                                tint = ErrorColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Clear", color = ErrorColor, fontSize = 13.sp)
                        }
                    }
                }
                // Tab row
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF181825),
                    contentColor = TagColor,
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    title,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                            },
                            selectedContentColor = TagColor,
                            unselectedContentColor = TimestampColor,
                        )
                    }
                }
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> LogsTab(entries, listState, padding)
            1 -> ApiDebugTab(padding)
            2 -> SettingsTab(padding)
        }
    }
}

// ─── Logs Tab ───────────────────────────────────────────────────────────

@Composable
private fun LogsTab(
    entries: List<DebugLog.Entry>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    padding: PaddingValues,
) {
    if (entries.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No log entries yet.\nInteract with the app to generate logs.",
                color = TimestampColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(entries) { entry ->
                LogEntry(entry)
            }
        }
    }
}

// ─── API Debug Tab ──────────────────────────────────────────────────────

@Composable
private fun ApiDebugTab(padding: PaddingValues) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var probeResults by remember { mutableStateOf<List<ConditionProbe.ProbeResult>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var lastRefresh by remember { mutableStateOf("—") }
    val clipboardManager = LocalClipboardManager.current

    // Inline suspend helper (runs inside LaunchedEffect's own scope)
    suspend fun doRefreshSuspend(force: Boolean = false) {
        loading = true
        try {
            probeResults = ConditionProbe.probeAll(context, forceRefresh = force)
            lastRefresh = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US)
                .format(java.util.Date())
        } catch (_: kotlinx.coroutines.CancellationException) {
            throw kotlinx.coroutines.CancellationException("scope cancelled")
        } catch (e: Exception) {
            // Probe failed silently — keep previous results
        } finally {
            loading = false
        }
    }

    // Auto-refresh at the interval set in DebugSettings while this tab is visible.
    // Runs in LaunchedEffect's own scope (auto-cancelled on leave).
    LaunchedEffect(Unit) {
        doRefreshSuspend(force = true)
        while (true) {
            kotlinx.coroutines.delay(DebugSettings.apiDebugRefreshMs)
            doRefreshSuspend(force = false)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        // Control bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF181825))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Last: $lastRefresh",
                color = TimestampColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = {
                val text = probeResults.joinToString("\n") { r ->
                    "[${r.status}] ${r.key} = ${r.value}" +
                            if (r.detail.isNotEmpty()) "  (${r.detail})" else ""
                }
                clipboardManager.setText(AnnotatedString(text))
            }) {
                Icon(Icons.Default.ContentCopy, "Copy", tint = InfoColor,
                    modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Copy", color = InfoColor, fontSize = 12.sp)
            }
            TextButton(
                onClick = {
                    scope.launch {
                        loading = true
                        try {
                            probeResults = ConditionProbe.probeAll(context, forceRefresh = true)
                            lastRefresh = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US)
                                .format(java.util.Date())
                        } catch (_: Exception) { }
                        finally { loading = false }
                    }
                },
                enabled = !loading,
            ) {
                Icon(Icons.Default.Refresh, "Refresh", tint = DebugColor,
                    modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Refresh", color = DebugColor, fontSize = 12.sp)
            }
        }

        if (loading && probeResults.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = TagColor, strokeWidth = 2.dp)
            }
        } else {
            // Group by category
            val grouped = probeResults.groupBy { it.category }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                grouped.forEach { (category, results) ->
                    item(key = "header_$category") {
                        CategoryHeader(category, results)
                    }
                    items(results, key = { "${category}_${it.key}" }) { result ->
                        ProbeKeyRow(result)
                    }
                    item(key = "spacer_$category") {
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryHeader(
    category: String,
    results: List<ConditionProbe.ProbeResult>,
) {
    val okCount = results.count { it.status == ConditionProbe.Status.OK }
    val total = results.size
    val summaryColor = when {
        okCount == total -> InfoColor
        okCount > 0 -> WarnColor
        else -> ErrorColor
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF313244), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            category.uppercase(),
            color = TagColor,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            "$okCount/$total OK",
            color = summaryColor,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun ProbeKeyRow(result: ConditionProbe.ProbeResult) {
    val statusColor = when (result.status) {
        ConditionProbe.Status.OK -> InfoColor
        ConditionProbe.Status.STUB -> StubColor
        ConditionProbe.Status.NO_PERMISSION -> WarnColor
        ConditionProbe.Status.ERROR -> ErrorColor
    }
    val statusLabel = when (result.status) {
        ConditionProbe.Status.OK -> "OK"
        ConditionProbe.Status.STUB -> "STUB"
        ConditionProbe.Status.NO_PERMISSION -> "PERM"
        ConditionProbe.Status.ERROR -> "ERR"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let {
                if (result.status == ConditionProbe.Status.ERROR) {
                    it.background(ErrorColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                } else it
            }
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Status badge
        Text(
            statusLabel,
            color = statusColor,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            modifier = Modifier
                .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        )
        Spacer(Modifier.width(6.dp))
        // Probe key name
        Text(
            result.key,
            color = TerminalText,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(8.dp))
        // Value (or detail for errors/perms)
        Text(
            if (result.detail.isNotEmpty() && result.status != ConditionProbe.Status.OK)
                result.detail else result.value,
            color = if (result.status == ConditionProbe.Status.OK) InfoColor else statusColor,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LogEntry(entry: DebugLog.Entry) {
    val levelColor = when (entry.level) {
        DebugLog.Level.DEBUG -> DebugColor
        DebugLog.Level.INFO -> InfoColor
        DebugLog.Level.WARN -> WarnColor
        DebugLog.Level.ERROR -> ErrorColor
    }
    val levelLabel = when (entry.level) {
        DebugLog.Level.DEBUG -> "DBG"
        DebugLog.Level.INFO -> "INF"
        DebugLog.Level.WARN -> "WRN"
        DebugLog.Level.ERROR -> "ERR"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let {
                if (entry.level == DebugLog.Level.ERROR) {
                    it
                        .background(
                            ErrorColor.copy(alpha = 0.1f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                } else {
                    it.padding(horizontal = 4.dp, vertical = 1.dp)
                }
            }
            .horizontalScroll(rememberScrollState())
    ) {
        Text(
            entry.timestamp,
            color = TimestampColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            levelLabel,
            color = levelColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            "[${entry.tag}]",
            color = TagColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            entry.message,
            color = if (entry.level == DebugLog.Level.ERROR) ErrorColor else TerminalText,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
        )
    }
}

// ─── Settings Tab ───────────────────────────────────────────────────────

@Composable
private fun SettingsTab(padding: PaddingValues) {
    var evalInterval by remember { mutableStateOf(DebugSettings.evalIntervalMs.toString()) }
    var locMinTime by remember { mutableStateOf(DebugSettings.locationMinTimeMs.toString()) }
    var locMinDist by remember { mutableStateOf(DebugSettings.locationMinDistanceM.toString()) }
    var weatherTtl by remember { mutableStateOf(DebugSettings.weatherCacheTtlMs.toString()) }
    var apiRefresh by remember { mutableStateOf(DebugSettings.apiDebugRefreshMs.toString()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 12.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // ── Alarm Evaluation ──
        item {
            SettingSectionHeader("Alarm Evaluation")
        }
        item {
            IntervalRow(
                label = "Eval interval",
                unit = "ms",
                value = evalInterval,
                onValueChange = { evalInterval = it },
                onApply = {
                    evalInterval.toLongOrNull()?.let { v ->
                        DebugSettings.evalIntervalMs = v.coerceAtLeast(1_000L)
                        evalInterval = DebugSettings.evalIntervalMs.toString()
                    }
                },
                hint = "default 15000",
            )
        }

        // ── GPS / Location ──
        item {
            SettingSectionHeader("GPS / Location")
        }
        item {
            IntervalRow(
                label = "Min time",
                unit = "ms",
                value = locMinTime,
                onValueChange = { locMinTime = it },
                onApply = {
                    locMinTime.toLongOrNull()?.let { v ->
                        DebugSettings.locationMinTimeMs = v.coerceAtLeast(1_000L)
                        locMinTime = DebugSettings.locationMinTimeMs.toString()
                        DebugSettings.restartLocationCallback?.invoke()
                    }
                },
                hint = "default 30000",
            )
        }
        item {
            IntervalRow(
                label = "Min distance",
                unit = "m",
                value = locMinDist,
                onValueChange = { locMinDist = it },
                onApply = {
                    locMinDist.toFloatOrNull()?.let { v ->
                        DebugSettings.locationMinDistanceM = v.coerceAtLeast(0f)
                        locMinDist = DebugSettings.locationMinDistanceM.toString()
                        DebugSettings.restartLocationCallback?.invoke()
                    }
                },
                hint = "default 10.0",
            )
        }

        // ── Weather ──
        item {
            SettingSectionHeader("Weather Cache")
        }
        item {
            IntervalRow(
                label = "Cache TTL",
                unit = "ms",
                value = weatherTtl,
                onValueChange = { weatherTtl = it },
                onApply = {
                    weatherTtl.toLongOrNull()?.let { v ->
                        DebugSettings.weatherCacheTtlMs = v.coerceAtLeast(10_000L)
                        weatherTtl = DebugSettings.weatherCacheTtlMs.toString()
                    }
                },
                hint = "default 600000",
            )
        }

        // ── Debug API Tab ──
        item {
            SettingSectionHeader("API Debug Tab")
        }
        item {
            IntervalRow(
                label = "Auto-refresh",
                unit = "ms",
                value = apiRefresh,
                onValueChange = { apiRefresh = it },
                onApply = {
                    apiRefresh.toLongOrNull()?.let { v ->
                        DebugSettings.apiDebugRefreshMs = v.coerceAtLeast(1_000L)
                        apiRefresh = DebugSettings.apiDebugRefreshMs.toString()
                    }
                },
                hint = "default 5000",
            )
        }

        // ── Reset all ──
        item { Spacer(Modifier.height(16.dp)) }
        item {
            OutlinedButton(
                onClick = {
                    DebugSettings.evalIntervalMs = 15_000L
                    DebugSettings.locationMinTimeMs = 30_000L
                    DebugSettings.locationMinDistanceM = 10f
                    DebugSettings.weatherCacheTtlMs = 10 * 60_000L
                    DebugSettings.apiDebugRefreshMs = 5_000L
                    evalInterval = "15000"
                    locMinTime = "30000"
                    locMinDist = "10.0"
                    weatherTtl = "600000"
                    apiRefresh = "5000"
                    DebugSettings.restartLocationCallback?.invoke()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = WarnColor),
            ) {
                Text("Reset All to Defaults", fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SettingSectionHeader(title: String) {
    Spacer(Modifier.height(8.dp))
    Text(
        title.uppercase(),
        color = TagColor,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF313244), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@Composable
private fun IntervalRow(
    label: String,
    unit: String,
    value: String,
    onValueChange: (String) -> Unit,
    onApply: () -> Unit,
    hint: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            color = TerminalText,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .width(130.dp)
                .height(48.dp),
            textStyle = androidx.compose.ui.text.TextStyle(
                color = InfoColor,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
            ),
            placeholder = {
                Text(hint, color = TimestampColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TagColor,
                unfocusedBorderColor = TimestampColor,
                cursorColor = TagColor,
            ),
        )
        Spacer(Modifier.width(4.dp))
        Text(unit, color = TimestampColor, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        Spacer(Modifier.width(4.dp))
        TextButton(
            onClick = onApply,
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        ) {
            Text("Apply", color = DebugColor, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
    }
}
