package com.salmanlaghari.risingflix.data

import com.google.gson.annotations.SerializedName

data class Category(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String,
    @SerializedName("icon") val icon: String?,
    @SerializedName("items") val items: List<MovieItem>
) {
    val safeId: String get() = id ?: name.lowercase().replace(" ", "_")
    val safeIcon: String get() = icon ?: "movie"
}
