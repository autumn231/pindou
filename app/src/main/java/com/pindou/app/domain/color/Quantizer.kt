package com.pindou.app.domain.color

import com.pindou.app.domain.model.BeadColor
import com.pindou.app.domain.model.Palette

/**
 * 颜色量化器: 给定 RGB, 在调色板中找最接近的拼豆色 (基于 CIEDE2000)
 */
class Quantizer(private val palette: Palette) {

    // 预计算调色板里每个色的 Lab 值, 避免重复转换
    private val labColors: List<DoubleArray> = palette.colors.map {
        ColorSpaces.rgbToLab(it.r, it.g, it.b)
    }

    /** 找到最接近的豆色索引 (返回调色板里的下标) */
    fun quantize(r: Int, g: Int, b: Int): Int {
        val targetLab = ColorSpaces.rgbToLab(r, g, b)
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

    fun colorAt(index: Int): BeadColor = palette.colors[index]
}
