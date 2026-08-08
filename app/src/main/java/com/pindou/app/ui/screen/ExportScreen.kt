package com.pindou.app.ui.screen

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.pindou.app.ui.MainViewModel
import com.pindou.app.util.Exporter
import com.pindou.app.util.saveToGallery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    vm: MainViewModel,
    onBack: () -> Unit
) {
    val grid = vm.grid
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("导出") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val g = grid
        if (g == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("没有图纸, 请先生成")
            }
            return@Scaffold
        }

        val palette = remember(g.paletteKey) { vm.paletteRegistry.load(g.paletteKey) }
        val exporter = remember { Exporter(vm.paletteRegistry) }

        // 异步生成预览 Bitmap
        var patternBmp by remember { mutableStateOf<Bitmap?>(null) }
        LaunchedEffect(g) {
            patternBmp = withContext(Dispatchers.Default) {
                exporter.exportPatternPng(g, cellSize = 24)
            }
        }
        DisposableEffect(patternBmp) {
            val bmp = patternBmp
            onDispose { bmp?.recycle() }
        }

        val usage = remember(g) { g.usageCounts() }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 图纸统计
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${g.width}×${g.height}", style = MaterialTheme.typography.titleMedium)
                    Text("尺寸", style = MaterialTheme.typography.labelSmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${g.colorCount}", style = MaterialTheme.typography.titleMedium)
                    Text("颜色", style = MaterialTheme.typography.labelSmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${g.totalBeads}", style = MaterialTheme.typography.titleMedium)
                    Text("总颗数", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(Modifier.height(16.dp))

            // 预览图
            val bmp = patternBmp
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            Spacer(Modifier.height(16.dp))

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 保存到相册
                Button(
                    onClick = {
                        val bmp2 = patternBmp ?: return@Button
                        scope.launch {
                            val uri = withContext(Dispatchers.IO) {
                                bmp2.saveToGallery(
                                    context,
                                    "pindou_${System.currentTimeMillis()}.png"
                                )
                            }
                            if (uri != null) {
                                snackbarHostState.showSnackbar("已保存到相册")
                            } else {
                                snackbarHostState.showSnackbar("保存失败")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("保存到相册")
                }
                // 分享图纸 (PNG 压缩在后台线程)
                OutlinedButton(
                    onClick = {
                        val bmp2 = patternBmp ?: return@OutlinedButton
                        scope.launch {
                            val file = withContext(Dispatchers.IO) {
                                File(context.cacheDir, "pindou_pattern_${System.currentTimeMillis()}.png").also { f ->
                                    FileOutputStream(f).use { bmp2.compress(Bitmap.CompressFormat.PNG, 100, it) }
                                }
                            }
                            shareFile(context, file, "image/png")
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("分享图纸")
                }
            }

            Spacer(Modifier.height(8.dp))

            // 分享用料清单 (CSV 写入在后台线程)
            OutlinedButton(
                onClick = {
                    scope.launch {
                        val file = withContext(Dispatchers.IO) {
                            val csv = exporter.exportUsageCsv(g)
                            File(context.cacheDir, "pindou_usage_${System.currentTimeMillis()}.csv").also { f ->
                                f.writeText(csv)
                            }
                        }
                        shareFile(context, file, "text/csv")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("分享用料清单 (CSV)") }

            Spacer(Modifier.height(24.dp))

            // 用料清单
            Text("用料清单", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    usage.forEach { (idx, count) ->
                        val c = palette.colors.getOrNull(idx) ?: return@forEach
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(24.dp)
                                    .background(Color(c.r, c.g, c.b))
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(c.code, modifier = Modifier.width(80.dp))
                            Text(c.name, modifier = Modifier.weight(1f))
                            Text("$count 颗")
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Spacer(Modifier.weight(1f))
                        Text("共 ${usage.sumOf { it.second }} 颗", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private fun shareFile(context: Context, file: File, mime: String) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "分享"))
    } catch (e: Exception) {
        // 无可处理分享的 App 时不崩溃
    }
}
