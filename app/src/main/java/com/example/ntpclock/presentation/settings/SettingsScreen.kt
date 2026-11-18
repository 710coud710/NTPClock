package com.example.ntpclock.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import com.example.ntpclock.core.common.VendorOptimizationHelper
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ntpclock.presentation.components.NtpClockHeader
import java.time.Instant
import java.time.ZoneId

private data class SelectOption(
    val value: String,
    val label: String,
)

private val SYNC_INTERVAL_OPTIONS = listOf(
    SelectOption("15", "15 phút"),
    SelectOption("30", "30 phút"),
    SelectOption("60", "60 phút"),
    SelectOption("120", "120 phút"),
    SelectOption("180", "180 phút"),
    SelectOption("720", "Nửa ngày"),
    SelectOption("1440", "1 ngày"),
)

private val SAMPLE_COUNT_OPTIONS = (1..10).map { count ->
    SelectOption(count.toString(), "$count mẫu")
}

private val TIMEOUT_MS_OPTIONS = listOf(1000, 1500, 2000, 2500, 3000, 5000, 8000, 10000).map { ms ->
    SelectOption(ms.toString(), "$ms ms")
}

private val MAX_RTT_MS_OPTIONS = listOf(100, 200, 500, 1000, 1500, 2000, 3000, 5000, 8000, 10000).map { ms ->
    SelectOption(ms.toString(), "$ms ms")
}

private val PRIORITY_TIME_ZONES = listOf(
    "Asia/Ho_Chi_Minh",
    "Asia/Bangkok",
    "Asia/Singapore",
    "Asia/Tokyo",
    "Asia/Seoul",
    "Asia/Shanghai",
    "Asia/Taipei",
    "Asia/Jakarta",
    "Europe/London",
    "America/New_York",
    "America/Los_Angeles",
)

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onRefreshPolicyClick: () -> Unit,
    onDisableAutoTimeClick: () -> Unit,
    onApplyTimezoneClick: () -> Unit,
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
    onDismissSaveConfirm: () -> Unit,
    onConfirmSave: () -> Unit,
    onResetDefaultsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isIgnoringBattery = remember(uiState) {
        VendorOptimizationHelper.isIgnoringBatteryOptimizations(context)
    }
    val vendorBrand = remember { VendorOptimizationHelper.getVendorBrandName() }

    if (uiState.showSaveConfirmDialog) {
        AlertDialog(
            onDismissRequest = onDismissSaveConfirm,
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White,
            title = {
                Text(
                    text = "Xác nhận lưu cấu hình",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                    ),
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Bạn có chắc muốn áp dụng các thay đổi cấu hình không?")
                    Text("• Máy chủ NTP: ${uiState.host.trim()}:${uiState.port}")
                    Text("• Múi giờ: ${uiState.timeZoneId.trim()}")
                    val intervalLabel = resolveOptionLabel(
                        value = uiState.syncIntervalMinutes,
                        options = SYNC_INTERVAL_OPTIONS,
                        fallbackSuffix = " phút",
                    )
                    Text("• Chu kỳ: $intervalLabel")
                    Text("• Số lượng mẫu: ${uiState.sampleCount} mẫu")
                    Text("• Timeout: ${uiState.timeoutMs} ms")
                    Text("• Max RTT: ${uiState.maxAcceptableRttMs} ms")
                    Text("• Tự đồng bộ định kỳ: ${if (uiState.autoSyncEnabled) "Bật" else "Tắt"}")
                    Text("• Đồng bộ khi khởi động: ${if (uiState.syncOnBootEnabled) "Bật" else "Tắt"}")
                }
            },
            confirmButton = {
                Button(
                    onClick = onConfirmSave,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                ) {
                    Text("Xác nhận lưu")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissSaveConfirm) {
                    Text("Hủy", color = Color(0xFF64748B))
                }
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Shared Reusable Header
        NtpClockHeader()

        // Section Title & Subtitle & Reset Defaults Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Cài đặt",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        fontSize = 26.sp,
                    ),
                )
            }
        }

        // CARD 1: Thiết lập thiết bị
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
                        text = "Thiết lập thiết bị",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            fontSize = 18.sp,
                        ),
                    )
                    TextButton(onClick = onRefreshPolicyClick) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Làm mới",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB),
                            ),
                        )
                    }
                }

                SettingStatusRow(
                    label = "Device Owner",
                    valueText = if (uiState.isDeviceOwner) "● Đã kích hoạt" else "Chưa provision",
                    isSuccess = uiState.isDeviceOwner,
                )
                SettingStatusRow(
                    label = "Automatic Date & Time",
                    valueText = if (!uiState.isAutoTimeEnabled) "OFF (0)" else "ON (1)",
                    isSuccess = !uiState.isAutoTimeEnabled,
                )
                SettingStatusRow(
                    label = "Automatic Time Zone",
                    valueText = if (!uiState.isAutoTimeZoneEnabled) "OFF (0)" else "ON (1)",
                    isSuccess = !uiState.isAutoTimeZoneEnabled,
                )

                // Current Timezone Display Box
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEFF6FF),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Language,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(20.dp),
                        )
                        Column {
                            Text(
                                text = "Múi giờ hiện tại",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF64748B),
                                    fontSize = 11.sp,
                                ),
                            )
                            Text(
                                text = uiState.currentTimezoneId,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D4ED8),
                                    fontSize = 15.sp,
                                ),
                            )
                        }
                    }
                }

                // Action Buttons: Tắt Auto Time & Đặt Timezone
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onDisableAutoTimeClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB)),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DoNotDisturbOn,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tắt Auto Time",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            ),
                        )
                    }

                    OutlinedButton(
                        onClick = onApplyTimezoneClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB)),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Public,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Đặt Timezone",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            ),
                        )
                    }
                }

                uiState.policyStatusMessage?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF2563EB)),
                    )
                }

                if (!uiState.isDeviceOwner && uiState.receiverComponentName.isNotBlank()) {
                    SelectionContainer {
                        Text(
                            text = "ADB: adb shell dpm set-device-owner ${uiState.receiverComponentName}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF64748B),
                                fontSize = 11.sp,
                            ),
                        )
                    }
                }
            }
        }

        // CARD 2: Cấu hình máy chủ (Index 01)
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
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically,
//                ) {
//
//                    Text(
//                        text = "01",
//                        style = MaterialTheme.typography.labelMedium.copy(
//                            fontWeight = FontWeight.Bold,
//                            color = Color(0xFFCBD5E1),
//                            fontSize = 14.sp,
//                        ),
//                    )
//                }
                Text(
                    text = "Cấu hình máy chủ",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        fontSize = 18.sp,
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CustomInputField(
                        value = uiState.host,
                        onValueChange = onHostChange,
                        label = "NTP host",
                        modifier = Modifier.weight(2f),
                    )
                    CustomInputField(
                        value = uiState.port,
                        onValueChange = onPortChange,
                        label = "Port",
                        keyboardType = KeyboardType.Number,
                        modifier = Modifier.weight(1f),
                    )
                }

                TimeZoneSelectorField(
                    selectedZoneId = uiState.timeZoneId,
                    onZoneSelected = onTimeZoneChange,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // CARD 3: Thông số đồng bộ (Index 02)
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
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically,
//                ) {
//
//                    Text(
//                        text = "02",
//                        style = MaterialTheme.typography.labelMedium.copy(
//                            fontWeight = FontWeight.Bold,
//                            color = Color(0xFFCBD5E1),
//                            fontSize = 14.sp,
//                        ),
//                    )
//                }
                Text(
                    text = "Thông số đồng bộ",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        fontSize = 18.sp,
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PresetDropdownField(
                        label = "Chu kỳ đồng bộ",
                        selectedValue = uiState.syncIntervalMinutes,
                        options = SYNC_INTERVAL_OPTIONS,
                        fallbackSuffix = " phút",
                        onValueSelected = onSyncIntervalChange,
                        modifier = Modifier.weight(1f),
                    )
                    PresetDropdownField(
                        label = "Số lượng mẫu lấy",
                        selectedValue = uiState.sampleCount,
                        options = SAMPLE_COUNT_OPTIONS,
                        fallbackSuffix = " mẫu",
                        onValueSelected = onSampleCountChange,
                        modifier = Modifier.weight(1f),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PresetDropdownField(
                        label = "Timeout",
                        selectedValue = uiState.timeoutMs,
                        options = TIMEOUT_MS_OPTIONS,
                        fallbackSuffix = " ms",
                        onValueSelected = onTimeoutChange,
                        modifier = Modifier.weight(1f),
                    )
                    PresetDropdownField(
                        label = "Max RTT",
                        selectedValue = uiState.maxAcceptableRttMs,
                        options = MAX_RTT_MS_OPTIONS,
                        fallbackSuffix = " ms",
                        onValueSelected = onMaxRttChange,
                        modifier = Modifier.weight(1f),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    CustomInputField(
                        value = uiState.maxOffsetBeforeSetMs,
                        onValueChange = onMaxOffsetChange,
                        label = "Max offset",
                        keyboardType = KeyboardType.Number,
                        trailingSuffix = "ms",
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = "Ngưỡng lệch cho phép(0 = không giới hạn).",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF64748B),
                            fontSize = 12.sp,
                        ),
                    )
                }
            }
        }

        // CARD 4: Tự động hóa (Index 03)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically,
//                ) {
//
//                    Text(
//                        text = "03",
//                        style = MaterialTheme.typography.labelMedium.copy(
//                            fontWeight = FontWeight.Bold,
//                            color = Color(0xFFCBD5E1),
//                            fontSize = 14.sp,
//                        ),
//                    )
//                }
                Text(
                    text = "Tự động cập nhật",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        fontSize = 18.sp,
                    ),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Định kỳ",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                fontSize = 15.sp,
                            ),
                        )
                        Text(
                            text = "Tự cập nhật theo chu kỳ mỗi ${uiState.syncIntervalMinutes} phút",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF64748B),
                                fontSize = 13.sp,
                            ),
                        )
                    }
                    Switch(
                        checked = uiState.autoSyncEnabled,
                        onCheckedChange = onAutoSyncToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF2563EB),
                        ),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Khi khởi động",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                fontSize = 15.sp,
                            ),
                        )
                        Text(
                            text = "Cập nhật giờ sau khi bật thiết bị",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF64748B),
                                fontSize = 13.sp,
                            ),
                        )
                    }
                    Switch(
                        checked = uiState.syncOnBootEnabled,
                        onCheckedChange = onSyncOnBootToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF2563EB),
                        ),
                    )
                }
            }
        }

        // CARD 5: Tối ưu hóa ngầm cho hãng máy (Samsung, Xiaomi/Redmi, Lenovo, Asus...)
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
                Text(
                    text = "Tối ưu ngầm ($vendorBrand)",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        fontSize = 18.sp,
                    ),
                )

                Text(
                    text = if (vendorBrand.contains("Xiaomi") || vendorBrand.contains("Redmi")) {
                        "Lưu ý cho Redmi Pad / Xiaomi: Bấm 'Tắt tối ưu pin' -> chọn 'Không hạn chế'. (Trên Redmi Pad, hệ điều hành Xiaomi loại bỏ menu Tự khởi chạy nên chỉ cần tắt tối ưu pin là đã hoạt động ngầm 100%)."
                    } else {
                        "Đảm bảo ứng dụng tự động cập nhật giờ chính xác ngay cả khi tắt app hoặc khoá màn hình trên các dòng máy Samsung, Redmi, Lenovo, Asus..."
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF64748B),
                        fontSize = 13.sp,
                    ),
                )

                SettingStatusRow(
                    label = "Tắt tối ưu hóa pin",
                    valueText = if (isIgnoringBattery) "● Đã tắt (Khuyên dùng)" else "Bật (Có thể bị kill)",
                    isSuccess = isIgnoringBattery,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = { VendorOptimizationHelper.requestIgnoreBatteryOptimizations(context) },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB)),
                    ) {
                        Text(
                            text = "Tắt tối ưu pin",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                            ),
                        )
                    }

                    OutlinedButton(
                        onClick = { VendorOptimizationHelper.openVendorAutostartSettings(context) },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB)),
                    ) {
                        Text(
                            text = "Cài đặt Autostart",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                            ),
                        )
                    }
                }
            }
        }

        uiState.statusMessage?.let { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2563EB),
                ),
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }

        // Primary Reset Button
        Button(
            onClick = onResetDefaultsClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF64748B),
            ),
        ) {
            Icon(
                imageVector = Icons.Default.SettingsBackupRestore,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Khôi phục mặc định",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                ),
            )
        }
        // Primary Save Button
        Button(
            onClick = onSaveClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2563EB),
                contentColor = Color.White,
            ),
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Lưu cấu hình",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                ),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SettingStatusRow(
    label: String,
    valueText: String,
    isSuccess: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Medium,
                color = Color(0xFF334155),
            ),
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isSuccess) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
        ) {
            Text(
                text = valueText,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isSuccess) Color(0xFF15803D) else Color(0xFF64748B),
                    fontSize = 12.sp,
                ),
            )
        }
    }
}

@Composable
private fun CustomInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    trailingSuffix: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        trailingIcon = trailingSuffix?.let { suffix ->
            {
                Text(
                    text = suffix,
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF94A3B8)),
                    modifier = Modifier.padding(end = 12.dp),
                )
            }
        },
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFF8FAFC),
            unfocusedContainerColor = Color(0xFFF8FAFC),
            focusedBorderColor = Color(0xFF2563EB),
            unfocusedBorderColor = Color(0xFFE2E8F0),
        ),
        modifier = modifier,
    )
}

private fun resolveOptionLabel(
    value: String,
    options: List<SelectOption>,
    fallbackSuffix: String,
): String {
    val trimmed = value.trim()
    val matched = options.firstOrNull { it.value == trimmed }
    return matched?.label ?: if (trimmed.isBlank()) "" else "$trimmed$fallbackSuffix"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PresetDropdownField(
    label: String,
    selectedValue: String,
    options: List<SelectOption>,
    fallbackSuffix: String,
    onValueSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val displayText = resolveOptionLabel(selectedValue, options, fallbackSuffix)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF8FAFC),
                unfocusedContainerColor = Color(0xFFF8FAFC),
                focusedBorderColor = Color(0xFF2563EB),
                unfocusedBorderColor = Color(0xFFE2E8F0),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    trailingIcon = if (option.value == selectedValue.trim()) {
                        { Icon(imageVector = Icons.Default.Check, contentDescription = null) }
                    } else {
                        null
                    },
                    onClick = {
                        onValueSelected(option.value)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun TimeZoneSelectorField(
    selectedZoneId: String,
    onZoneSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }
    val allZones = remember { buildTimeZoneOptions() }
    val displayText = allZones.firstOrNull { it.value == selectedZoneId.trim() }?.label
        ?: if (selectedZoneId.contains("Ho_Chi_Minh") || selectedZoneId.contains("Saigon")) "Asia/Ho_Chi_Minh (UTC+07:00)" else selectedZoneId

    Box(modifier = modifier) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text("Múi giờ · Timezone") },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Chọn Timezone",
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF8FAFC),
                unfocusedContainerColor = Color(0xFFF8FAFC),
                focusedBorderColor = Color(0xFF2563EB),
                unfocusedBorderColor = Color(0xFFE2E8F0),
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showDialog = true },
        )
    }

    if (showDialog) {
        TimeZonePickerDialog(
            selectedZoneId = selectedZoneId.trim(),
            allZones = allZones,
            onSelect = { zoneId ->
                onZoneSelected(zoneId)
                showDialog = false
            },
            onDismiss = { showDialog = false },
        )
    }
}

@Composable
private fun TimeZonePickerDialog(
    selectedZoneId: String,
    allZones: List<SelectOption>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filteredZones = remember(query, allZones) {
        val needle = query.trim()
        if (needle.isEmpty()) {
            allZones
        } else {
            allZones.filter {
                it.label.contains(needle, ignoreCase = true) ||
                    it.value.contains(needle, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        title = {
            Text(
                text = "Chọn Timezone",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Tìm kiếm") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                ) {
                    items(items = filteredZones, key = { it.value }) { option ->
                        val isSelected = option.value == selectedZoneId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(option.value) }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) {
                                    Color(0xFF2563EB)
                                } else {
                                    Color(0xFF0F172A)
                                },
                                modifier = Modifier.weight(1f),
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                )
                            }
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng", color = Color(0xFF2563EB))
            }
        },
    )
}

private fun buildTimeZoneOptions(): List<SelectOption> {
    val now = Instant.now()
    val available = ZoneId.getAvailableZoneIds()
    val priority = PRIORITY_TIME_ZONES.filter { it in available }
    val remaining = available.filterNot { it in priority }.sorted()
    return (priority + remaining).map { id ->
        val offsetId = runCatching { ZoneId.of(id).rules.getOffset(now).id }
            .getOrDefault("")
            .let { if (it == "Z") "+00:00" else it }
        val label = if (offsetId.isNotEmpty()) "$id (UTC$offsetId)" else id
        SelectOption(value = id, label = label)
    }
}
