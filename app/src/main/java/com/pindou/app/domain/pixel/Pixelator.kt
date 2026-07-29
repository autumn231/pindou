package com.pindou.app.domain.pixel

import android.graphics.Bitmap
import com.pindou.app.domain.color.Quantizer
import com.pindou.app.domain.model.PixelGrid

/**
 * 像素化: 把 Bitmap 下采样到 width x height 网格, 每格一个拼豆
 * 双线性区域平均采样 + CIEDE2000 量化
 */
class Pixelator(private val quantizer: Quantizer) {

    /**
     * @param source 原图
     * @param targetWidth 目标网格宽
     * @param targetHeight 目标网格高
     * @param mask 透明蒙版 (可选), 长度 = targetWidth*targetHeight, 0 = 透明
     */
    fun pixelate(
        source: Bitmap,
        targetWidth: Int,
        targetHeight: Int,
        mask: IntArray? = null
    ): PixelGrid {
        val indices = IntArray(targetWidth * targetHeight) { -1 }

        val srcW = source.width
        val srcH = source.height
        val pixels = IntArray(srcW * srcH)
        source.getPixels(pixels, 0, srcW, 0, 0, srcW, srcH)

        val sx = srcW.toFloat() / targetWidth
        val sy = srcH.toFloat() / targetHeight

        for (y in 0 until targetHeight) {
            for (x in 0 until targetWidth) {
                val idx = y * targetWidth + x
                if (mask != null && mask[idx] == 0) {
                    indices[idx] = -1
                    continue
                }
                val srcX0 = (x * sx).toInt().coerceIn(0, srcW - 1)
                val srcX1 = ((x + 1) * sx).toInt().coerceIn(0, srcW - 1)
                val srcY0 = (y * sy).toInt().coerceIn(0, srcH - 1)
                val srcY1 = ((y + 1) * sy).toInt().coerceIn(0, srcH - 1)

                var r = 0
                var g = 0
                var b = 0
                var count = 0
                for (syi in srcY0..srcY1) {
                    val rowOffset = syi * srcW
                    for (sxi in srcX0..srcX1) {
                        val c = pixels[rowOffset + sxi]
                        r += (c shr 16) and 0xFF
                        g += (c shr 8) and 0xFF
                        b += c and 0xFF
                        count++
                    }
                }
                if (count == 0) continue
                indices[idx] = quantizer.quantize(r / count, g / count, b / count)
            }
        }

        return PixelGrid(targetWidth, targetHeight, "", indices)
    }
}
