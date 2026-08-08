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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val paletteRegistry = PaletteRegistry(application)
    private val subjectExtractor = SubjectExtractor(application)

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

    // 状态响应: isAutoAvailable 必须是 Compose State, 否则 TFLite 异步加载后 UI 不重组
    var isAutoAvailable by mutableStateOf(false)
        private set

    private var mask: IntArray? = null
    // 串行化耗时操作, 避免快速连点并发写状态
    private var processJob: Job? = null

    init {
        // 异步初始化 TFLite, 失败不影响 App 启动 (自动抠图不可用时降级到手动)
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                try {
                    subjectExtractor.init()
                    isAutoAvailable = subjectExtractor.isAutoAvailable()
                } catch (e: Throwable) {
                    error = "自动提取初始化失败: ${e.message}"
                }
            }
        }
    }

    fun setSource(bmp: Bitmap) {
        val scaled = bmp.scaledToMax(1080)
        // 回收旧 Bitmap, 避免内存堆积
        if (scaled !== bmp) {
            // 缩放产生了新对象, 原图可回收
            bmp.recycle()
        }
        val oldSource = sourceBitmap
        val oldMasked = maskedBitmap
        sourceBitmap = scaled
        maskedBitmap = scaled
        mask = null
        grid = null
        error = null
        // 旧的 maskedBitmap 如果与 oldSource 不同实例, 单独回收
        if (oldMasked !== null && oldMasked !== oldSource) oldMasked?.recycle()
        oldSource?.recycle()
    }

    fun setPalette(key: String) {
        paletteKey = key
        grid = null
        error = null
    }

    fun toggleDither() {
        useDither = !useDither
        grid = null
        error = null
    }

    fun setGridSize(size: Int) {
        _gridSize.value = size.coerceIn(10, 200)
        grid = null
        error = null
    }

    fun skipExtraction() {
        mask = null
        maskedBitmap = sourceBitmap
    }

    private fun startProcessing(block: suspend () -> Unit) {
        // 取消上一个耗时操作, 串行化避免并发污染状态
        processJob?.cancel()
        processJob = viewModelScope.launch {
            isProcessing = true
            error = null
            try {
                block()
            } finally {
                isProcessing = false
            }
        }
    }

    fun extractAuto(onResult: (Boolean) -> Unit) {
        startProcessing {
            try {
                val src = sourceBitmap ?: run { onResult(false); return@startProcessing }
                val result = withContext(Dispatchers.Default) {
                    val m = subjectExtractor.extractAuto(src)
                    if (m != null) m to m.applyToBitmap(src) else null
                }
                if (result != null) {
                    mask = result.first
                    val oldMasked = maskedBitmap
                    maskedBitmap = result.second
                    if (oldMasked !== null && oldMasked !== sourceBitmap && oldMasked !== result.second) {
                        oldMasked.recycle()
                    }
                    onResult(true)
                } else {
                    error = "自动提取不可用, 请使用手动模式"
                    onResult(false)
                }
            } catch (e: Throwable) {
                error = e.message ?: "自动提取失败"
                onResult(false)
            }
        }
    }

    fun extractManual(rect: Rect, onResult: (Boolean) -> Unit) {
        startProcessing {
            try {
                val src = sourceBitmap ?: run { onResult(false); return@startProcessing }
                val result = withContext(Dispatchers.Default) {
                    val m = subjectExtractor.extractManual(src, rect)
                    m to m.applyToBitmap(src)
                }
                mask = result.first
                val oldMasked = maskedBitmap
                maskedBitmap = result.second
                if (oldMasked !== null && oldMasked !== sourceBitmap && oldMasked !== result.second) {
                    oldMasked.recycle()
                }
                onResult(true)
            } catch (e: Throwable) {
                error = e.message ?: "手动提取失败"
                onResult(false)
            }
        }
    }

    fun generateGrid(onDone: () -> Unit = {}) {
        startProcessing {
            try {
                val src = maskedBitmap ?: run { onDone(); return@startProcessing }
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
        processJob?.cancel()
        subjectExtractor.close()
        sourceBitmap?.recycle()
        if (maskedBitmap !== sourceBitmap) maskedBitmap?.recycle()
        super.onCleared()
    }
}
