package com.example.ntpclock.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ntpclock.core.time.TimeZoneManager
import com.example.ntpclock.domain.model.SyncHistoryRecord
import com.example.ntpclock.presentation.components.NtpClockHeader
import com.example.ntpclock.sync.SyncStatus

@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    onClearHistoryClick: () -> Unit,
    onFilterSelect: (HistoryFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeZoneManager = remember { TimeZoneManager() }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White,
            title = {
                Text(
                    text = "Xác nhận xóa lịch sử",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                    ),
                )
            },
            text = {
                Text(
                    text = "Bạn có chắc chắn muốn xóa toàn bộ lịch sử đồng bộ không? Hành động này không thể hoàn tác.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF475569),
                    ),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearHistoryClick()
                        showClearConfirmDialog = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                ) {
                    Text("Xóa tất cả")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Hủy", color = Color(0xFF64748B))
                }
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(start = 16.dp, top = 16.dp, end = 16.dp),
    ) {
        // Shared Reusable Header
        NtpClockHeader()

        Spacer(modifier = Modifier.height(8.dp))

        // Title Row & Delete All Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Lịch sử",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        fontSize = 26.sp,
                    ),
                )
            }

            TextButton(
                onClick = { showClearConfirmDialog = true },
                enabled = uiState.rawRecords.isNotEmpty(),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = if (uiState.rawRecords.isNotEmpty()) Color(0xFFDC2626) else Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Xóa tất cả",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.rawRecords.isNotEmpty()) Color(0xFFDC2626) else Color(0xFF94A3B8),
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Stat Summary Card (Thẻ thống kê)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "${uiState.totalCount}",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2563EB),
                            fontSize = 28.sp,
                        ),
                    )
                    Text(
                        text = "Tổng lịch sử",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF64748B),
                            fontSize = 13.sp,
                        ),
                    )
                }

                Column {
                    Text(
                        text = "${uiState.successCount}",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A),
                            fontSize = 28.sp,
                        ),
                    )
                    Text(
                        text = "Thành công",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF64748B),
                            fontSize = 13.sp,
                        ),
                    )
                }

                Column {
                    Text(
                        text = "${uiState.warningCount}",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD97706),
                            fontSize = 28.sp,
                        ),
                    )
                    Text(
                        text = "Cần kiểm tra",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF64748B),
                            fontSize = 13.sp,
                        ),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Filter Chips Row (Tất cả / Thành công / Cảnh báo)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChipItem(
                label = "Tất cả",
                isSelected = uiState.filter == HistoryFilter.ALL,
                onClick = { onFilterSelect(HistoryFilter.ALL) },
            )
            FilterChipItem(
                label = "Thành công",
                isSelected = uiState.filter == HistoryFilter.SUCCESS,
                onClick = { onFilterSelect(HistoryFilter.SUCCESS) },
            )
            FilterChipItem(
                label = "Cảnh báo",
                isSelected = uiState.filter == HistoryFilter.WARNING,
                onClick = { onFilterSelect(HistoryFilter.WARNING) },
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // History Records List
        if (uiState.records.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Không có bản ghi lịch sử nào.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF64748B)),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.records, key = { it.id }) { record ->
                    HistoryItemCard(record = record, timeZoneManager = timeZoneManager)
                }

                item {
                    val nowText = remember { timeZoneManager.formatDate(System.currentTimeMillis()) }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${uiState.records.size} bản ghi gần nhất · $nowText",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF2563EB) else Color(0xFFF1F5F9),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else Color(0xFF475569),
                fontSize = 13.sp,
            ),
        )
    }
}

@Composable
private fun HistoryItemCard(
    record: SyncHistoryRecord,
    timeZoneManager: TimeZoneManager,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Header Row: Status Badge & Trigger Source
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusTag(status = record.status)
                Text(
                    text = record.triggerSource.name,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp,
                    ),
                )
            }

            // Record Title / Action Description
            val titleText = when (record.status) {
                SyncStatus.NTP_SUCCESS -> "Lấy giờ chuẩn từ máy chủ thành công."
                SyncStatus.NTP_FAILED -> "Lấy giờ máy chủ không thành công."
                SyncStatus.CLOCK_SYNC_SUCCESS -> "Đã cập nhật thời gian thiết bị thành công."
                else -> record.errorMessage ?: "Máy chủ không phản hồi hoặc quá thời gian chờ."
            }
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    fontSize = 15.sp,
                ),
            )

            // Date & Time Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = timeZoneManager.formatDate(record.runAtEpochMillis),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF64748B),
                        fontSize = 13.sp,
                    ),
                )
                Text(
                    text = timeZoneManager.formatTime(record.runAtEpochMillis),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        fontSize = 14.sp,
                    ),
                )
            }

            // Server & Samples Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Server  ${record.ntpServer}:${record.ntpPort}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF64748B),
                        fontSize = 13.sp,
                    ),
                )
                Text(
                    text = "Mẫu  ${record.samplesValid}/${record.samplesAttempted}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        fontSize = 13.sp,
                    ),
                )
            }

            // Badges & Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MetricTag(text = "Offset  ${record.offsetMillis ?: "-"} ms")
                    MetricTag(text = "RTT  ${record.rttMillis ?: "-"} ms")
                }
                Text(
                    text = "Stratum ${record.stratum ?: "-"} · LI ${record.leapIndicator ?: "-"}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                    ),
                )
            }

            // Detailed Epoch & API Information Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF8FAFC),
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Before         ${timeZoneManager.formatDateTime(record.beforeEpochMillis)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF475569),
                            fontSize = 12.sp,
                        ),
                    )
                    record.targetEpochMillis?.let { target ->
                        Text(
                            text = "Target         ${timeZoneManager.formatDateTime(target)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF475569),
                                fontSize = 12.sp,
                            ),
                        )
                    }
                    record.afterEpochMillis?.let { after ->
                        Text(
                            text = "After          ${timeZoneManager.formatDateTime(after)} (${record.postSyncDiffMillis ?: "-"} ms)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF475569),
                                fontSize = 12.sp,
                            ),
                        )
                    }
                    Text(
                        text = "API: ${record.apiSuccess}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                        ),
                    )
                    record.errorMessage?.let { err ->
                        Text(
                            text = "Lỗi: $err",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFDC2626),
                                fontSize = 12.sp,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusTag(status: SyncStatus) {
    val (bgColor, textColor) = when (status) {
        SyncStatus.NTP_SUCCESS -> Pair(Color(0xFFDBEAFE), Color(0xFF1D4ED8))
        SyncStatus.CLOCK_SYNC_SUCCESS -> Pair(Color(0xFFDCFCE7), Color(0xFF15803D))
        else -> Pair(Color(0xFFFEF3C7), Color(0xFFB45309))
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
    ) {
        Text(
            text = status.name,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = textColor,
                fontSize = 11.sp,
            ),
        )
    }
}

@Composable
private fun MetricTag(text: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color(0xFFF1F5F9),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color(0xFF475569),
                fontSize = 11.sp,
            ),
        )
    }
}
