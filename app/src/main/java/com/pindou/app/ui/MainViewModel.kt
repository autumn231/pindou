package com.pindou.app.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pindou.app.data.segmentation.SubjectExtractor
import com.pindou.app.domain.color.Quantizer
import com.pindou.app.domain.model.PixelGrid
import com.pindou.app.domain.palette.PaletteRegistry
import com.pindou.app.domain.pixel.Ditherer
import com.pindou.app.domain.pixel.Pixelator
import com.pindou.app.util.applyToBitmap
import com.pindou.app.util.scaledToMax
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val paletteRegistry = PaletteRegistry(application)
    val subjectExtractor = SubjectExtractor(application)

    var sourceBitmap by mutableStateOf<Bitmap?>(null)
        private set
    var maskedBitmap by mutableStateOf<Bitmap?>(null)
        private set
    var grid by mutableStateOf<PixelGrid?>(null)
        private set

    var paletteKey by mutableStateOf("artkal_c")
        private set
    var useDither by mutableStateOf(false)
        private set
    // val to avoid JVM setGridSize(I)V clash with fun setGridSize below
    private val _gridSize = mutableStateOf(50)
    val gridSize: Int by _gridSize

    var isProcessing by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private var mask: IntArray? = null

    val isAutoAvailable: Boolean get() = subjectExtractor.isAutoAvailable()

    init {
        subjectExtractor.init()
    }

    fun setSource(bmp: Bitmap) {
        val scaled = bmp.scaledToMax(1080)
        sourceBitmap = scaled
        maskedBitmap = scaled
        mask = null
        grid = null
        error = null
    }

    fun setPalette(key: String) {
        paletteKey = key
        grid = null
    }

    fun toggleDither() {
        useDither = !useDither
        grid = null
    }

    fun setGridSize(size: Int) {
        _gridSize.value = size.coerceIn(10, 200)
        grid = null
    }

    fun skipExtraction() {
        mask = null
        maskedBitmap = sourceBitmap
    }

    fun extractAuto(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            isProcessing = true
            error = null
            try {
                val src = sourceBitmap ?: run { onResult(false); return@launch }
                val m = withContext(Dispatchers.Default) {
                    subjectExtractor.extractAuto(src)
                }
                if (m != null) {
                    mask = m
                    maskedBitmap = m.applyToBitmap(src)
                    onResult(true)
                } else {
                    error = "自动提取不可用, 请使用手动模式"
                    onResult(false)
                }
            } catch (e: Throwable) {
                // Throwable 而非 Exception: 捕获 UnsatisfiedLinkError 等 Error 避免闪退
                error = e.message ?: "自动提取失败"
                onResult(false)
            } finally {
                isProcessing = false
            }
        }
    }

    fun extractManual(rect: Rect, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            isProcessing = true
            error = null
            try {
                val src = sourceBitmap ?: run { onResult(false); return@launch }
                val m = withContext(Dispatchers.Default) {
                    subjectExtractor.extractManual(src, rect)
                }
                mask = m
                maskedBitmap = m.applyToBitmap(src)
                onResult(true)
            } catch (e: Throwable) {
                // Throwable 而非 Exception: 捕获 UnsatisfiedLinkError 等 Error 避免闪退
                error = e.message ?: "手动提取失败"
                onResult(false)
            } finally {
                isProcessing = false
            }
        }
    }

    fun generateGrid(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            isProcessing = true
            error = null
            try {
                val src = maskedBitmap ?: run { onDone(); return@launch }
                val palette = paletteRegistry.load(paletteKey)
                val quantizer = Quantizer(palette)
                val (tw, th) = computeTargetGrid(src, gridSize)
                val maskForGrid = mask?.let { resizeMaskForGrid(it, src.width, src.height, tw, th) }

                val g = withContext(Dispatchers.Default) {
                    if (useDither) {
                        Ditherer(quantizer).dither(src, tw, th, maskForGrid)
                    } else {
                        Pixelator(quantizer).pixelate(src, tw, th, maskForGrid)
                    }
                }
                grid = g.copy(paletteKey = paletteKey)
                onDone()
            } catch (e: Exception) {
                error = e.message ?: "生成图纸失败"
                onDone()
            } finally {
                isProcessing = false
            }
        }
    }

    private fun computeTargetGrid(src: Bitmap, targetSize: Int): Pair<Int, Int> {
        val w = src.width
        val h = src.height
        return if (w >= h) {
            val tw = targetSize
            val th = (targetSize.toFloat() * h / w).toInt().coerceAtLeast(1)
            tw to th
        } else {
            val th = targetSize
            val tw = (targetSize.toFloat() * w / h).toInt().coerceAtLeast(1)
            tw to th
        }
    }

    private fun resizeMaskForGrid(mask: IntArray, srcW: Int, srcH: Int, dstW: Int, dstH: Int): IntArray {
        val out = IntArray(dstW * dstH)
        val xRatio = srcW.toFloat() / dstW
        val yRatio = srcH.toFloat() / dstH
        for (y in 0 until dstH) {
            for (x in 0 until dstW) {
                val sx = (x * xRatio).toInt().coerceIn(0, srcW - 1)
                val sy = (y * yRatio).toInt().coerceIn(0, srcH - 1)
                out[y * dstW + x] = mask[sy * srcW + sx]
            }
        }
        return out
    }

    override fun onCleared() {
        subjectExtractor.close()
        super.onCleared()
    }
}
