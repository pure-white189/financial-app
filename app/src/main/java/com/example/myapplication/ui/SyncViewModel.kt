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
    var onFirstSyncCompleted: (() -> Unit)? = null

    fun syncNow() {
        if (_syncState.value is SyncState.Syncing) return
        viewModelScope.launch {
            _syncState.value = SyncState.Syncing
            val newState = when (val result = syncRepository.syncAll()) {
                is SyncResult.Success -> SyncState.Success(
                    uploaded = result.uploaded,
                    downloaded = result.downloaded
                )
                is SyncResult.Error -> SyncState.Error(result.message)
            }
            _syncState.value = newState
            if (newState is SyncState.Success) {
                onFirstSyncCompleted?.invoke()
                onFirstSyncCompleted = null   // only fire once
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
    data class Success(val uploaded: Int, val downloaded: Int) : SyncState()
    data class Error(val message: String) : SyncState()
}