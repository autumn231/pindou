package com.pindou.app.domain.color

import kotlin.math.cbrt
import kotlin.math.pow

/**
 * RGB <-> Lab 颜色空间转换
 * 参照 sRGB + D65 标准光源
 */
object ColorSpaces {

    // sRGB -> XYZ 转换矩阵 (D65)
    private val M = arrayOf(
        doubleArrayOf(0.4124564, 0.3575761, 0.1804375),
        doubleArrayOf(0.2126729, 0.7151522, 0.0721750),
        doubleArrayOf(0.0193339, 0.1191920, 0.9503041)
    )

    // D65 参考白点
    private const val Xn = 0.95047
    private const val Yn = 1.00000
    private const val Zn = 1.08883

    /** RGB(0..255) -> Lab(L: 0..100, a/b: ~-128..128) */
    fun rgbToLab(r: Int, g: Int, b: Int): DoubleArray =
        rgbToLab(r.toDouble(), g.toDouble(), b.toDouble())

    /** 浮点 RGB 重载, 保留分数精度 (抖动用) */
    fun rgbToLab(r: Double, g: Double, b: Double): DoubleArray {
        // sRGB -> linear RGB
        val rl = inverseGamma(r / 255.0)
        val gl = inverseGamma(g / 255.0)
        val bl = inverseGamma(b / 255.0)

        // linear RGB -> XYZ
        val x = M[0][0] * rl + M[0][1] * gl + M[0][2] * bl
        val y = M[1][0] * rl + M[1][1] * gl + M[1][2] * bl
        val z = M[2][0] * rl + M[2][1] * gl + M[2][2] * bl

        // XYZ -> Lab
        val fx = labFunc(x / Xn)
        val fy = labFunc(y / Yn)
        val fz = labFunc(z / Zn)

        val L = 116.0 * fy - 16.0
        val a = 500.0 * (fx - fy)
        val bb = 200.0 * (fy - fz)

        return doubleArrayOf(L, a, bb)
    }

    private fun inverseGamma(c: Double): Double {
        return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    private fun labFunc(t: Double): Double {
        return if (t > 216.0 / 24389.0) cbrt(t) else (903.3 * t + 16.0) / 116.0
    }
}
