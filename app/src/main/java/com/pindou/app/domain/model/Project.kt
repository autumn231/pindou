package com.pindou.app.domain.model

import android.graphics.Bitmap

data class Project(
    val id: Long = System.currentTimeMillis(),
    val name: String = "未命名",
    val sourceImage: Bitmap? = null,
    val paletteKey: String = "artkal_c",
    val grid: PixelGrid? = null,
    val useDither: Boolean = false,
    val gridSize: Int = 50
)
