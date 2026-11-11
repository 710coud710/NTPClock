package com.example.ntpclock.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ntpclock.core.time.TimeZoneManager
import com.example.ntpclock.presentation.components.StatusRow
import com.example.ntpclock.presentation.components.SyncStatusBadge

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onSyncNowClick: () -> Unit,
    onTestNtpClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onOpenDiagnosticsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeZoneManager = remember { TimeZoneManager() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (uiState.isBusy) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "ĐỒNG HỒ HỆ THỐNG",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    TextButton(onClick = onRefreshClick, enabled = !uiState.isBusy) {
                        Text("Làm mới")
                    }
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = uiState.currentSystemTimeText,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Text(
                        text = uiState.currentMillisText,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Monospace,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
                Text(
                    text = "${uiState.currentDateText} • ${uiState.currentTimezoneId}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "NTP Server: ${uiState.settings.host}:${uiState.settings.port} (${uiState.settings.sampleCount} mẫu/lần)",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                )
            }
        }

        if (!uiState.isDeviceOwner || uiState.isAutoTimeEnabled) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "CẦN HOÀN TẤT CẤU HÌNH QUẢN TRỊ",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    if (!uiState.isDeviceOwner) {
                        Text(
                            text = "• Thiết bị chưa được provision Device Owner. Vào tab Diagnostics để xem lệnh ADB và kiểm tra điều kiện.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                    if (uiState.isAutoTimeEnabled) {
                        Text(
                            text = "• Automatic Date & Time đang bật. Cần tắt để DevicePolicyManager.setTime() có hiệu lực.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                    OutlinedButton(onClick = onOpenDiagnosticsClick) {
                        Text("Mở Diagnostics")
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "TRẠNG THÁI HỆ THỐNG",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                StatusRow("Kết nối LAN / Wi-Fi", uiState.isLanConnected, "Đã kết nối", "Mất kết nối")
                StatusRow("Device Owner", uiState.isDeviceOwner, "Đã kích hoạt", "Chưa provision")
                StatusRow("Automatic Time", !uiState.isAutoTimeEnabled, "Đã tắt (OFF)", "Đang bật (ON)")
                StatusRow(
                    "Lịch định kỳ (${uiState.settings.syncIntervalMinutes} phút)",
                    uiState.settings.autoSyncEnabled,
                    uiState.periodicWorkStateText,
                    "Đang tắt",
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onTestNtpClick,
                        enabled = !uiState.isBusy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Test NTP")
                    }
                    Button(
                        onClick = onSyncNowClick,
                        enabled = !uiState.isBusy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Đồng bộ ngay")
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "PHIÊN ĐỒNG BỘ GẦN NHẤT",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )

                val lastRun = uiState.lastSyncResult
                if (lastRun != null) {
                    SyncStatusBadge(status = lastRun.status)
                    Text(lastRun.message, style = MaterialTheme.typography.bodyMedium)
                    lastRun.sample?.let { sample ->
                        Text(
                            text = "Offset: ${sample.offsetMillis} ms | RTT: ${sample.roundTripDelayMillis} ms | Stratum: ${sample.stratum}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }

                val latestSuccess = uiState.latestSuccessfulSync
                if (latestSuccess != null) {
                    Text(
                        text = "Thành công gần nhất (${latestSuccess.triggerSource}): ${
                            timeZoneManager.formatDateTime(latestSuccess.runAtEpochMillis)
                        }",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        text = "Offset đo được: ${latestSuccess.offsetMillis ?: "-"} ms • RTT: ${latestSuccess.rttMillis ?: "-"} ms",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    )
                } else {
                    Text(
                        text = "Chưa có lần đồng bộ thành công nào được lưu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

