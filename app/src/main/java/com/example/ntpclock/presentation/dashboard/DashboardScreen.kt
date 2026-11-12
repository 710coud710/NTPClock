package com.example.ntpclock.presentation.dashboard

import android.hardware.lights.Light
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ntpclock.core.time.TimeZoneManager
import com.example.ntpclock.presentation.components.NtpClockHeader

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onSyncNowClick: () -> Unit,
    onTestNtpClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeZoneManager = remember { TimeZoneManager() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, top = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (uiState.isBusy) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF2563EB),
            )
        }

        // Shared Reusable Header
        NtpClockHeader()

        // Section Title & Readiness Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Đồng bộ thời gian",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        fontSize = 24.sp,
                    ),
                )
            }
            IconButton(
                onClick = onRefreshClick,
                enabled = !uiState.isBusy,
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Làm mới",
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        // THẺ 1: GIỜ HỆ THỐNG · NTP (Dark Navy Blue Card)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2042)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(
                modifier = Modifier.padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = "GIỜ HỆ THỐNG · NTP",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp,
                                letterSpacing = 0.8.sp,
                            ),
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1E293B),
                    ) {
                        Text(
                            text = if (uiState.currentTimezoneId.contains("Ho_Chi_Minh") || uiState.currentTimezoneId.contains("Saigon")) "UTC+07:00" else uiState.currentTimezoneId,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFE2E8F0),
                                fontSize = 11.sp,
                            ),
                        )
                    }
                }

                val ntpTime = uiState.currentNtpTimeText ?: uiState.currentSystemTimeText
                val ntpMillis = uiState.currentNtpMillisText ?: uiState.currentMillisText
                Text(
                    text = "$ntpTime$ntpMillis",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 32.sp,
                    ),
                )

                Text(
                    text = uiState.currentDateText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFFCBD5E1),
                        fontSize = 14.sp,
                    ),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${uiState.settings.host}:${uiState.settings.port}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFCBD5E1),
                            fontSize = 13.sp,
                        ),
                    )
                    val rtt = uiState.lastSyncResult?.sample?.roundTripDelayMillis
                        ?: uiState.latestSuccessfulSync?.rttMillis ?: 3L
                    Text(
                        text = "RTT  $rtt ms",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFCBD5E1),
                            fontSize = 13.sp,
                        ),
                    )
                }
            }
        }

        // Arrow connector
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.ArrowDownward,
                contentDescription = null,
                tint = Color(0xFF3B82F6),
                modifier = Modifier.size(24.dp),
            )
        }

        // THẺ 2: GIỜ THIẾT BỊ (Light Blue Card)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF619DC6)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Smartphone,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "GIỜ THIẾT BỊ",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3B82F6),
                            fontSize = 12.sp,
                            letterSpacing = 0.8.sp,
                        ),
                    )
                }
                Text(
                    text = "${uiState.currentSystemTimeText}${uiState.currentMillisText}",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D4ED8),
                        fontSize = 32.sp,
                    ),
                )
            }
        }

        // Action Buttons Row: Kiểm tra & Đồng bộ ngay
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onTestNtpClick,
                enabled = !uiState.isBusy,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF2563EB),
                    containerColor = Color.White,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Kiểm tra",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    ),
                )
            }

            Button(
                onClick = onSyncNowClick,
                enabled = !uiState.isBusy,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2563EB),
                    contentColor = Color.White,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Đồng bộ ngay",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    ),
                )
            }
        }

        // Warning card if Device Owner or Auto Time requires admin attention
        if (!uiState.isDeviceOwner || uiState.isAutoTimeEnabled) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
            ) {
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
                            text = "CẦN CẤU HÌNH QUẢN TRỊ",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626),
                            ),
                        )
                        OutlinedButton(
                            onClick = onOpenSettingsClick,
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text("Cài đặt", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    if (!uiState.isDeviceOwner) {
                        Text(
                            text = "• Chưa kích hoạt Device Owner. Vào Cài đặt để xem hướng dẫn.",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF991B1B)),
                        )
                    }
                    if (uiState.isAutoTimeEnabled) {
                        Text(
                            text = "• Automatic Date & Time đang bật (cần tắt để cập nhật giờ).",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF991B1B)),
                        )
                    }
                }
            }
        }

        // THẺ 3: Trạng thái thiết bị
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Trạng thái thiết bị",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            fontSize = 18.sp,
                        ),
                    )
                    Text(
                        text = "4 dịch vụ",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF94A3B8),
                            fontSize = 13.sp,
                        ),
                    )
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Service Item 1: Kết nối LAN / Wi-Fi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Wifi,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(20.dp),
                        )
                        Column {
                            Text(
                                text = "Kết nối LAN / Wi-Fi",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A),
                                ),
                            )
                            Text(
                                text = if (uiState.isLanConnected) "Wi-Fi · NTP-Office" else "Mất kết nối",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF64748B),
                                    fontSize = 12.sp,
                                ),
                            )
                        }
                    }
                    StatusBadge(
                        text = if (uiState.isLanConnected) "Đã kết nối" else "Mất kết nối",
                        isSuccess = uiState.isLanConnected,
                    )
                }

                // Service Item 2: Device owner time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(20.dp),
                        )
                        Column {
                            Text(
                                text = "Device owner time",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A),
                                ),
                            )
                            Text(
                                text = "Quyền cập nhật giờ thiết bị",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF64748B),
                                    fontSize = 12.sp,
                                ),
                            )
                        }
                    }
                    StatusBadge(
                        text = if (uiState.isDeviceOwner) "Active" else "Inactive",
                        isSuccess = uiState.isDeviceOwner,
                    )
                }

                // Service Item 3: Cập nhật định kỳ
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(20.dp),
                        )
                        Column {
                            Text(
                                text = "Cập nhật mỗi ${uiState.settings.syncIntervalMinutes} phút",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A),
                                ),
                            )
                            Text(
                                text = "Tự động chạy ở background",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF64748B),
                                    fontSize = 12.sp,
                                ),
                            )
                        }
                    }
                    StatusBadge(
                        text = if (uiState.settings.autoSyncEnabled) "On" else "Off",
                        isSuccess = uiState.settings.autoSyncEnabled,
                    )
                }

                // Service Item 4: Automatic time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Smartphone,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(20.dp),
                        )
                        Column {
                            Text(
                                text = "Automatic time",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A),
                                ),
                            )
                            Text(
                                text = "Thời gian tự động của Android",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFF64748B),
                                    fontSize = 12.sp,
                                ),
                            )
                        }
                    }
                    StatusBadge(
                        text = if (!uiState.isAutoTimeEnabled) "Off" else "On",
                        isSuccess = !uiState.isAutoTimeEnabled,
                    )
                }
            }
        }

        // FOOTER: Sync result summary
        val lastTimeText = when {
            uiState.lastSyncResult != null -> timeZoneManager.formatTime(uiState.lastSyncResult.executedAtEpochMillis)
            uiState.latestSuccessfulSync != null -> timeZoneManager.formatTime(uiState.latestSuccessfulSync.runAtEpochMillis)
            else -> null
        }

        if (lastTimeText != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "Đồng bộ gần nhất $lastTimeText · Thành công",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF475569),
                        fontSize = 14.sp,
                    ),
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(
    text: String,
    isSuccess: Boolean,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSuccess) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = if (isSuccess) Color(0xFF15803D) else Color(0xFF64748B),
                fontSize = 12.sp,
            ),
        )
    }
}
