package com.pindou.app

import android.app.Application
import org.opencv.android.OpenCVLoader

class PindouApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 提前加载 OpenCV native 库 (libopencv_java4.so)
        // 否则 GrabCut 首次调用 Mat() 会抛 UnsatisfiedLinkError 闪退
        OpenCVLoader.initLocal()
    }
}
