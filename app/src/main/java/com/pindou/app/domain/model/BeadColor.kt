package com.pindou.app.domain.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BeadColor(
    val code: String,
    val name: String,
    val rgb: List<Int>,
    val effect: String = "solid"
) {
    val r: Int get() = rgb.getOrElse(0) { 0 }
    val g: Int get() = rgb.getOrElse(1) { 0 }
    val b: Int get() = rgb.getOrElse(2) { 0 }
}
