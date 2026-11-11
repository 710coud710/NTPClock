package com.example.ntpclock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ntpclock.presentation.poc.PocScreen
import com.example.ntpclock.presentation.poc.PocViewModel
import com.example.ntpclock.ui.theme.NTPClockTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NTPClockTheme {
                val pocViewModel: PocViewModel = viewModel(
                    factory = PocViewModel.factory(applicationContext)
                )
                val uiState by pocViewModel.uiState.collectAsStateWithLifecycle()

                PocScreen(
                    uiState = uiState,
                    onHostChange = pocViewModel::onNtpHostChanged,
                    onPortChange = pocViewModel::onNtpPortChanged,
                    onRefreshClick = { pocViewModel.refreshAllStatus(logEvent = true) },
                    onDisableAutoTimeClick = pocViewModel::disableAutoTime,
                    onApplyTimezoneClick = pocViewModel::applyVietnamTimezone,
                    onTestNtpClick = pocViewModel::testNtpOnly,
                    onSyncClockClick = pocViewModel::syncAndApplyClock,
                    onClearLogsClick = pocViewModel::clearLogs,
                )
            }
        }
    }
}
