package com.salmanlaghari.risingflix.data

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Rising Flix API Service
 * 
 * Updated to use proxy server for MovieBox content
 * All content is served through our proxy - no MovieBox branding!
 */
interface ApiService {
    
    // ===== PROXY SERVER ENDPOINTS =====
    // Update this URL to your proxy server address
    // For local testing: http://10.0.2.2:8080 (Android Emulator)
    // For production: https://your-server.com
    
    @GET("api/content")
    suspend fun getContentList(): ContentResponse
    
    @GET("api/content/{id}")
    suspend fun getVideoDetails(@Path("id") id: String): VideoDetails
    
    @GET("api/search")
    suspend fun searchMovies(@Query("q") query: String): SearchResponse
    
    @GET("api/trending")
    suspend fun getTrendingMovies(): List<MovieItem>
    
    @GET("api/video/{id}")
    suspend fun getVideoUrl(@Path("id") id: String): VideoUrlResponse
    
    // ===== LEGACY ENDPOINTS (for backward compatibility) =====
    // These can be removed once migration is complete
    
    @GET("Salmanlaghari/Rising-Flix/main/content.json")
    suspend fun getLegacyContentList(): ContentResponse
    
    @GET("Salmanlaghari/Rising-Flix/main/trending.json")
    suspend fun getLegacyTrendingMovies(): List<MovieItem>
    
    @GET("Salmanlaghari/Rising-Flix/main/popular_dramas.json")
    suspend fun getLegacyPopularDramas(): List<MovieItem>
    
    @GET("Salmanlaghari/Rising-Flix/main/search.json")
    suspend fun legacySearchMovies(@Query("q") query: String): SearchResponse
    
    @GET("Salmanlaghari/Rising-Flix/main/videos/{id}.json")
    suspend fun getLegacyVideoDetails(@Path("id") id: String): VideoDetails
}

/**
 * Response for video URL endpoint
 */
data class VideoUrlResponse(
    val contentId: String,
    val title: String,
    val videoUrl: String,
    val originalUrl: String?,
    val quality: String,
    val poster: String?
)
