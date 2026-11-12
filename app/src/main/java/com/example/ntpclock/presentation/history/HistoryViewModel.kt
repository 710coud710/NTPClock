package com.example.ntpclock.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ntpclock.AppContainer
import com.example.ntpclock.domain.model.SyncHistoryRecord
import com.example.ntpclock.sync.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HistoryFilter {
    ALL, SUCCESS, WARNING
}

data class HistoryUiState(
    val rawRecords: List<SyncHistoryRecord> = emptyList(),
    val filter: HistoryFilter = HistoryFilter.ALL,
) {
    val totalCount: Int get() = rawRecords.size
    val successCount: Int get() = rawRecords.count { isSuccess(it.status) }
    val warningCount: Int get() = rawRecords.count { !isSuccess(it.status) }

    val records: List<SyncHistoryRecord> get() = when (filter) {
        HistoryFilter.ALL -> rawRecords
        HistoryFilter.SUCCESS -> rawRecords.filter { isSuccess(it.status) }
        HistoryFilter.WARNING -> rawRecords.filter { !isSuccess(it.status) }
    }

    private fun isSuccess(status: SyncStatus): Boolean {
        return status == SyncStatus.CLOCK_SYNC_SUCCESS || status == SyncStatus.NTP_SUCCESS
    }
}

class HistoryViewModel(
    private val container: AppContainer,
) : ViewModel() {

    private val _filter = MutableStateFlow(HistoryFilter.ALL)

    val uiState: StateFlow<HistoryUiState> = combine(
        container.syncHistoryRepository.observeHistory(limit = 100),
        _filter
    ) { records, filter ->
        HistoryUiState(rawRecords = records, filter = filter)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = HistoryUiState(),
    )

    fun setFilter(filter: HistoryFilter) {
        _filter.value = filter
    }

    fun clearHistory() {
        viewModelScope.launch {
            container.syncHistoryRepository.clearAll()
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    HistoryViewModel(container) as T
            }
    }
}
