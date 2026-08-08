package com.pindou.app.data.segmentation

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * 自动主体提取: TFLite U2Net 推理
 * 输入: Bitmap -> 缩放到 320x320 -> 归一化 -> 推理 -> mask
 * 自适应 uint8/fp32 输入输出类型
 */
class AutoSegmenter(private val context: Context) {

    @Volatile
    private var interpreter: Interpreter? = null
    @Volatile
    private var inputDataType: DataType = DataType.FLOAT32
    @Volatile
    private var outputDataType: DataType = DataType.FLOAT32
    private val inputSize = 320

    fun init() {
        val modelBuffer = loadModelFile("models/u2net.tflite")
        val options = Interpreter.Options().apply { setNumThreads(4) }
        interpreter = Interpreter(modelBuffer, options).also {
            inputDataType = it.getInputTensor(0).dataType()
            outputDataType = it.getOutputTensor(0).dataType()
        }
    }

    fun isInitialized(): Boolean = interpreter != null

    fun segment(source: Bitmap): IntArray {
        val interp = interpreter ?: throw IllegalStateException("AutoSegmenter 未初始化")
        val scaled = Bitmap.createScaledBitmap(source, inputSize, inputSize, true)
        try {
            val pixels = IntArray(inputSize * inputSize)
            scaled.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

            val input: ByteBuffer = when (inputDataType) {
                DataType.UINT8 -> {
                    ByteBuffer.allocateDirect(inputSize * inputSize * 3).order(ByteOrder.nativeOrder()).also { buf ->
                        for (pixel in pixels) {
                            buf.put(((pixel shr 16) and 0xFF).toByte())
                            buf.put(((pixel shr 8) and 0xFF).toByte())
                            buf.put((pixel and 0xFF).toByte())
                        }
                        buf.rewind()
                    }
                }
                else -> {
                    ByteBuffer.allocateDirect(inputSize * inputSize * 3 * 4).order(ByteOrder.nativeOrder()).also { buf ->
                        for (pixel in pixels) {
                            buf.putFloat(((pixel shr 16) and 0xFF).toFloat() / 255.0f)
                            buf.putFloat(((pixel shr 8) and 0xFF).toFloat() / 255.0f)
                            buf.putFloat((pixel and 0xFF).toFloat() / 255.0f)
                        }
                        buf.rewind()
                    }
                }
            }

            val outputBytes = if (outputDataType == DataType.UINT8) 1 else 4
            val output = ByteBuffer.allocateDirect(inputSize * inputSize * outputBytes).order(ByteOrder.nativeOrder())
            val outFloat = if (outputDataType != DataType.UINT8) output.asFloatBuffer() else null

            interp.run(input, output)

            val maskSmall = IntArray(inputSize * inputSize)
            for (i in 0 until inputSize * inputSize) {
                val v = if (outputDataType == DataType.UINT8) {
                    (output.get(i).toInt() and 0xFF) / 255.0f
                } else {
                    outFloat!!.get(i)
                }
                maskSmall[i] = if (v > 0.5f) 1 else 0
            }
            return resizeMask(maskSmall, inputSize, inputSize, source.width, source.height)
        } finally {
            scaled.recycle()
        }
    }

    private fun resizeMask(mask: IntArray, sw: Int, sh: Int, dw: Int, dh: Int): IntArray {
        val out = IntArray(dw * dh)
        val xRatio = sw.toFloat() / dw
        val yRatio = sh.toFloat() / dh
        for (y in 0 until dh) {
            for (x in 0 until dw) {
                val sx = (x * xRatio).toInt().coerceIn(0, sw - 1)
                val sy = (y * yRatio).toInt().coerceIn(0, sh - 1)
                out[y * dw + x] = mask[sy * sw + sx]
            }
        }
        return out
    }

    private fun loadModelFile(assetPath: String): MappedByteBuffer {
        // openFd 返回的 AssetFileDescriptor 必须关闭, 否则 fd 泄漏
        return context.assets.openFd(assetPath).use { fis ->
            FileInputStream(fis.fileDescriptor).use { stream ->
                stream.channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    fis.startOffset,
                    fis.declaredLength
                )
            }
        }
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
