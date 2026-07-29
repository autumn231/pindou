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
        // 1. 先用 OpenCVLoader.initLocal() (内部调用 System.loadLibrary("opencv_java4"))
        try {
            val ok = OpenCVLoader.initLocal()
            Log.i(TAG, "OpenCVLoader.initLocal() = $ok")
            if (ok) return
        } catch (e: Throwable) {
            Log.e(TAG, "OpenCVLoader.initLocal() threw", e)
        }
        // 2. 兠底: 直接 System.loadLibrary
        try {
            System.loadLibrary("opencv_java4")
            Log.i(TAG, "System.loadLibrary(opencv_java4) succeeded")
        } catch (e: Throwable) {
            Log.e(TAG, "System.loadLibrary(opencv_java4) FAILED", e)
        }
    }

    companion object {
        private const val TAG = "PindouApp"
    }
}
