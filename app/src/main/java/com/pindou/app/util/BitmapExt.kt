package com.pindou.app.util

import android.graphics.Bitmap

/** Bitmap 工具函数 */

fun Bitmap.scaledToMax(maxDim: Int): Bitmap {
    val w = width
    val h = height
    val scale = if (w >= h) maxDim.toFloat() / w else maxDim.toFloat() / h
    if (scale >= 1f) return this
    val newW = (w * scale).toInt().coerceAtLeast(1)
    val newH = (h * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, newW, newH, true)
}

/** 把 mask (长度 = w*h, 0 = 透明) 应用到 source, 返回透明背景的 Bitmap */
fun IntArray.applyToBitmap(source: Bitmap): Bitmap {
    val w = source.width
    val h = source.height
    if (size != w * h) {
        throw IllegalArgumentException("mask size ${size} != bitmap ${w * h}")
    }
    // 原地修改, 避免双倍内存分配
    val pixels = IntArray(w * h)
    source.getPixels(pixels, 0, w, 0, 0, w, h)
    for (i in pixels.indices) {
        if (this[i] == 0) pixels[i] = 0
    }
    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    result.setPixels(pixels, 0, w, 0, 0, w, h)
    return result
}
