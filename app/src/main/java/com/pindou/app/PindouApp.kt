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
        // 方式1: OpenCVLoader.initLocal() (内部 System.loadLibrary + 触发 Core.getBuildInformation)
        try {
            val ok = OpenCVLoader.initLocal()
            Log.i(TAG, "OpenCVLoader.initLocal() = $ok")
            if (ok) return
        } catch (e: Throwable) {
            Log.e(TAG, "OpenCVLoader.initLocal() threw", e)
        }
        // 方式2: 直接 System.loadLibrary
        try {
            System.loadLibrary("opencv_java4")
            Log.i(TAG, "System.loadLibrary(opencv_java4) succeeded")
            return
        } catch (e: Throwable) {
            Log.e(TAG, "System.loadLibrary(opencv_java4) FAILED", e)
        }
        // 方式3: 用绝对路径 System.load 加载已解压的 .so
        try {
            val soPath = applicationInfo.nativeLibraryDir + "/libopencv_java4.so"
            Log.i(TAG, "trying System.load($soPath)")
            System.load(soPath)
            Log.i(TAG, "System.load($soPath) succeeded")
        } catch (e: Throwable) {
            // 加载失败不抛出, 仅记录; GrabCut 调用时会再报错
            Log.e(TAG, "System.load absolute path FAILED", e)
        }
    }

    companion object {
        private const val TAG = "PindouApp"
    }
}
