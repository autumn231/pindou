package com.pindou.app.util

import android.util.Log
import org.opencv.android.OpenCVLoader

/**
 * OpenCV native 库加载器 (全局单例, 线程安全)
 * 所有需要 OpenCV 的模块共享同一份加载状态, 避免重复加载
 */
object OpenCvInitializer {

    private const val TAG = "OpenCvInitializer"

    @Volatile
    private var loaded = false

    /**
     * 确保 OpenCV native 库已加载
     * @return true 已加载成功, false 加载失败
     */
    fun ensureLoaded(): Boolean {
        if (loaded) return true
        synchronized(this) {
            if (loaded) return true
            try {
                val ok = OpenCVLoader.initLocal()
                Log.i(TAG, "OpenCVLoader.initLocal() = $ok")
                if (!ok) {
                    // 降级: 尝试直接加载
                    System.loadLibrary("opencv_java4")
                    Log.i(TAG, "System.loadLibrary fallback ok")
                }
                loaded = true
            } catch (e: Throwable) {
                Log.e(TAG, "load OpenCV FAILED", e)
            }
            return loaded
        }
    }

    /** 是否已加载 */
    fun isLoaded(): Boolean = loaded
}
