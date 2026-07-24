package com.salmanlaghari.risingflix.data

import com.google.gson.annotations.SerializedName

data class ContentResponse(
    @SerializedName("featured") val featured: VideoItem?,
    @SerializedName("categories") val categories: List<Category>
)
