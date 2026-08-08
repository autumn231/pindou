package com.pindou.app.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pindou.app.data.ImageEnhancer
import com.pindou.app.data.ProjectRepository
import com.pindou.app.data.segmentation.SubjectExtractor
import com.pindou.app.domain.color.Quantizer
import com.pindou.app.domain.model.PixelGrid
import com.pindou.app.domain.model.ProjectMeta
import com.pindou.app.domain.palette.PaletteRegistry
import com.pindou.app.domain.pixel.Ditherer
import com.pindou.app.domain.pixel.Pixelator
import com.pindou.app.util.applyToBitmap
import com.pindou.app.util.rotate
import com.pindou.app.util.scaledToMax
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val paletteRegistry = PaletteRegistry(application)
    val subjectExtractor = SubjectExtractor(application)
    val projectRepository = ProjectRepository(application)
    val imageEnhancer = ImageEnhancer()

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
    private val _gridSize = mutableStateOf(50)
    val gridSize: Int by _gridSize

    var isProcessing by mutableStateOf(false)
        private set

    // 当前屏幕的错误信息 (切换屏幕时自动清除, 避免跨屏泄漏)
    var error by mutableStateOf<String?>(null)
        private set

    // 保存操作的结果消息 (用于 Snackbar 提示)
    var message by mutableStateOf<String?>(null)
        private set

    // 可观察的自动提取可用状态, init 完成后更新 UI
    var isAutoAvailable by mutableStateOf(false)
        private set

    // 当前项目元数据 (用于保存/加载)
    var currentProject by mutableStateOf<ProjectMeta?>(null)
        private set

    // 是否有未保存的修改
    var hasUnsavedChanges by mutableStateOf(false)
        private set

    private var mask: IntArray? = null
    private var processJob: Job? = null
    private var gridSizeDebounceJob: Job? = null

    init {
        // 异步初始化 TFLite, 失败不影响 App 启动
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
        if (scaled !== bmp) {
            bmp.recycle()
        }
        val oldSource = sourceBitmap
        val oldMasked = maskedBitmap
        sourceBitmap = scaled
        maskedBitmap = scaled
        mask = null
        grid = null
        error = null
        if (oldMasked != null && oldMasked !== oldSource) {
            oldMasked?.recycle()
        }
        oldSource?.recycle()
        markDirty()
    }

    fun setPalette(key: String) {
        if (paletteKey == key) return
        paletteKey = key
        grid = null
        markDirty()
    }

    fun toggleDither() {
        useDither = !useDither
        grid = null
        markDirty()
    }

    /**
     * 滑块拖动时实时更新显示值, 但延迟清除 grid (防抖)
     * 避免拖动过程中每个 tick 都清空已生成的图纸
     */
    fun setGridSize(size: Int) {
        val clamped = size.coerceIn(10, 200)
        if (_gridSize.value == clamped) return
        _gridSize.value = clamped
        markDirty()
        // 防抖: 500ms 内不再改变才清除 grid
        gridSizeDebounceJob?.cancel()
        gridSizeDebounceJob = viewModelScope.launch {
            delay(500)
            grid = null
        }
    }

    fun skipExtraction() {
        mask = null
        maskedBitmap = sourceBitmap
        markDirty()
    }

    /**
     * 旋转原图 90 度 (顺时针), 同时清除已有的 mask 和 grid
     */
    fun rotateSource() {
        val src = sourceBitmap ?: return
        val rotated = src.rotate(90f)
        if (rotated !== src) {
            val oldMasked = maskedBitmap
            sourceBitmap = rotated
            maskedBitmap = rotated
            mask = null
            grid = null
            if (oldMasked != null && oldMasked !== src) oldMasked.recycle()
            src.recycle()
            markDirty()
        }
    }

    /** 清除提取的蒙版, 恢复原图 */
    fun clearMask() {
        mask = null
        val oldMasked = maskedBitmap
        maskedBitmap = sourceBitmap
        if (oldMasked != null && oldMasked !== sourceBitmap) {
            oldMasked.recycle()
        }
        markDirty()
    }

    /**
     * 自动增强: 智能分析图像并组合白平衡 + CLAHE + Gamma
     * 适合一键优化光线不好的照片
     */
    fun enhanceAuto() {
        startProcessing {
            try {
                val src = sourceBitmap ?: return@startProcessing
                val enhanced = withContext(Dispatchers.Default) {
                    imageEnhancer.autoEnhance(src)
                }
                replaceSourceBitmap(enhanced)
                message = "自动增强完成"
            } catch (e: Throwable) {
                error = e.message ?: "图像增强失败"
            }
        }
    }

    /**
     * CLAHE 对比度增强
     * @param clipLimit 对比度限制 (1.0~5.0, 越大越强)
     */
    fun enhanceCLAHE(clipLimit: Double = 2.0) {
        startProcessing {
            try {
                val src = sourceBitmap ?: return@startProcessing
                val enhanced = withContext(Dispatchers.Default) {
                    imageEnhancer.enhanceCLAHE(src, clipLimit = clipLimit)
                }
                replaceSourceBitmap(enhanced)
                message = "对比度增强完成"
            } catch (e: Throwable) {
                error = e.message ?: "图像增强失败"
            }
        }
    }

    /**
     * 白平衡 (消除色偏)
     * @param strength 校正强度 (0.0~1.0)
     */
    fun enhanceWhiteBalance(strength: Double = 0.8) {
        startProcessing {
            try {
                val src = sourceBitmap ?: return@startProcessing
                val enhanced = withContext(Dispatchers.Default) {
                    imageEnhancer.whiteBalance(src, strength = strength)
                }
                replaceSourceBitmap(enhanced)
                message = "白平衡校正完成"
            } catch (e: Throwable) {
                error = e.message ?: "图像增强失败"
            }
        }
    }

    /**
     * Gamma 亮度校正
     * @param gamma < 1 提亮, > 1 压暗
     */
    fun enhanceGamma(gamma: Double) {
        startProcessing {
            try {
                val src = sourceBitmap ?: return@startProcessing
                val enhanced = withContext(Dispatchers.Default) {
                    imageEnhancer.gammaCorrect(src, gamma = gamma)
                }
                replaceSourceBitmap(enhanced)
                message = "亮度校正完成"
            } catch (e: Throwable) {
                error = e.message ?: "图像增强失败"
            }
        }
    }

    /**
     * 替换 sourceBitmap (增强/旋转后), 回收旧 Bitmap, 清除 mask 和 grid
     */
    private fun replaceSourceBitmap(newBitmap: Bitmap) {
        val oldSource = sourceBitmap
        val oldMasked = maskedBitmap
        sourceBitmap = newBitmap
        maskedBitmap = newBitmap
        mask = null
        grid = null
        if (oldMasked != null && oldMasked !== oldSource) {
            oldMasked.recycle()
        }
        oldSource?.recycle()
        markDirty()
    }

    /** 清除当前屏幕的错误消息 */
    fun clearError() {
        error = null
    }

    /** 清除消息 (Snackbar 显示后调用) */
    fun clearMessage() {
        message = null
    }

    private fun markDirty() {
        hasUnsavedChanges = true
    }

    /**
     * 串行化处理: 取消上一次未完成的操作
     */
    private fun startProcessing(block: suspend () -> Unit) {
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
                val m = withContext(Dispatchers.Default) {
                    subjectExtractor.extractAuto(src)
                }
                if (m != null) {
                    mask = m
                    val newMasked = withContext(Dispatchers.Default) {
                        m.applyToBitmap(src)
                    }
                    val oldMasked = maskedBitmap
                    maskedBitmap = newMasked
                    if (oldMasked != null && oldMasked !== sourceBitmap) {
                        oldMasked.recycle()
                    }
                    markDirty()
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
                val m = withContext(Dispatchers.Default) {
                    subjectExtractor.extractManual(src, rect)
                }
                mask = m
                val newMasked = withContext(Dispatchers.Default) {
                    m.applyToBitmap(src)
                }
                val oldMasked = maskedBitmap
                maskedBitmap = newMasked
                if (oldMasked != null && oldMasked !== sourceBitmap) {
                    oldMasked.recycle()
                }
                markDirty()
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
                markDirty()
                onDone()
            } catch (e: Exception) {
                error = e.message ?: "生成图纸失败"
                onDone()
            }
        }
    }

    /**
     * 保存当前项目到本地存储
     * IO 在后台线程, 状态写回在 Main 线程
     */
    fun saveProject(name: String = currentProject?.name ?: "未命名") {
        val src = sourceBitmap
        val g = grid
        if (src == null && g == null) {
            message = "没有内容可保存"
            return
        }
        viewModelScope.launch {
            try {
                val meta = currentProject?.copy(name = name) ?: ProjectMeta(name = name)
                // 磁盘 IO 在后台线程
                val saved = withContext(Dispatchers.IO) {
                    projectRepository.save(meta, src, g)
                }
                // 状态写回在 Main 线程
                currentProject = saved
                hasUnsavedChanges = false
                message = "项目已保存: ${saved.name}"
            } catch (e: Exception) {
                error = "保存失败: ${e.message}"
            }
        }
    }

    /**
     * 加载已有项目
     * IO 在后台线程, Bitmap 回收在 Main 线程 (避免 Compose 正在绘制时 recycle)
     */
    fun loadProject(projectId: Long) {
        viewModelScope.launch {
            isProcessing = true
            try {
                // 磁盘 IO + Bitmap 解码在后台线程
                val (meta, src, g) = withContext(Dispatchers.IO) {
                    val m = projectRepository.listProjects().find { it.id == projectId }
                    Triple(m, m?.let { projectRepository.loadSource(it.id) }, m?.let { projectRepository.loadGrid(it.id) })
                }
                if (meta != null && src != null) {
                    // 状态写回 + Bitmap 回收在 Main 线程
                    val oldSrc = sourceBitmap
                    val oldMasked = maskedBitmap
                    sourceBitmap = src
                    maskedBitmap = src
                    mask = null
                    grid = g
                    paletteKey = meta.paletteKey
                    useDither = meta.useDither
                    _gridSize.value = meta.gridSize
                    currentProject = meta
                    hasUnsavedChanges = false
                    if (oldMasked != null && oldMasked !== oldSrc) oldMasked.recycle()
                    oldSrc?.recycle()
                    message = "已加载: ${meta.name}"
                }
            } catch (e: Exception) {
                error = "加载失败: ${e.message}"
            } finally {
                isProcessing = false
            }
        }
    }

    /**
     * 删除项目
     * IO 在后台线程, 状态写回在 Main 线程
     */
    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                projectRepository.delete(projectId)
            }
            if (currentProject?.id == projectId) {
                currentProject = null
            }
            message = "项目已删除"
        }
    }

    /** 列出所有已保存项目 */
    suspend fun listProjects() = withContext(Dispatchers.IO) {
        projectRepository.listProjects()
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

    /** 开始新项目: 清除所有状态 */
    fun newProject() {
        processJob?.cancel()
        val oldSrc = sourceBitmap
        val oldMasked = maskedBitmap
        sourceBitmap = null
        maskedBitmap = null
        mask = null
        grid = null
        error = null
        currentProject = null
        hasUnsavedChanges = false
        paletteKey = "artkal_c"
        useDither = false
        _gridSize.value = 50
        if (oldMasked != null && oldMasked !== oldSrc) oldMasked.recycle()
        oldSrc?.recycle()
    }

    override fun onCleared() {
        processJob?.cancel()
        gridSizeDebounceJob?.cancel()
        subjectExtractor.close()
        // 注意: 不在这里 recycle Bitmap, 因为协程可能仍在后台执行
        // (cancel 是协作式的, 阻塞中的 native 调用不会立即中断)
        // ViewModel 销毁后 Bitmap 会被 GC 回收, 不需要手动 recycle
        super.onCleared()
    }
}
