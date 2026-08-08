package com.pindou.app.data.segmentation

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Rect as CvRect
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

/**
 * 手动主体提取: OpenCV GrabCut
 * 用户框选矩形, 算法迭代分割
 */
class ManualSegmenter {

    /**
     * @param source 原图
     * @param rect 框选区域 (在原图坐标系)
     * @param iterations 迭代次数, 默认 5
     * @return mask: 1 = 前景, 0 = 背景, 长度 = source.width * source.height
     */
    fun grabCut(source: Bitmap, rect: Rect, iterations: Int = 5): IntArray {
        // Utils.bitmapToMat 要求可读写的 ARGB_8888 bitmap (hardware bitmap 不支持)
        val src = if (source.config == Bitmap.Config.ARGB_8888 && !source.isRecycled) {
            source
        } else {
            source.copy(Bitmap.Config.ARGB_8888, false)
        }

        val srcMat = Mat()
        val mask = Mat(src.height, src.width, CvType.CV_8UC1, Scalar(0.0))
        val bgdModel = Mat()
        val fgdModel = Mat()
        try {
            Utils.bitmapToMat(src, srcMat)
            Imgproc.cvtColor(srcMat, srcMat, Imgproc.COLOR_RGBA2RGB)

            val left = rect.left.coerceIn(0, src.width - 1)
            val top = rect.top.coerceIn(0, src.height - 1)
            val cvRect = CvRect(
                left,
                top,
                rect.width().coerceIn(1, src.width - left),
                rect.height().coerceIn(1, src.height - top)
            )

            Imgproc.grabCut(srcMat, mask, cvRect, bgdModel, fgdModel, iterations, Imgproc.GC_INIT_WITH_RECT)

            // mask 取值: 0=bg, 1=fg, 2=probable bg, 3=probable fg
            val maskBytes = ByteArray(src.width * src.height)
            mask.get(0, 0, maskBytes)

            val result = IntArray(src.width * src.height)
            for (i in maskBytes.indices) {
                val v = maskBytes[i].toInt() and 0xFF
                result[i] = if (v == 1 || v == 3) 1 else 0
            }
            return result
        } finally {
            // 确保异常时也释放 native Mat 内存
            srcMat.release()
            mask.release()
            bgdModel.release()
            fgdModel.release()
            if (src !== source) src.recycle()
        }
    }
}
