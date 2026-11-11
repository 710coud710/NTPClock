package com.example.ntpclock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ntpclock.presentation.navigation.NtpClockApp
import com.example.ntpclock.ui.theme.NTPClockTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as NtpClockApplication).container
        setContent {
            NTPClockTheme {
                NtpClockApp(container = container)
            }
        }
    }
}
