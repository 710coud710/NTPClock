package com.example.ntpclock.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ntpclock.sync.SyncStatus

@Composable
fun StatusRow(
    label: String,
    ok: Boolean,
    okText: String,
    failText: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        StatusPill(text = if (ok) okText else failText, isSuccess = ok)
    }
}

@Composable
fun StatusPill(
    text: String,
    isSuccess: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = if (isSuccess) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.errorContainer
        },
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (isSuccess) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onErrorContainer
            },
        )
    }
}

@Composable
fun SyncStatusBadge(
    status: SyncStatus,
    modifier: Modifier = Modifier,
) {
    val isSuccess = status == SyncStatus.CLOCK_SYNC_SUCCESS || status == SyncStatus.NTP_SUCCESS
    StatusPill(text = status.name, isSuccess = isSuccess, modifier = modifier)
}

