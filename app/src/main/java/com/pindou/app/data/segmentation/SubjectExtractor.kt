package com.pindou.app.data.segmentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log

/**
 * 主体提取门面: 优先自动 (U2Net), 失败降级到手动 (GrabCut)
 */
class SubjectExtractor(private val context: Context) {

    private val autoSegmenter = AutoSegmenter(context)
    private val manualSegmenter = ManualSegmenter()

    fun init() {
        try {
            autoSegmenter.init()
        } catch (e: Throwable) {
            Log.w(TAG, "自动提取初始化失败, 降级为手动模式", e)
        }
    }

    fun isAutoAvailable(): Boolean = autoSegmenter.isInitialized()

    fun extractAuto(source: Bitmap): IntArray? {
        if (!autoSegmenter.isInitialized()) return null
        return try {
            autoSegmenter.segment(source)
        } catch (e: Throwable) {
            Log.w(TAG, "自动提取运行失败", e)
            null
        }
    }

    fun extractManual(source: Bitmap, rect: Rect): IntArray {
        return manualSegmenter.grabCut(source, rect)
    }

    fun close() {
        autoSegmenter.close()
    }

    companion object {
        private const val TAG = "SubjectExtractor"
    }
}
