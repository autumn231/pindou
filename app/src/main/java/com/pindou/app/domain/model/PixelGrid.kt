package com.pindou.app.domain.model

/**
 * 像素化后的网格: 每格存调色板里的颜色索引 (or -1 表示透明)
 */
data class PixelGrid(
    val width: Int,
    val height: Int,
    val paletteKey: String,
    val colorIndices: IntArray  // length = width*height, -1 = transparent
) {
    fun get(x: Int, y: Int): Int = colorIndices[y * width + x]

    fun set(x: Int, y: Int, value: Int) {
        colorIndices[y * width + x] = value
    }

    /** 统计每种颜色使用量, 返回 (colorIndex, count) 列表, 按数量降序 */
    fun usageCounts(): List<Pair<Int, Int>> {
        val counts = HashMap<Int, Int>()
        for (idx in colorIndices) {
            if (idx < 0) continue
            counts[idx] = (counts[idx] ?: 0) + 1
        }
        return counts.entries.map { it.key to it.value }.sortedByDescending { it.second }
    }

    /** 使用的不同颜色种类数 */
    val colorCount: Int get() = colorIndices.toSet().count { it >= 0 }

    /** 总豆数 (不含透明格) */
    val totalBeads: Int get() = colorIndices.count { it >= 0 }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PixelGrid
        if (width != other.width) return false
        if (height != other.height) return false
        if (paletteKey != other.paletteKey) return false
        if (!colorIndices.contentEquals(other.colorIndices)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + paletteKey.hashCode()
        result = 31 * result + colorIndices.contentHashCode()
        return result
    }
}
