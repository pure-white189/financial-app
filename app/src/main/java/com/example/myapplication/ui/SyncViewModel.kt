package com.example.myapplication.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.SyncRepository
import com.example.myapplication.data.SyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SyncViewModel(private val syncRepository: SyncRepository) : ViewModel() {

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState

    fun syncNow() {
        if (_syncState.value is SyncState.Syncing) return
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            _syncState.value = when (val result = syncRepository.syncAll()) {
                is SyncResult.Success -> SyncState.Success(
                    message = "已同步：上传 ${result.uploaded} 条，下载 ${result.downloaded} 条"
                )
                is SyncResult.Error -> SyncState.Error(result.message)
            }
        }
    }

    fun resetState() {
        _syncState.value = SyncState.Idle
    }

    class Factory(private val syncRepository: SyncRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return SyncViewModel(syncRepository) as T
        }
    }
}

sealed class SyncState {
    object Idle : SyncState()
    object Syncing : SyncState()
    data class Success(val message: String) : SyncState()
    data class Error(val message: String) : SyncState()
}