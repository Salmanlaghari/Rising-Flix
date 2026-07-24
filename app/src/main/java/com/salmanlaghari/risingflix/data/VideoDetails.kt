package com.salmanlaghari.risingflix.data

import com.google.gson.annotations.SerializedName

data class VideoDetails(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("poster") val poster: String,
    @SerializedName("backdrop") val backdrop: String,
    @SerializedName("description") val description: String,
    @SerializedName("rating") val rating: String,
    @SerializedName("duration") val duration: String,
    @SerializedName("videoUrl") val videoUrl: String,
    @SerializedName("releaseYear") val releaseYear: String,
    @SerializedName("genre") val genre: String,
    @SerializedName("language") val language: String,
    @SerializedName("quality") val quality: String,
    @SerializedName("subtitlesUrl") val subtitlesUrl: String? = null,
    @SerializedName("relatedItems") val relatedItems: List<MovieItem> = emptyList()
)
