package com.pindou.app

import android.app.Application
import android.util.Log
import org.opencv.android.OpenCVLoader

class PindouApp : Application() {
    override fun onCreate() {
        super.onCreate()
        loadOpenCV()
    }

    private fun loadOpenCV() {
        // OpenCVLoader.initLocal 内部调用 System.loadLibrary("opencv_java4")
        try {
            val ok = OpenCVLoader.initLocal()
            Log.i(TAG, "OpenCVLoader.initLocal() = $ok")
            if (ok) return
        } catch (e: Throwable) {
            Log.e(TAG, "OpenCVLoader.initLocal() threw", e)
        }
        // 兠底: 直接 System.loadLibrary
        try {
            System.loadLibrary("opencv_java4")
            Log.i(TAG, "System.loadLibrary(opencv_java4) succeeded")
        } catch (e: Throwable) {
            // 加载失败不抛出, 仅记录; GrabCut 调用时会再报错
            Log.e(TAG, "System.loadLibrary(opencv_java4) FAILED", e)
        }
    }

    companion object {
        private const val TAG = "PindouApp"
    }
}
