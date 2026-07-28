package com.salmanlaghari.risingflix.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Content Repository for Rising Flix
 * 
 * Manages content from proxy server (MovieBox content without branding)
 */
object ContentRepository {
    
    // ===== CONFIGURATION =====
    // Update this to your proxy server address
    // For Android Emulator: http://10.0.2.2:8080
    // For real device: http://YOUR_SERVER_IP:8080
    // For production: https://your-domain.com
    
    private const val PROXY_SERVER_URL = "http://10.0.2.2:8080"
    
    // Legacy GitHub URL (for backward compatibility)
    private const val LEGACY_BASE_URL = "https://raw.githubusercontent.com/"
    
    // ===== API INSTANCES =====
    
    // Proxy server API (for MovieBox content)
    private val proxyApi: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(PROXY_SERVER_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
    
    // Legacy GitHub API (for original content)
    private val legacyApi: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(LEGACY_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
    
    // Flag to determine which API to use
    private var useProxyServer = true
    
    // ===== CONTENT METHODS =====
    
    /**
     * Get all content (movies, series, etc.)
     */
    suspend fun getContentList(): ContentResponse {
        return try {
            if (useProxyServer) {
                // Try proxy server first
                proxyApi.getContentList()
            } else {
                // Use legacy GitHub content
                legacyApi.getLegacyContentList()
            }
        } catch (e: Exception) {
            // Fallback to legacy if proxy fails
            try {
                legacyApi.getLegacyContentList()
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
                legacyApi.getLegacyVideoDetails(id)
            }
        } catch (e: Exception) {
            // Fallback to legacy
            try {
                legacyApi.getLegacyVideoDetails(id)
            } catch (e2: Exception) {
                null
            }
        }
    }
    
    /**
     * Search for movies/shows
     */
    suspend fun searchMovies(query: String): SearchResponse {
        return try {
            if (useProxyServer) {
                proxyApi.searchMovies(query)
            } else {
                legacyApi.legacySearchMovies(query)
            }
        } catch (e: Exception) {
            try {
                legacyApi.legacySearchMovies(query)
            } catch (e2: Exception) {
                SearchResponse(results = emptyList())
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
                legacyApi.getLegacyTrendingMovies()
            }
        } catch (e: Exception) {
            try {
                legacyApi.getLegacyTrendingMovies()
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
            // Try to make a simple request to the proxy server
            val response = proxyApi.getContentList()
            true
        } catch (e: Exception) {
            false
        }
    }
}
