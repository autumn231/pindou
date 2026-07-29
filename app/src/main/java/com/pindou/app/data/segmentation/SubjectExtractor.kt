package com.pindou.app.data.segmentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect

/**
 * 主体提取门面: 优先自动 (U2Net), 失败降级到手动 (GrabCut)
 */
class SubjectExtractor(private val context: Context) {

    val autoSegmenter = AutoSegmenter(context)
    val manualSegmenter = ManualSegmenter()

    fun init() {
        try {
            autoSegmenter.init()
        } catch (e: Exception) {
            // 自动加载失败时, 后续走手动模式
        }
    }

    fun isAutoAvailable(): Boolean = autoSegmenter.isInitialized()

    fun extractAuto(source: Bitmap): IntArray? {
        if (!autoSegmenter.isInitialized()) return null
        return try {
            autoSegmenter.segment(source)
        } catch (e: Exception) {
            null
        }
    }

    fun extractManual(source: Bitmap, rect: Rect): IntArray {
        return manualSegmenter.grabCut(source, rect)
    }

    fun close() {
        autoSegmenter.close()
    }
}
