package com.salmanlaghari.risingflix.viewmodel

import com.salmanlaghari.risingflix.data.ContentResponse

sealed interface UiState {
    object Loading : UiState
    data class Success(val data: ContentResponse) : UiState
    data class Error(val message: String) : UiState
}
