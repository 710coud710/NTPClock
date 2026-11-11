package com.example.ntpclock.presentation.poc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ntpclock.core.time.TimeZoneManager
import com.example.ntpclock.sync.SyncExecutionResult
import com.example.ntpclock.sync.SyncStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PocScreen(
    uiState: PocUiState,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onRefreshClick: () -> Unit,
    onDisableAutoTimeClick: () -> Unit,
    onApplyTimezoneClick: () -> Unit,
    onTestNtpClick: () -> Unit,
    onSyncClockClick: () -> Unit,
    onClearLogsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeZoneManager = remember { TimeZoneManager() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("NTPClock — Phase 0 PoC", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Kiểm chứng Device Owner & System Clock",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onRefreshClick, enabled = !uiState.isBusy) {
                        Text("Làm mới")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (uiState.isBusy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            SystemClockCard(uiState = uiState)
            PreflightGateCard(
                uiState = uiState,
                onDisableAutoTimeClick = onDisableAutoTimeClick,
                onApplyTimezoneClick = onApplyTimezoneClick,
            )
            NtpActionsCard(
                uiState = uiState,
                onHostChange = onHostChange,
                onPortChange = onPortChange,
                onTestNtpClick = onTestNtpClick,
                onSyncClockClick = onSyncClockClick,
            )
            SyncResultCard(
                result = uiState.lastSyncResult,
                timeZoneManager = timeZoneManager,
            )
            DeviceMetadataCard(uiState = uiState)
            EventLogCard(
                logs = uiState.logs,
                onClearLogsClick = onClearLogsClick,
            )
        }
    }
}

@Composable
private fun SystemClockCard(uiState: PocUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "ĐỒNG HỒ HỆ THỐNG HIỆN TẠI",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = uiState.currentSystemTimeText,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text = "${uiState.currentDateText} • Timezone: ${uiState.currentTimezoneId}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PreflightGateCard(
    uiState: PocUiState,
    onDisableAutoTimeClick: () -> Unit,
    onApplyTimezoneClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "KIỂM TRA ĐIỀU KIỆN GO/NO-GO",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            StatusRow("Kết nối Wi-Fi / LAN", uiState.isLanConnected, "Đã kết nối", "Chưa kết nối")
            StatusRow("Device Owner", uiState.isDeviceOwner, "Đạt (Device Owner)", "Chưa cấp quyền")
            StatusRow("Automatic Date & Time", !uiState.isAutoTimeEnabled, "Đã tắt (AUTO_TIME=0)", "Đang bật")
            StatusRow(
                "Timezone Việt Nam",
                uiState.isTargetTimezone,
                TimeZoneManager.TARGET_TIMEZONE_ID,
                uiState.currentTimezoneId,
            )

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onDisableAutoTimeClick,
                    enabled = !uiState.isBusy,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Tắt Auto Time")
                }
                OutlinedButton(
                    onClick = onApplyTimezoneClick,
                    enabled = !uiState.isBusy,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Đặt TZ UTC+7")
                }
            }
        }
    }
}

@Composable
private fun NtpActionsCard(
    uiState: PocUiState,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onTestNtpClick: () -> Unit,
    onSyncClockClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "MÁY CHỦ NTP & ĐỒNG BỘ",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = uiState.ntpHost,
                    onValueChange = onHostChange,
                    label = { Text("NTP Host") },
                    singleLine = true,
                    enabled = !uiState.isBusy,
                    modifier = Modifier.weight(2f),
                )
                OutlinedTextField(
                    value = uiState.ntpPort,
                    onValueChange = onPortChange,
                    label = { Text("Port") },
                    singleLine = true,
                    enabled = !uiState.isBusy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onTestNtpClick,
                    enabled = !uiState.isBusy,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Test NTP (Chỉ đọc)")
                }
                Button(
                    onClick = onSyncClockClick,
                    enabled = !uiState.isBusy,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Đồng bộ ngay")
                }
            }
        }
    }
}

@Composable
private fun SyncResultCard(
    result: SyncExecutionResult?,
    timeZoneManager: TimeZoneManager,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "KẾT QUẢ LẦN CHẠY GẦN NHẤT",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            if (result == null) {
                Text(
                    "Chưa chạy kiểm thử NTP nào.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }

            val badgeContainer = when (result.status) {
                SyncStatus.CLOCK_SYNC_SUCCESS, SyncStatus.NTP_SUCCESS -> MaterialTheme.colorScheme.primaryContainer
                SyncStatus.CLOCK_SYNC_DENIED, SyncStatus.NTP_FAILED, SyncStatus.CLOCK_SYNC_VERIFY_FAILED ->
                    MaterialTheme.colorScheme.errorContainer
            }
            Surface(color = badgeContainer, shape = MaterialTheme.shapes.small) {
                Text(
                    text = result.status.name,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Text(result.message, style = MaterialTheme.typography.bodyMedium)

            result.sample?.let { sample ->
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = "Server: ${sample.serverHost}:${sample.serverPort} (VN=${sample.version}, Stratum=${sample.stratum}, LI=${sample.leapIndicator})",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    text = "Offset: ${sample.offsetMillis} ms | Delay(RTT): ${sample.roundTripDelayMillis} ms",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                )
            }
            result.targetEpochMillis?.let { target ->
                Text(
                    text = "Trước: ${timeZoneManager.formatDateTime(result.beforeEpochMillis)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                )
                Text(
                    text = "Mục tiêu: ${timeZoneManager.formatDateTime(target)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                )
            }
            result.afterEpochMillis?.let { after ->
                Text(
                    text = "Sau đặt: ${timeZoneManager.formatDateTime(after)} (lệch=${result.postSyncDiffMillis} ms)",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                )
            }
        }
    }
}

@Composable
private fun DeviceMetadataCard(uiState: PocUiState) {
    val profile = uiState.deviceProfile
    val adbCommand = "adb shell dpm set-device-owner ${uiState.receiverComponentName}"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "HỒ SƠ FIRMWARE & PROVISIONING ADB",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Model: ${profile.manufacturer} ${profile.model} (${profile.device})",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "Android ${profile.androidVersion} (API ${profile.apiLevel}) • Patch: ${profile.securityPatch}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "Fingerprint: ${profile.buildFingerprint}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Lệnh ADB Device Owner:",
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        text = adbCommand,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    )
                }
            }
        }
    }
}

@Composable
private fun EventLogCard(
    logs: List<String>,
    onClearLogsClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "NHẬT KÝ KIỂM THỬ (${logs.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                TextButton(onClick = onClearLogsClick, enabled = logs.isNotEmpty()) {
                    Text("Xóa log")
                }
            }
            if (logs.isEmpty()) {
                Text(
                    "Chưa có log.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                logs.forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    ok: Boolean,
    okText: String,
    failText: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Surface(
            color = if (ok) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
            shape = MaterialTheme.shapes.small,
        ) {
            Text(
                text = if (ok) okText else failText,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

