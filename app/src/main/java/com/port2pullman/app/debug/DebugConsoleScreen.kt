package com.port2pullman.app.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val TerminalBg = Color(0xFF1E1E2E)
private val TerminalText = Color(0xFFCDD6F4)
private val DebugColor = Color(0xFF89B4FA)
private val InfoColor = Color(0xFFA6E3A1)
private val WarnColor = Color(0xFFF9E2AF)
private val ErrorColor = Color(0xFFF38BA8)
private val TagColor = Color(0xFFCBA6F7)   // Purple for tags
private val TimestampColor = Color(0xFF6C7086)

@Composable
fun DebugConsoleScreen(
    onBack: () -> Unit,
) {
    val entries by DebugLog.entries.collectAsState()
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current

    // Auto-scroll to bottom on new entries
    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) {
            listState.animateScrollToItem(entries.size - 1)
        }
    }

    Scaffold(
        containerColor = TerminalBg,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF181825))
                    .statusBarsPadding()
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
    ) { padding ->
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
