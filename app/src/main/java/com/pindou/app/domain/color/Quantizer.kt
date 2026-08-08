package com.pindou.app.domain.color

import com.pindou.app.domain.model.BeadColor
import com.pindou.app.domain.model.Palette

/**
 * 颜色量化器: 给定 RGB, 在调色板中找最接近的拼豆色 (基于 CIEDE2000)
 */
class Quantizer(private val palette: Palette) {

    init {
        require(palette.colors.isNotEmpty()) { "调色板不能为空" }
    }

    // 预计算调色板里每个色的 Lab 值, 避免重复转换
    private val labColors: List<DoubleArray> = palette.colors.map {
        ColorSpaces.rgbToLab(it.r, it.g, it.b)
    }

    /** 找到最接近的豆色索引 (返回调色板里的下标) */
    fun quantize(r: Int, g: Int, b: Int): Int {
        val rc = r.coerceIn(0, 255)
        val gc = g.coerceIn(0, 255)
        val bc = b.coerceIn(0, 255)
        val targetLab = ColorSpaces.rgbToLab(rc, gc, bc)
        return findClosest(targetLab)
    }

    /** 浮点 RGB 入参, 保留分数精度 (抖动用) */
    fun quantize(r: Float, g: Float, b: Float): Int {
        val targetLab = ColorSpaces.rgbToLab(
            r.coerceIn(0f, 255f).toDouble(),
            g.coerceIn(0f, 255f).toDouble(),
            b.coerceIn(0f, 255f).toDouble()
        )
        return findClosest(targetLab)
    }

    fun quantize(lab: DoubleArray): Int = findClosest(lab)

    private fun findClosest(targetLab: DoubleArray): Int {
        var bestIdx = 0
        var bestDist = Double.MAX_VALUE
        labColors.forEachIndexed { idx, lab ->
            val d = ColorDifference.ciede2000(targetLab, lab)
            if (d < bestDist) {
                bestDist = d
                bestIdx = idx
            }
        }
        return bestIdx
    }

    fun colorAt(index: Int): BeadColor =
        palette.colors.getOrElse(index) { palette.colors[0] }
}
