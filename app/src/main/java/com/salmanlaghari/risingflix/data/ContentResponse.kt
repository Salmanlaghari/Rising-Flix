package com.salmanlaghari.risingflix.data

import com.google.gson.annotations.SerializedName

data class ContentResponse(
    @SerializedName("featured") val featured: MovieItem?,
    @SerializedName("categories") val categories: List<Category>
)
