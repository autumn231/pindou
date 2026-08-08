package com.pindou.app.ui.screen

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.pindou.app.ui.MainViewModel
import com.pindou.app.util.Exporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val grid by vm::grid
    val isProcessing by vm::isProcessing
    val error by vm::error
    val paletteKey by vm::paletteKey
    val useDither by vm::useDither
    val gridSize by vm::gridSize
    val maskedBitmap by vm::maskedBitmap

    var showSaveDialog by remember { mutableStateOf(false) }
    var projectName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("调整图纸") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // 保存项目
                    IconButton(onClick = {
                        projectName = vm.currentProject?.name ?: ""
                        showSaveDialog = true
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "保存")
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
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                grid?.let { g ->
                    val exporter = remember { Exporter(vm.paletteRegistry) }
                    // 异步生成预览 Bitmap
                    var previewBmp by remember { mutableStateOf<Bitmap?>(null) }
                    LaunchedEffect(g) {
                        previewBmp = withContext(Dispatchers.Default) {
                            exporter.exportPatternPng(g, cellSize = 8)
                        }
                    }
                    DisposableEffect(previewBmp) {
                        val bmp = previewBmp
                        onDispose { bmp?.recycle() }
                    }
                    val bmp = previewBmp
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        CircularProgressIndicator()
                    }
                } ?: Text("点击下方\"生成图纸\"", style = MaterialTheme.typography.bodyLarge)
            }

            Spacer(Modifier.height(8.dp))

            // 图纸统计信息
            grid?.let { g ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text("${g.width}×${g.height}", style = MaterialTheme.typography.labelMedium)
                    Text("${g.colorCount} 种颜色", style = MaterialTheme.typography.labelMedium)
                    Text("${g.totalBeads} 颗", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(8.dp))
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }

            var paletteMenuExpanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(
                    onClick = { paletteMenuExpanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("调色板: ${vm.paletteRegistry.displayName(paletteKey)}")
                }
                DropdownMenu(
                    expanded = paletteMenuExpanded,
                    onDismissRequest = { paletteMenuExpanded = false }
                ) {
                    vm.paletteRegistry.availablePalettes.forEach { (key, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                vm.setPalette(key)
                                paletteMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text("网格尺寸: $gridSize")
            Slider(
                value = gridSize.toFloat(),
                onValueChange = { vm.setGridSize(it.toInt()) },
                valueRange = 10f..200f,
                steps = 18
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Floyd-Steinberg 抖动", modifier = Modifier.weight(1f))
                Switch(checked = useDither, onCheckedChange = { vm.toggleDither() })
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { vm.generateGrid() },
                    enabled = !isProcessing && maskedBitmap != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (grid != null) "重新生成" else "生成图纸")
                }
                Button(
                    onClick = onNext,
                    enabled = !isProcessing && grid != null,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("下一步")
                }
            }

            if (isProcessing) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }

    // 保存项目对话框
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("保存项目") },
            text = {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("项目名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = projectName.trim().ifEmpty { "未命名" }
                    vm.saveProject(name)
                    showSaveDialog = false
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("取消") }
            }
        )
    }
}
