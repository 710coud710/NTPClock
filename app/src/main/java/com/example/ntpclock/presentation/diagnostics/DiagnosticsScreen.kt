package com.example.ntpclock.presentation.diagnostics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.ntpclock.presentation.components.StatusRow

@Composable
fun DiagnosticsScreen(
    uiState: DiagnosticsUiState,
    onRefreshClick: () -> Unit,
    onDisableAutoTimeClick: () -> Unit,
    onApplyTimezoneClick: () -> Unit,
    onEnqueueWorkerClick: () -> Unit,
    onClearLogsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile = uiState.deviceProfile
    val adbCommand = "adb shell dpm set-device-owner ${uiState.receiverComponentName}"

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "QUẢN TRỊ THIẾT BỊ & POLICY",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    TextButton(onClick = onRefreshClick) {
                        Text("Làm mới")
                    }
                }

                StatusRow("Device Owner", uiState.isDeviceOwner, "true", "false")
                StatusRow("Device Admin Active", uiState.isAdminActive, "true", "false")
                StatusRow("Automatic Date & Time", !uiState.isAutoTimeEnabled, "OFF (0)", "ON (1)")
                StatusRow("Automatic Time Zone", !uiState.isAutoTimeZoneEnabled, "OFF (0)", "ON (1)")
                StatusRow("LAN / Wi-Fi", uiState.isLanConnected, "Connected", "Disconnected")
                Text(
                    text = "Timezone hiện tại: ${uiState.currentTimezoneId} • WorkManager: ${uiState.periodicWorkState}",
                    style = MaterialTheme.typography.bodySmall,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onDisableAutoTimeClick,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Tắt Auto Time")
                    }
                    OutlinedButton(
                        onClick = onApplyTimezoneClick,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Đặt Timezone")
                    }
                }
                OutlinedButton(
                    onClick = onEnqueueWorkerClick,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Chạy WorkManager Sync ngay")
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "PROVISIONING DEVICE OWNER (ADB)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Yêu cầu: Thiết bị chưa đăng nhập tài khoản Google/Xiaomi (hoặc đã Factory Reset) và bật USB Debugging.",
                    style = MaterialTheme.typography.bodySmall,
                )
                SelectionContainer {
                    Text(
                        text = adbCommand,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "HỒ SƠ PHẦN CỨNG & FIRMWARE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                SelectionContainer {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Model: ${profile.manufacturer} ${profile.model} (${profile.device})",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = "Android ${profile.androidVersion} (API ${profile.apiLevel}) • Patch: ${profile.securityPatch}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = "Fingerprint: ${profile.buildFingerprint}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        )
                    }
                }
            }
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
                        text = "NHẬT KÝ DIAGNOSTICS (${uiState.logs.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    TextButton(onClick = onClearLogsClick, enabled = uiState.logs.isNotEmpty()) {
                        Text("Xóa")
                    }
                }
                if (uiState.logs.isEmpty()) {
                    Text(
                        text = "Chưa có log.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    uiState.logs.forEach { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        )
                    }
                }
            }
        }
    }
}

