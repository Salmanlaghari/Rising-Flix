package com.salmanlaghari.risingflix.data

import com.google.gson.annotations.SerializedName

data class VideoItem(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("category") val category: String,
    @SerializedName("videoUrl") val videoUrl: String,
    @SerializedName("thumbnailUrl") val thumbnailUrl: String,
    @SerializedName("description") val description: String,
    @SerializedName("rating") val rating: String,
    @SerializedName("quality") val quality: String,
    @SerializedName("year") val year: String,
    @SerializedName("duration") val duration: String
)
