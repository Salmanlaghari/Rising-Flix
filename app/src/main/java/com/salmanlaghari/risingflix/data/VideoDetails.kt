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
    @SerializedName("actualDurationSeconds") val actualDurationSeconds: Int? = null,
    @SerializedName("contentType") val contentType: String? = null,
    @SerializedName("subtitlesUrl") val subtitlesUrl: String? = null,
    @SerializedName("relatedItems") val relatedItems: List<MovieItem> = emptyList()
) {
    val isShortClip: Boolean get() = contentType == "short_clip" ||
        actualDurationSeconds?.let { it > 0 && it < CatalogResolver.MIN_FEATURE_SECONDS } == true
    val displayTitle: String get() = when {
        isShortClip -> "$title (short clip)"
        contentType == "details_only" -> "$title (details only)"
        else -> title
    }
}
