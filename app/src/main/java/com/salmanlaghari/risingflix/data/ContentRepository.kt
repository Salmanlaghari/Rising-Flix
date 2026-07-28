package com.salmanlaghari.risingflix.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Content Repository for Rising Flix
 * 
 * Manages content from proxy server (MovieBox content without branding)
 * Backward compatible with existing MainViewModel
 */
class ContentRepository(private val apiService: ApiService) {
    
    companion object {
        // ===== CONFIGURATION =====
        // Update this to your proxy server address
        // For Android Emulator: http://10.0.2.2:8080
        // For real device: http://YOUR_SERVER_IP:8080
        // For production: https://your-domain.com
        
        private const val PROXY_SERVER_URL = "http://10.0.2.2:8080"
        
        // Legacy GitHub URL (for backward compatibility)
        private const val LEGACY_BASE_URL = "https://raw.githubusercontent.com/"
        
        // Singleton proxy API instance
        private val proxyApi: ApiService by lazy {
            Retrofit.Builder()
                .baseUrl(PROXY_SERVER_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
    
    // Flag to determine which API to use
    private var useProxyServer = true
    
    // ===== CONTENT METHODS =====
    
    /**
     * Get all content (movies, series, etc.)
     * Backward compatible with forceRefresh parameter
     */
    suspend fun getContentList(forceRefresh: Boolean = false): ContentResponse {
        return try {
            if (useProxyServer) {
                // Try proxy server first
                proxyApi.getContentList()
            } else {
                // Use legacy GitHub content
                apiService.getLegacyContentList()
            }
        } catch (e: Exception) {
            // Fallback to legacy if proxy fails
            try {
                apiService.getLegacyContentList()
            } catch (e2: Exception) {
                // Return empty response if both fail
                ContentResponse(
                    featured = null,
                    categories = emptyList()
                )
            }
        }
    }
    
    /**
     * Get video details by ID
     */
    suspend fun getVideoDetails(id: String): VideoDetails? {
        return try {
            if (useProxyServer) {
                // Get from proxy server
                val details = proxyApi.getVideoDetails(id)
                
                // Get the proxied video URL (no MovieBox branding!)
                val videoUrlResponse = try {
                    proxyApi.getVideoUrl(id)
                } catch (e: Exception) {
                    null
                }
                
                // Update video URL to use proxy
                if (videoUrlResponse != null) {
                    details.copy(videoUrl = videoUrlResponse.videoUrl)
                } else {
                    details
                }
            } else {
                apiService.getLegacyVideoDetails(id)
            }
        } catch (e: Exception) {
            // Fallback to legacy
            try {
                apiService.getLegacyVideoDetails(id)
            } catch (e2: Exception) {
                null
            }
        }
    }
    
    /**
     * Search for movies/shows
     * Returns List<MovieItem> for backward compatibility
     */
    suspend fun searchMovies(query: String): List<MovieItem> {
        return try {
            if (useProxyServer) {
                val response = proxyApi.searchMovies(query)
                response.results
            } else {
                val response = apiService.legacySearchMovies(query)
                response.results
            }
        } catch (e: Exception) {
            try {
                val response = apiService.legacySearchMovies(query)
                response.results
            } catch (e2: Exception) {
                emptyList()
            }
        }
    }
    
    /**
     * Get trending movies
     */
    suspend fun getTrendingMovies(): List<MovieItem> {
        return try {
            if (useProxyServer) {
                proxyApi.getTrendingMovies()
            } else {
                apiService.getLegacyTrendingMovies()
            }
        } catch (e: Exception) {
            try {
                apiService.getLegacyTrendingMovies()
            } catch (e2: Exception) {
                emptyList()
            }
        }
    }
    
    /**
     * Get video URL for streaming
     * Returns a proxied URL that strips MovieBox branding
     */
    suspend fun getVideoUrl(contentId: String): String? {
        return try {
            if (useProxyServer) {
                val response = proxyApi.getVideoUrl(contentId)
                response.videoUrl
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Toggle between proxy server and legacy content
     */
    fun setUseProxyServer(useProxy: Boolean) {
        useProxyServer = useProxy
    }
    
    /**
     * Check if proxy server is available
     */
    suspend fun isProxyServerAvailable(): Boolean {
        return try {
            proxyApi.getContentList()
            true
        } catch (e: Exception) {
            false
        }
    }
}
