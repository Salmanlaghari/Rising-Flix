package com.salmanlaghari.risingflix.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Content Repository for Rising Flix
 * 
 * Manages content from proxy server (MovieBox content without branding)
 * Backward compatible with existing MainViewModel
 * 
 * IMPORTANT: Default is LEGACY mode (GitHub) for stability
 * Proxy server is optional and only used when explicitly enabled
 */
class ContentRepository(private val apiService: ApiService) {
    
    companion object {
        // ===== CONFIGURATION =====
        // Proxy server URL - only used when proxy is explicitly enabled
        // For Android Emulator: http://10.0.2.2:8080
        // For real device: http://YOUR_SERVER_IP:8080
        // For production: https://your-domain.com
        
        private const val PROXY_SERVER_URL = "http://10.0.2.2:8080"
        
        // Singleton proxy API instance (lazy initialized)
        // Only created when actually needed
        private var _proxyApi: ApiService? = null
        
        private fun getProxyApi(): ApiService {
            if (_proxyApi == null) {
                _proxyApi = Retrofit.Builder()
                    .baseUrl(PROXY_SERVER_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(ApiService::class.java)
            }
            return _proxyApi!!
        }
    }
    
    // ===== IMPORTANT: Default to LEGACY mode for stability =====
    // Proxy server is OFF by default to prevent crashes
    // User must explicitly enable it when proxy server is running
    private var useProxyServer = false
    
    // ===== CONTENT METHODS =====
    
    /**
     * Get all content (movies, series, etc.)
     * Backward compatible with forceRefresh parameter
     * 
     * Priority: Legacy GitHub > Proxy Server > Empty
     */
    suspend fun getContentList(forceRefresh: Boolean = false): ContentResponse {
        // Always try legacy first (most reliable)
        return try {
            apiService.getLegacyContentList()
        } catch (e: Exception) {
            // If legacy fails, try proxy server
            if (useProxyServer) {
                try {
                    getProxyApi().getContentList()
                } catch (e2: Exception) {
                    // Both failed, return empty
                    ContentResponse(
                        featured = null,
                        categories = emptyList()
                    )
                }
            } else {
                // Legacy failed, proxy disabled, return empty
                ContentResponse(
                    featured = null,
                    categories = emptyList()
                )
            }
        }
    }
    
    /**
     * Get video details by ID
     * 
     * Priority: Legacy GitHub > Proxy Server > null
     */
    suspend fun getVideoDetails(id: String): VideoDetails? {
        // Always try legacy first
        return try {
            apiService.getLegacyVideoDetails(id)
        } catch (e: Exception) {
            // If legacy fails, try proxy server
            if (useProxyServer) {
                try {
                    val details = getProxyApi().getVideoDetails(id)
                    
                    // Get the proxied video URL (no MovieBox branding!)
                    val videoUrlResponse = try {
                        getProxyApi().getVideoUrl(id)
                    } catch (e2: Exception) {
                        null
                    }
                    
                    // Update video URL to use proxy
                    if (videoUrlResponse != null) {
                        details.copy(videoUrl = videoUrlResponse.videoUrl)
                    } else {
                        details
                    }
                } catch (e2: Exception) {
                    null
                }
            } else {
                null
            }
        }
    }
    
    /**
     * Search for movies/shows
     * Returns List<MovieItem> for backward compatibility
     * 
     * Priority: Legacy GitHub > Proxy Server > Empty list
     */
    suspend fun searchMovies(query: String): List<MovieItem> {
        // Always try legacy first
        return try {
            val response = apiService.legacySearchMovies(query)
            response.results
        } catch (e: Exception) {
            // If legacy fails, try proxy server
            if (useProxyServer) {
                try {
                    val response = getProxyApi().searchMovies(query)
                    response.results
                } catch (e2: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
    }
    
    /**
     * Get trending movies
     * 
     * Priority: Legacy GitHub > Proxy Server > Empty list
     */
    suspend fun getTrendingMovies(): List<MovieItem> {
        // Always try legacy first
        return try {
            apiService.getLegacyTrendingMovies()
        } catch (e: Exception) {
            // If legacy fails, try proxy server
            if (useProxyServer) {
                try {
                    getProxyApi().getTrendingMovies()
                } catch (e2: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
    }
    
    /**
     * Get video URL for streaming
     * Returns a proxied URL that strips MovieBox branding
     * Returns null if proxy server is not available
     */
    suspend fun getVideoUrl(contentId: String): String? {
        if (!useProxyServer) return null
        
        return try {
            val response = getProxyApi().getVideoUrl(contentId)
            response.videoUrl
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Toggle between proxy server and legacy content
     * Default is OFF (legacy mode)
     */
    fun setUseProxyServer(useProxy: Boolean) {
        useProxyServer = useProxy
    }
    
    /**
     * Check if proxy server is available
     * Returns false if connection fails
     */
    suspend fun isProxyServerAvailable(): Boolean {
        if (!useProxyServer) return false
        
        return try {
            getProxyApi().getContentList()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Enable proxy server with automatic fallback
     * Checks if proxy is available before enabling
     */
    suspend fun enableProxyIfAvailable(): Boolean {
        return try {
            val available = isProxyServerAvailable()
            useProxyServer = available
            available
        } catch (e: Exception) {
            useProxyServer = false
            false
        }
    }
}
