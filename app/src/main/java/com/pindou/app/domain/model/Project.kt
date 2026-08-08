package com.pindou.app.domain.model

/**
 * 可持久化的项目元数据 (不含 Bitmap, 用于列表展示和存储)
 */
data class ProjectMeta(
    val id: Long = System.currentTimeMillis(),
    val name: String = "未命名",
    val paletteKey: String = "artkal_c",
    val useDither: Boolean = false,
    val gridSize: Int = 50,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
