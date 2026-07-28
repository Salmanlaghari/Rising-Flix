package com.salmanlaghari.risingflix.data

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("Salmanlaghari/Rising-Flix/main/content.json")
    suspend fun getContentList(): ContentResponse

    @GET("Salmanlaghari/Rising-Flix/main/trending.json")
    suspend fun getTrendingMovies(): List<MovieItem>

    @GET("Salmanlaghari/Rising-Flix/main/popular_dramas.json")
    suspend fun getPopularDramas(): List<MovieItem>

    @GET("Salmanlaghari/Rising-Flix/main/search.json")
    suspend fun searchMovies(@Query("q") query: String): SearchResponse

    @GET("Salmanlaghari/Rising-Flix/main/videos/{id}.json")
    suspend fun getVideoDetails(@Path("id") id: String): VideoDetails
}
