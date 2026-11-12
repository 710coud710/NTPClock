package com.example.ntpclock.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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
import com.example.ntpclock.presentation.history.HistoryScreen
import com.example.ntpclock.presentation.history.HistoryViewModel
import com.example.ntpclock.presentation.settings.SettingsScreen
import com.example.ntpclock.presentation.settings.SettingsViewModel

enum class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Dashboard("dashboard", "Đồng bộ", Icons.Filled.Sync),
    History("history", "Lịch sử", Icons.Filled.History),
    Settings("settings", "Cài đặt", Icons.Filled.Settings),
}

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
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFFF8FAFC),
                tonalElevation = 0.dp,
            ) {
                AppDestination.entries.forEach { destination ->
                    val selected = currentRoute == destination.route
                    NavigationBarItem(
                        selected = selected,
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
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF2563EB),
                            selectedTextColor = Color(0xFF2563EB),
                            indicatorColor = Color(0xFFEFF6FF),
                            unselectedIconColor = Color(0xFF64748B),
                            unselectedTextColor = Color(0xFF64748B),
                        ),
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
                    onOpenSettingsClick = {
                        navController.navigate(AppDestination.Settings.route) {
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
                    onFilterSelect = vm::setFilter,
                )
            }
            composable(AppDestination.Settings.route) {
                val vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
                val state by vm.uiState.collectAsStateWithLifecycle()
                SettingsScreen(
                    uiState = state,
                    onRefreshPolicyClick = vm::refreshPolicyStatus,
                    onDisableAutoTimeClick = vm::disableAutoTime,
                    onApplyTimezoneClick = vm::applyTargetTimezone,
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
                    onSaveClick = vm::requestSaveConfirmation,
                    onDismissSaveConfirm = vm::dismissSaveConfirmation,
                    onConfirmSave = vm::confirmSaveSettings,
                    onResetDefaultsClick = vm::resetDefaults,
                )
            }
        }
    }
}
