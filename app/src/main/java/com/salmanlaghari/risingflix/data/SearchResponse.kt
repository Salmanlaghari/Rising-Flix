package com.salmanlaghari.risingflix.data

import com.google.gson.annotations.SerializedName

data class SearchResponse(
    @SerializedName("results") val results: List<MovieItem>
)
