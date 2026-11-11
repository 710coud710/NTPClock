package com.example.ntpclock.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ntpclock.AppContainer
import com.example.ntpclock.domain.model.SyncHistoryRecord
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(
    val records: List<SyncHistoryRecord> = emptyList(),
)

class HistoryViewModel(
    private val container: AppContainer,
) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = container.syncHistoryRepository
        .observeHistory(limit = 100)
        .map { HistoryUiState(records = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = HistoryUiState(),
        )

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

