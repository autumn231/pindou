package com.pindou.app.domain.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Palette(
    val brand: String,
    val spec: String = "",
    val version: String = "",
    val source: String = "",
    val colors: List<BeadColor>
)
