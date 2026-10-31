package com.salmanlaghari.risingflix.data

import com.google.gson.annotations.SerializedName

data class MovieItem(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName(value = "thumbnailUrl", alternate = ["poster", "coverUrl"]) val poster: String?,
    @SerializedName(value = "backdrop", alternate = ["backdropUrl"]) val backdrop: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("rating") val rating: String? = null,
    @SerializedName("duration") val duration: String? = null,
    @SerializedName("videoUrl") val videoUrl: String,
    @SerializedName("category") val category: String = "Movies",
    @SerializedName(value = "year", alternate = ["releaseYear"]) val year: String? = null,
    @SerializedName("quality") val quality: String? = null
) {
    val safePoster: String get() = poster?.takeIf(String::isNotBlank) ?: backdrop?.takeIf(String::isNotBlank).orEmpty()
    val safeBackdrop: String get() = backdrop?.takeIf(String::isNotBlank) ?: poster?.takeIf(String::isNotBlank).orEmpty()
    val safeReleaseYear: String get() = year?.takeIf(String::isNotBlank) ?: "—"
    val safeQuality: String get() = quality?.takeIf(String::isNotBlank) ?: "HD"
    val safeRating: String get() = rating?.takeIf(String::isNotBlank) ?: "—"
    val safeDuration: String get() = duration?.takeIf(String::isNotBlank) ?: "—"
    val safeDescription: String get() = description ?: ""
}
