package com.example.travelmate.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.travelmate.utils.SeedData
import com.example.travelmate.utils.SeedResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class SeedState {
    object Idle : SeedState()
    object Loading : SeedState()
    data class Done(val result: SeedResult) : SeedState()
    data class Error(val message: String) : SeedState()
}

class SeedViewModel : ViewModel() {
    private val _seedState = MutableStateFlow<SeedState>(SeedState.Idle)
    val seedState: StateFlow<SeedState> = _seedState

    fun runSeed() {
        viewModelScope.launch {
            _seedState.value = SeedState.Loading
            val result = SeedData.runAll()
            _seedState.value = when {
                result.success -> SeedState.Done(result)
                result.needsRulesSetup -> SeedState.Error(
                    "PERMISSION_DENIED: Firestore Rules chưa cho phép ghi dữ liệu"
                )
                else -> SeedState.Error(result.error ?: "Lỗi không xác định")
            }
        }
    }

    fun reset() {
        _seedState.value = SeedState.Idle
    }
}
