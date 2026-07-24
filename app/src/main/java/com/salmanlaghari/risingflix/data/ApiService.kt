package com.salmanlaghari.risingflix.data

import retrofit2.http.GET

interface ApiService {
    @GET("Salmanlaghari/Rising-Flix/main/content.json")
    suspend fun getContentList(): ContentResponse
}
