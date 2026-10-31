package com.salmanlaghari.risingflix.data

import retrofit2.http.GET

/** Both endpoints serve the same strict allowlisted licensed feed. */
interface ApiService {
    @GET("Salmanlaghari/Rising-Flix/feat/catalog-playback-auto-sync/licensed_content.json")
    suspend fun getPullRequestLicensedCatalog(): ContentResponse

    @GET("Salmanlaghari/Rising-Flix/main/licensed_content.json")
    suspend fun getMainLicensedCatalog(): ContentResponse
}
