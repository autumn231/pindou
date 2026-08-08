package com.pindou.app.ui.screen

import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.pindou.app.ui.MainViewModel
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtractScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val sourceBitmap by vm::sourceBitmap
    val maskedBitmap by vm::maskedBitmap
    val isProcessing by vm::isProcessing
    val error by vm::error
    val isAutoAvailable by vm::isAutoAvailable

    var displaySize by remember { mutableStateOf(IntSize.Zero) }
    var dragStart by remember { mutableStateOf<Offset?>(null) }
    var dragEnd by remember { mutableStateOf<Offset?>(null) }
    var manualRect by remember { mutableStateOf<Rect?>(null) }

    // 图像增强弹窗
    var showEnhanceSheet by remember { mutableStateOf(false) }
    var claheStrength by remember { mutableStateOf(2.0f) }
    var gammaValue by remember { mutableStateOf(1.0f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("提取主体") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 图像增强
                    IconButton(onClick = { showEnhanceSheet = true }) {
                        Icon(Icons.Default.Tune, contentDescription = "图像增强")
                    }
                    // 旋转原图
                    IconButton(onClick = {
                        vm.rotateSource()
                        manualRect = null
                        dragStart = null
                        dragEnd = null
                    }) {
                        Icon(Icons.Default.RotateRight, contentDescription = "旋转90度")
                    }
                    // 清除蒙版, 恢复原图
                    if (maskedBitmap !== sourceBitmap) {
                        IconButton(onClick = {
                            vm.clearMask()
                            manualRect = null
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "清除蒙版")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onSizeChanged { displaySize = it }
            ) {
                val bmp = maskedBitmap ?: sourceBitmap
                bmp?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    dragStart = offset
                                    dragEnd = offset
                                },
                                onDragEnd = {
                                    val s = dragStart; val e = dragEnd
                                    if (s != null && e != null && displaySize != IntSize.Zero) {
                                        val src = sourceBitmap
                                        if (src != null) {
                                            manualRect = mapToBitmapCoords(
                                                s, e, displaySize, src.width, src.height
                                            )
                                        }
                                    }
                                },
                                onDrag = { change, _ ->
                                    dragEnd = change.position
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val s = dragStart; val e = dragEnd
                        if (s != null && e != null) {
                            drawRect(
                                color = Color(0xFFB4E6D2),
                                topLeft = Offset(min(s.x, e.x), min(s.y, e.y)),
                                size = Size(abs(e.x - s.x), abs(e.y - s.y)),
                                style = Stroke(width = 4f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
            }

            manualRect?.let { rect ->
                Text("已选框: ${rect.width()}x${rect.height()}", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isAutoAvailable) {
                    Button(
                        onClick = { vm.extractAuto { ok -> if (ok) onNext() } },
                        enabled = !isProcessing,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("自动抠图")
                    }
                }
                OutlinedButton(
                    onClick = {
                        manualRect?.let { rect ->
                            vm.extractManual(rect) { ok -> if (ok) onNext() }
                        }
                    },
                    enabled = !isProcessing && manualRect != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("手动 GrabCut")
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = onSkip) {
                    Icon(Icons.Default.SkipNext, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("跳过提取")
                }
                if (maskedBitmap != null) {
                    Button(onClick = onNext, enabled = !isProcessing) {
                        Text("下一步")
                    }
                }
            }

            if (isProcessing) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(8.dp))
            if (maskedBitmap !== sourceBitmap) {
                Text(
                    "已提取主体 (背景已透明)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    "提示: 在图上拖拽画矩形框选主体, 然后点手动 GrabCut",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // 图像增强底部弹窗
    if (showEnhanceSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showEnhanceSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "图像增强",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    "改善光线不均、偏色、过暗/过亮等问题",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(16.dp))

                if (isProcessing) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    // 一键自动增强
                    Button(
                        onClick = { vm.enhanceAuto() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("一键自动增强")
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))

                    // 白平衡
                    OutlinedButton(
                        onClick = { vm.enhanceWhiteBalance() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.InvertColors, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("白平衡 (消除色偏)")
                    }

                    Spacer(Modifier.height(16.dp))

                    // CLAHE 对比度增强
                    Text("对比度增强 (CLAHE)", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "强度: ${"%.1f".format(claheStrength)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = claheStrength,
                        onValueChange = { claheStrength = it },
                        valueRange = 1.0f..5.0f,
                        steps = 0
                    )
                    OutlinedButton(
                        onClick = { vm.enhanceCLAHE(claheStrength.toDouble()) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("应用对比度增强")
                    }

                    Spacer(Modifier.height(16.dp))

                    // Gamma 亮度校正
                    Text("亮度校正 (Gamma)", style = MaterialTheme.typography.titleMedium)
                    Text(
                        when {
                            gammaValue < 0.9f -> "提亮 (${ "%.2f".format(gammaValue) })"
                            gammaValue > 1.1f -> "压暗 (${ "%.2f".format(gammaValue) })"
                            else -> "不变 (1.00)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = gammaValue,
                        onValueChange = { gammaValue = it },
                        valueRange = 0.3f..2.0f
                    )
                    OutlinedButton(
                        onClick = { vm.enhanceGamma(gammaValue.toDouble()) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.WbSunny, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("应用亮度校正")
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(
                        "提示: 增强后需重新提取主体",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(16.dp))
                TextButton(
                    onClick = { showEnhanceSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("关闭")
                }
            }
        }
    }
}

private fun mapToBitmapCoords(
    start: Offset, end: Offset, displaySize: IntSize,
    bmpW: Int, bmpH: Int
): Rect {
    val dispW = displaySize.width.toFloat()
    val dispH = displaySize.height.toFloat()
    val scale = min(dispW / bmpW, dispH / bmpH)
    val drawnW = bmpW * scale
    val drawnH = bmpH * scale
    val offsetX = (dispW - drawnW) / 2
    val offsetY = (dispH - drawnH) / 2

    val left = ((min(start.x, end.x) - offsetX) / scale).toInt().coerceIn(0, bmpW - 1)
    val top = ((min(start.y, end.y) - offsetY) / scale).toInt().coerceIn(0, bmpH - 1)
    val right = ((max(start.x, end.x) - offsetX) / scale).toInt().coerceIn(left + 1, bmpW)
    val bottom = ((max(start.y, end.y) - offsetY) / scale).toInt().coerceIn(top + 1, bmpH)

    return Rect(left, top, right, bottom)
}
