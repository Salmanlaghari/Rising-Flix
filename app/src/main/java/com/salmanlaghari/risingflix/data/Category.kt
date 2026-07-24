package com.salmanlaghari.risingflix.data

import com.google.gson.annotations.SerializedName

data class Category(
    @SerializedName("name") val name: String,
    @SerializedName("items") val items: List<VideoItem>
)
