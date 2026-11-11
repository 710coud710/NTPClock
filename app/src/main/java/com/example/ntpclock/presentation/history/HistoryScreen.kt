package com.example.ntpclock.presentation.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.ntpclock.core.time.TimeZoneManager
import com.example.ntpclock.domain.model.SyncHistoryRecord
import com.example.ntpclock.presentation.components.SyncStatusBadge

@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    onClearHistoryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeZoneManager = remember { TimeZoneManager() }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Lịch sử đồng bộ (${uiState.records.size})",
                style = MaterialTheme.typography.titleMedium,
            )
            TextButton(
                onClick = onClearHistoryClick,
                enabled = uiState.records.isNotEmpty(),
            ) {
                Text("Xóa tất cả")
            }
        }

        if (uiState.records.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Chưa có lịch sử đồng bộ nào.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(uiState.records, key = { it.id }) { record ->
                    HistoryItemCard(record = record, timeZoneManager = timeZoneManager)
                }
            }
        }
    }
}

@Composable
private fun HistoryItemCard(
    record: SyncHistoryRecord,
    timeZoneManager: TimeZoneManager,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SyncStatusBadge(status = record.status)
                Text(
                    text = record.triggerSource.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = "Chạy lúc: ${timeZoneManager.formatDateTime(record.runAtEpochMillis)}",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            )
            Text(
                text = "Server: ${record.ntpServer}:${record.ntpPort} | Mẫu: ${record.samplesValid}/${record.samplesAttempted} | API: ${record.apiSuccess}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = "Offset: ${record.offsetMillis ?: "-"} ms | RTT: ${record.rttMillis ?: "-"} ms | Stratum: ${record.stratum ?: "-"} | LI: ${record.leapIndicator ?: "-"}",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            )
            Text(
                text = "Trước: ${timeZoneManager.formatDateTime(record.beforeEpochMillis)}",
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            )
            record.targetEpochMillis?.let { target ->
                Text(
                    text = "Mục tiêu: ${timeZoneManager.formatDateTime(target)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                )
            }
            record.afterEpochMillis?.let { after ->
                Text(
                    text = "Sau đặt: ${timeZoneManager.formatDateTime(after)} (lệch=${record.postSyncDiffMillis ?: "-"} ms)",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                )
            }
            record.errorMessage?.let { error ->
                Text(
                    text = "Chi tiết: $error",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

