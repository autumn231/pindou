package com.pindou.app.data

import android.graphics.Bitmap
import com.pindou.app.util.OpenCvInitializer
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

/**
 * 图像增强: 解决用户拍照常见的光线问题
 *
 * 算法:
 * - CLAHE: 限制对比度自适应直方图均衡化, 改善局部光线不均
 * - 白平衡 (灰世界): 消除色偏, 还原真实色彩
 * - Gamma 校正: 调整整体明暗
 * - 自动增强: 智能组合以上算法, 一键优化
 */
class ImageEnhancer {

    /**
     * CLAHE 对比度增强
     * 在 LAB 颜色空间对亮度通道做局部直方图均衡, 不影响色彩
     *
     * @param source 原图
     * @param clipLimit 对比度限制 (越大增强越强, 推荐 2.0~4.0)
     * @param tileGridSize 分块大小 (推荐 8x8)
     */
    fun enhanceCLAHE(
        source: Bitmap,
        clipLimit: Double = 2.0,
        tileGridSize: Int = 8
    ): Bitmap {
        if (!OpenCvInitializer.ensureLoaded()) {
            throw IllegalStateException("OpenCV 未加载, 无法执行图像增强")
        }

        val rgba = Mat()
        val rgb = Mat()
        val lab = Mat()
        val channels = ArrayList<Mat>()
        val clahe = Imgproc.createCLAHE(clipLimit, org.opencv.core.Size(tileGridSize.toDouble(), tileGridSize.toDouble()))
        try {
            Utils.bitmapToMat(source, rgba)
            Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2RGB)
            Imgproc.cvtColor(rgb, lab, Imgproc.COLOR_RGB2Lab)

            Core.split(lab, channels)
            // L 通道 (channels[0]) 做 CLAHE, a/b 通道不变
            clahe.apply(channels[0], channels[0])
            Core.merge(channels, lab)

            Imgproc.cvtColor(lab, rgb, Imgproc.COLOR_Lab2RGB)
            Imgproc.cvtColor(rgb, rgba, Imgproc.COLOR_RGB2RGBA)

            val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(rgba, result)
            return result
        } finally {
            rgba.release()
            rgb.release()
            lab.release()
            channels.forEach { it.release() }
            // CLAHE 继承自 Algorithm, 使用 clear() 释放原生资源
            clahe.clear()
        }
    }

    /**
     * 白平衡 (灰世界算法 Gray World)
     * 假设场景平均色为中性灰, 按各通道均值缩放消除色偏
     *
     * @param source 原图
     * @param strength 增强强度 0.0~1.0 (1.0 = 完全校正)
     */
    fun whiteBalance(source: Bitmap, strength: Double = 0.8): Bitmap {
        if (!OpenCvInitializer.ensureLoaded()) {
            throw IllegalStateException("OpenCV 未加载, 无法执行图像增强")
        }

        val rgba = Mat()
        val rgb = Mat()
        try {
            Utils.bitmapToMat(source, rgba)
            Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2RGB)

            // 计算各通道均值
            val mean = Core.mean(rgb)
            val avgR = mean.`val`[0]
            val avgG = mean.`val`[1]
            val avgB = mean.`val`[2]
            val grayAvg = (avgR + avgG + avgB) / 3.0

            if (grayAvg < 1.0) return source.copy(Bitmap.Config.ARGB_8888, false)

            // 计算缩放因子, 混合原始值 (strength 控制强度)
            val factor = 1.0 - strength
            val scaleR = (grayAvg / avgR * strength + factor).coerceIn(0.5, 2.0)
            val scaleG = (grayAvg / avgG * strength + factor).coerceIn(0.5, 2.0)
            val scaleB = (grayAvg / avgB * strength + factor).coerceIn(0.5, 2.0)

            // 应用缩放
            val scales = Scalar(scaleR, scaleG, scaleB)
            Core.multiply(rgb, scales, rgb, 1.0, CvType.CV_8UC3)

            Imgproc.cvtColor(rgb, rgba, Imgproc.COLOR_RGB2RGBA)
            val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(rgba, result)
            return result
        } finally {
            rgba.release()
            rgb.release()
        }
    }

    /**
     * Gamma 校正
     * gamma < 1 提亮暗部, gamma > 1 压暗亮部
     *
     * @param source 原图
     * @param gamma gamma 值 (0.1~3.0, 1.0 = 不变)
     */
    fun gammaCorrect(source: Bitmap, gamma: Double): Bitmap {
        if (!OpenCvInitializer.ensureLoaded()) {
            throw IllegalStateException("OpenCV 未加载, 无法执行图像增强")
        }
        if (gamma == 1.0) return source.copy(Bitmap.Config.ARGB_8888, false)

        val rgba = Mat()
        val rgb = Mat()
        try {
            Utils.bitmapToMat(source, rgba)
            Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2RGB)

            // 构建查找表 (0~255 -> 校正后 0~255)
            val lut = Mat(1, 256, CvType.CV_8U)
            val invGamma = 1.0 / gamma
            val lutData = ByteArray(256)
            for (i in 0 until 256) {
                val corrected = Math.pow(i / 255.0, invGamma) * 255.0
                lutData[i] = corrected.toInt().coerceIn(0, 255).toByte()
            }
            lut.put(0, 0, lutData)
            Core.LUT(rgb, lut, rgb)

            Imgproc.cvtColor(rgb, rgba, Imgproc.COLOR_RGB2RGBA)
            val result = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(rgba, result)
            return result
        } finally {
            rgba.release()
            rgb.release()
        }
    }

    /**
     * 自动增强: 智能分析图像并组合多项处理
     * 1. 白平衡消除色偏
     * 2. CLAHE 改善局部对比度
     * 3. 根据平均亮度自动 Gamma 校正
     *
     * @param source 原图
     * @return 增强后的 Bitmap
     */
    fun autoEnhance(source: Bitmap): Bitmap {
        if (!OpenCvInitializer.ensureLoaded()) {
            throw IllegalStateException("OpenCV 未加载, 无法执行图像增强")
        }

        // 分析原图亮度
        val avgBrightness = calculateBrightness(source)

        // 1. 白平衡
        var result = whiteBalance(source, strength = 0.7)

        // 2. CLAHE (根据亮度调整强度)
        val clipLimit = when {
            avgBrightness < 60 -> 3.0   // 很暗, 强增强
            avgBrightness < 100 -> 2.5  // 偏暗
            avgBrightness > 200 -> 1.5  // 很亮, 弱增强
            else -> 2.0                 // 正常
        }
        val claheResult = enhanceCLAHE(result, clipLimit = clipLimit)
        // 回收白平衡产生的中间 Bitmap
        if (result !== source) result.recycle()
        result = claheResult

        // 3. 自动 Gamma (根据亮度)
        val gamma = when {
            avgBrightness < 60 -> 0.6   // 很暗, 大幅提亮
            avgBrightness < 100 -> 0.75 // 偏暗, 提亮
            avgBrightness > 200 -> 1.3  // 很亮, 压暗
            avgBrightness > 170 -> 1.15 // 偏亮, 轻微压暗
            else -> 1.0                 // 正常, 不调整
        }
        if (gamma != 1.0) {
            val gammaResult = gammaCorrect(result, gamma)
            // 回收 CLAHE 产生的中间 Bitmap (claheResult 此时 == result)
            if (result !== source) result.recycle()
            result = gammaResult
        }

        return result
    }

    /**
     * 计算图像平均亮度 (0~255)
     */
    private fun calculateBrightness(source: Bitmap): Double {
        val rgba = Mat()
        val gray = Mat()
        try {
            Utils.bitmapToMat(source, rgba)
            Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
            return Core.mean(gray).`val`[0]
        } finally {
            rgba.release()
            gray.release()
        }
    }
}
