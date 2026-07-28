package com.salmanlaghari.risingflix.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salmanlaghari.risingflix.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainViewModel : ViewModel() {

    private val apiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://raw.githubusercontent.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    private val repository by lazy { ContentRepository(apiService) }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedVideoDetails = MutableStateFlow<VideoDetails?>(null)
    val selectedVideoDetails: StateFlow<VideoDetails?> = _selectedVideoDetails.asStateFlow()

    private val _currentNavSection = MutableStateFlow(0) // 0: Home, 1: Explore, 2: Library, 3: Profile
    val currentNavSection: StateFlow<Int> = _currentNavSection.asStateFlow()

    private val _exploreCategoryFilter = MutableStateFlow<String?>(null)
    val exploreCategoryFilter: StateFlow<String?> = _exploreCategoryFilter.asStateFlow()

    init {
        // Delay content fetch to avoid race conditions
        viewModelScope.launch {
            try {
                fetchContent()
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to initialize. Please restart the app.")
            }
        }
    }

    fun fetchContent() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val response = repository.getContentList(forceRefresh = true)
                // Ensure we always have valid data
                if (response.categories.isEmpty() && response.featured == null) {
                    // If no content, show error with helpful message
                    _uiState.value = UiState.Error("No content available. Please check your internet connection.")
                } else {
                    _uiState.value = UiState.Success(response)
                }
            } catch (e: Exception) {
                // Never crash - always show error state
                _uiState.value = UiState.Error(e.localizedMessage ?: "Failed to load content. Please try again.")
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            try {
                if (query.isBlank()) {
                    val response = repository.getContentList()
                    _uiState.value = UiState.Success(response)
                } else {
                    val results = repository.searchMovies(query)
                    // Map results to matching Category representation
                    _uiState.value = UiState.Success(
                        ContentResponse(
                            featured = null,
                            categories = listOf(Category(id = "search_results", name = "Search Results", icon = "search", items = results))
                        )
                    )
                }
            } catch (e: Exception) {
                // Never crash - show error state
                _uiState.value = UiState.Error("Search failed. Please try again.")
            }
        }
    }

    fun selectVideo(video: MovieItem?) {
        viewModelScope.launch {
            if (video == null) {
                _selectedVideoDetails.value = null
            } else {
                _uiState.value = UiState.Loading
                try {
                    val details = repository.getVideoDetails(video.id)
                    _selectedVideoDetails.value = details
                    // Restore success state
                    val response = repository.getContentList()
                    _uiState.value = UiState.Success(response)
                } catch (e: Exception) {
                    // Fallback to prevent crash - create basic details from video info
                    val basicDetails = VideoDetails(
                        id = video.id,
                        title = video.title,
                        poster = video.safePoster,
                        backdrop = video.safeBackdrop,
                        description = video.safeDescription,
                        rating = video.safeRating,
                        duration = video.safeDuration,
                        videoUrl = video.videoUrl,
                        releaseYear = video.safeReleaseYear,
                        genre = video.category,
                        language = "English",
                        quality = video.safeQuality
                    )
                    _selectedVideoDetails.value = basicDetails
                    // Restore success state
                    try {
                        val response = repository.getContentList()
                        _uiState.value = UiState.Success(response)
                    } catch (e2: Exception) {
                        // If we can't get content list, at least show the video details
                        _uiState.value = UiState.Error("Content loaded with limited info.")
                    }
                }
            }
        }
    }

    fun setNavSection(section: Int) {
        _currentNavSection.value = section
    }

    fun setExploreCategory(categoryName: String) {
        _exploreCategoryFilter.value = categoryName
    }
}
