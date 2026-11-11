package com.example.ntpclock.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onToggleAdminUnlock: () -> Unit,
    onHostChange: (String) -> Unit,
    onPortChange: (String) -> Unit,
    onTimeZoneChange: (String) -> Unit,
    onSyncIntervalChange: (String) -> Unit,
    onSampleCountChange: (String) -> Unit,
    onTimeoutChange: (String) -> Unit,
    onMaxRttChange: (String) -> Unit,
    onMaxOffsetChange: (String) -> Unit,
    onAutoSyncToggle: (Boolean) -> Unit,
    onSyncOnBootToggle: (Boolean) -> Unit,
    onSaveClick: () -> Unit,
    onResetDefaultsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val editable = !uiState.settingsLockedByPolicy || uiState.isAdminUnlocked

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Chế độ Quản trị (Admin)",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = if (editable) "Đang mở khóa chỉnh sửa cấu hình NTP" else "Đang khóa sửa NTP host/policy đối với người dùng thường",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onToggleAdminUnlock) {
                    Text(if (uiState.isAdminUnlocked) "Khóa lại" else "Mở khóa")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "CẤU HÌNH MÁY CHỦ & GIAO THỨC NTP",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = uiState.host,
                        onValueChange = onHostChange,
                        label = { Text("NTP Host") },
                        enabled = editable,
                        singleLine = true,
                        modifier = Modifier.weight(2f),
                    )
                    OutlinedTextField(
                        value = uiState.port,
                        onValueChange = onPortChange,
                        label = { Text("Port") },
                        enabled = editable,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }

                OutlinedTextField(
                    value = uiState.timeZoneId,
                    onValueChange = onTimeZoneChange,
                    label = { Text("Timezone") },
                    enabled = editable,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = uiState.syncIntervalMinutes,
                        onValueChange = onSyncIntervalChange,
                        label = { Text("Chu kỳ (phút, >=15)") },
                        enabled = editable,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = uiState.sampleCount,
                        onValueChange = onSampleCountChange,
                        label = { Text("Số mẫu (1..10)") },
                        enabled = editable,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = uiState.timeoutMs,
                        onValueChange = onTimeoutChange,
                        label = { Text("Timeout (ms)") },
                        enabled = editable,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = uiState.maxAcceptableRttMs,
                        onValueChange = onMaxRttChange,
                        label = { Text("Max RTT (ms)") },
                        enabled = editable,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }

                OutlinedTextField(
                    value = uiState.maxOffsetBeforeSetMs,
                    onValueChange = onMaxOffsetChange,
                    label = { Text("Max offset trước khi đặt (ms, 0 = không giới hạn)") },
                    enabled = editable,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "ĐỒNG BỘ TỰ ĐỘNG",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Tự đồng bộ định kỳ (WorkManager)", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = uiState.autoSyncEnabled,
                        onCheckedChange = onAutoSyncToggle,
                        enabled = editable,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Đồng bộ sau khi khởi động máy", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = uiState.syncOnBootEnabled,
                        onCheckedChange = onSyncOnBootToggle,
                        enabled = editable,
                    )
                }
            }
        }

        uiState.statusMessage?.let { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onResetDefaultsClick,
                enabled = editable,
                modifier = Modifier.weight(1f),
            ) {
                Text("Mặc định")
            }
            Button(
                onClick = onSaveClick,
                enabled = editable,
                modifier = Modifier.weight(1f),
            ) {
                Text("Lưu cấu hình")
            }
        }
    }
}

