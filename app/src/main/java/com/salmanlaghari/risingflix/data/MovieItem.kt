package com.salmanlaghari.risingflix.data

import com.google.gson.annotations.SerializedName

data class MovieItem(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("thumbnailUrl") val poster: String?, // fallback to poster in json
    @SerializedName("backdrop") val backdrop: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("rating") val rating: String? = null,
    @SerializedName("duration") val duration: String? = null,
    @SerializedName("videoUrl") val videoUrl: String,
    @SerializedName("category") val category: String = "Movies",
    @SerializedName("year") val year: String? = null, // fallback to year in json
    @SerializedName("quality") val quality: String? = null
) {
    val safePoster: String get() = poster ?: ""
    val safeBackdrop: String get() = backdrop ?: poster ?: ""
    val safeReleaseYear: String get() = year ?: "2026"
    val safeQuality: String get() = quality ?: "4K"
    val safeRating: String get() = rating ?: "9.0"
    val safeDuration: String get() = duration ?: "10 min"
    val safeDescription: String get() = description ?: ""
}
