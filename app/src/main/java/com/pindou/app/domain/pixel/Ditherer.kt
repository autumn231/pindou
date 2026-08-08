package com.pindou.app.domain.pixel

import android.graphics.Bitmap
import com.pindou.app.domain.color.Quantizer
import com.pindou.app.domain.model.PixelGrid
import kotlin.math.roundToInt

/**
 * Floyd-Steinberg 抖动
 * 把量化误差分散给邻居像素, 用零散豆色模拟渐变
 */
class Ditherer(private val quantizer: Quantizer) {

    fun dither(
        source: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        mask: IntArray? = null
    ): PixelGrid {
        require(targetWidth > 0 && targetHeight > 0) { "目标尺寸必须 > 0" }
        val srcW = source.width
        val srcH = source.height
        val pixels = IntArray(srcW * srcH)
        source.getPixels(pixels, 0, srcW, 0, 0, srcW, srcH)

        val sx = srcW.toFloat() / targetWidth
        val sy = srcH.toFloat() / targetHeight

        val bufR = FloatArray(targetWidth * targetHeight)
        val bufG = FloatArray(targetWidth * targetHeight)
        val bufB = FloatArray(targetWidth * targetHeight)
        val transparent = BooleanArray(targetWidth * targetHeight)

        for (y in 0 until targetHeight) {
            for (x in 0 until targetWidth) {
                val idx = y * targetWidth + x
                if (mask != null && mask[idx] == 0) {
                    transparent[idx] = true
                    continue
                }
                // 采样区间 [srcX0, srcX1): 左闭右开, 避免相邻格子重叠采样
                val srcX0 = (x * sx).toInt().coerceIn(0, srcW - 1)
                val srcX1 = ((x + 1) * sx).toInt().coerceIn(srcX0 + 1, srcW)
                val srcY0 = (y * sy).toInt().coerceIn(0, srcH - 1)
                val srcY1 = ((y + 1) * sy).toInt().coerceIn(srcY0 + 1, srcH)

                var r = 0f
                var g = 0f
                var b = 0f
                var count = 0
                for (syi in srcY0 until srcY1) {
                    val rowOffset = syi * srcW
                    for (sxi in srcX0 until srcX1) {
                        val c = pixels[rowOffset + sxi]
                        // 跳过完全透明的像素, 避免拉暗均值
                        if ((c ushr 24) and 0xFF == 0) continue
                        r += ((c shr 16) and 0xFF).toFloat()
                        g += ((c shr 8) and 0xFF).toFloat()
                        b += (c and 0xFF).toFloat()
                        count++
                    }
                }
                if (count > 0) {
                    bufR[idx] = r / count
                    bufG[idx] = g / count
                    bufB[idx] = b / count
                }
            }
        }

        val indices = IntArray(targetWidth * targetHeight) { -1 }
        for (y in 0 until targetHeight) {
            for (x in 0 until targetWidth) {
                val idx = y * targetWidth + x
                if (transparent[idx]) continue

                val r = bufR[idx].roundToInt().coerceIn(0, 255)
                val g = bufG[idx].roundToInt().coerceIn(0, 255)
                val b = bufB[idx].roundToInt().coerceIn(0, 255)

                val paletteIdx = quantizer.quantize(r, g, b)
                indices[idx] = paletteIdx

                val beadColor = quantizer.colorAt(paletteIdx)
                val er = bufR[idx] - beadColor.r
                val eg = bufG[idx] - beadColor.g
                val eb = bufB[idx] - beadColor.b

                distribute(bufR, bufG, bufB, x + 1, y, targetWidth, targetHeight, er, eg, eb, 7f / 16f, transparent)
                distribute(bufR, bufG, bufB, x - 1, y + 1, targetWidth, targetHeight, er, eg, eb, 3f / 16f, transparent)
                distribute(bufR, bufG, bufB, x, y + 1, targetWidth, targetHeight, er, eg, eb, 5f / 16f, transparent)
                distribute(bufR, bufG, bufB, x + 1, y + 1, targetWidth, targetHeight, er, eg, eb, 1f / 16f, transparent)
            }
        }

        return PixelGrid(targetWidth, targetHeight, "", indices)
    }

    private fun distribute(
        bufR: FloatArray, bufG: FloatArray, bufB: FloatArray,
        x: Int, y: Int, w: Int, h: Int,
        er: Float, eg: Float, eb: Float, weight: Float,
        transparent: BooleanArray
    ) {
        if (x < 0 || x >= w || y < 0 || y >= h) return
        val idx = y * w + x
        if (transparent[idx]) return
        bufR[idx] += er * weight
        bufG[idx] += eg * weight
        bufB[idx] += eb * weight
    }
}
