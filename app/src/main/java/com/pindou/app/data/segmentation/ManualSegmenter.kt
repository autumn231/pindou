package com.pindou.app.data.segmentation

import android.graphics.Bitmap
import android.graphics.Rect
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Rect as CvRect
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

/**
 * 手动主体提取: OpenCV GrabCut
 * 用户框选矩形, 算法迭代分割, 失败可手动加画笔修正
 */
class ManualSegmenter {

    /**
     * @param source 原图
     * @param rect 框选区域 (在原图坐标系)
     * @param iterations 迭代次数, 默认 5
     * @return mask: 1 = 前景, 0 = 背景, 长度 = source.width * source.height
     */
    fun grabCut(source: Bitmap, rect: Rect, iterations: Int = 5): IntArray {
        val srcMat = Mat()
        Utils.bitmapToMat(source, srcMat)
        Imgproc.cvtColor(srcMat, srcMat, Imgproc.COLOR_RGBA2RGB)

        val mask = Mat(source.height, source.width, CvType.CV_8UC1, Scalar(0.0))
        val bgdModel = Mat()
        val fgdModel = Mat()
        val cvRect = CvRect(
            rect.left.coerceAtLeast(0),
            rect.top.coerceAtLeast(0),
            rect.width().coerceAtLeast(1).coerceAtMost(source.width - rect.left.coerceAtLeast(0)),
            rect.height().coerceAtLeast(1).coerceAtMost(source.height - rect.top.coerceAtLeast(0))
        )

        Imgproc.grabCut(srcMat, mask, cvRect, bgdModel, fgdModel, iterations, Imgproc.GC_INIT_WITH_RECT)

        // mask 取值: 0=bg, 1=fg, 2=probable bg, 3=probable fg
        val maskBytes = ByteArray(source.width * source.height)
        mask.get(0, 0, maskBytes)

        val result = IntArray(source.width * source.height)
        for (i in maskBytes.indices) {
            val v = maskBytes[i].toInt() and 0xFF
            result[i] = if (v == 1 || v == 3) 1 else 0
        }

        srcMat.release()
        mask.release()
        bgdModel.release()
        fgdModel.release()

        return result
    }
}
