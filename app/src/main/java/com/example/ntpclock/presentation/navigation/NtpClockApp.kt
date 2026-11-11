package com.example.ntpclock.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ntpclock.AppContainer
import com.example.ntpclock.presentation.dashboard.DashboardScreen
import com.example.ntpclock.presentation.dashboard.DashboardViewModel
import com.example.ntpclock.presentation.diagnostics.DiagnosticsScreen
import com.example.ntpclock.presentation.diagnostics.DiagnosticsViewModel
import com.example.ntpclock.presentation.history.HistoryScreen
import com.example.ntpclock.presentation.history.HistoryViewModel
import com.example.ntpclock.presentation.settings.SettingsScreen
import com.example.ntpclock.presentation.settings.SettingsViewModel

enum class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Dashboard("dashboard", "Tổng quan", Icons.Filled.Schedule),
    History("history", "Lịch sử", Icons.Filled.History),
    Diagnostics("diagnostics", "Kiểm tra", Icons.Filled.Build),
    Settings("settings", "Cài đặt", Icons.Filled.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NtpClockApp(
    container: AppContainer,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: AppDestination.Dashboard.route

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("NTPClock") },
            )
        },
        bottomBar = {
            NavigationBar {
                AppDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Dashboard.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(AppDestination.Dashboard.route) {
                val vm: DashboardViewModel = viewModel(factory = DashboardViewModel.factory(container))
                val state by vm.uiState.collectAsStateWithLifecycle()
                DashboardScreen(
                    uiState = state,
                    onSyncNowClick = vm::syncNow,
                    onTestNtpClick = vm::testNtpReadOnly,
                    onRefreshClick = vm::refreshPolicyStatus,
                    onOpenDiagnosticsClick = {
                        navController.navigate(AppDestination.Diagnostics.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(AppDestination.History.route) {
                val vm: HistoryViewModel = viewModel(factory = HistoryViewModel.factory(container))
                val state by vm.uiState.collectAsStateWithLifecycle()
                HistoryScreen(
                    uiState = state,
                    onClearHistoryClick = vm::clearHistory,
                )
            }
            composable(AppDestination.Diagnostics.route) {
                val vm: DiagnosticsViewModel = viewModel(factory = DiagnosticsViewModel.factory(container))
                val state by vm.uiState.collectAsStateWithLifecycle()
                DiagnosticsScreen(
                    uiState = state,
                    onRefreshClick = { vm.refreshStatus(logEvent = true) },
                    onDisableAutoTimeClick = vm::disableAutoTime,
                    onApplyTimezoneClick = vm::applyTargetTimezone,
                    onEnqueueWorkerClick = vm::enqueueImmediateWorkerSync,
                    onClearLogsClick = vm::clearLogs,
                )
            }
            composable(AppDestination.Settings.route) {
                val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
                val state by vm.uiState.collectAsStateWithLifecycle()
                SettingsScreen(
                    uiState = state,
                    onToggleAdminUnlock = vm::toggleAdminUnlock,
                    onHostChange = vm::onHostChange,
                    onPortChange = vm::onPortChange,
                    onTimeZoneChange = vm::onTimeZoneChange,
                    onSyncIntervalChange = vm::onSyncIntervalChange,
                    onSampleCountChange = vm::onSampleCountChange,
                    onTimeoutChange = vm::onTimeoutChange,
                    onMaxRttChange = vm::onMaxRttChange,
                    onMaxOffsetChange = vm::onMaxOffsetChange,
                    onAutoSyncToggle = vm::onAutoSyncToggle,
                    onSyncOnBootToggle = vm::onSyncOnBootToggle,
                    onSaveClick = vm::saveSettings,
                    onResetDefaultsClick = vm::resetDefaults,
                )
            }
        }
    }
}

