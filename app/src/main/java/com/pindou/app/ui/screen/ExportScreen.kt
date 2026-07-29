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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
        }
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
        val patternBmp = remember(g) { exporter.exportPatternPng(g, cellSize = 24) }
        val usage = remember(g) { g.usageCounts() }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                bitmap = patternBmp.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val file = File(context.cacheDir, "pindou_pattern_${System.currentTimeMillis()}.png")
                        patternBmp.compress(Bitmap.CompressFormat.PNG, 100, FileOutputStream(file))
                        shareFile(context, file, "image/png")
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("分享图纸") }
                OutlinedButton(
                    onClick = {
                        val csv = exporter.exportUsageCsv(g)
                        val file = File(context.cacheDir, "pindou_usage_${System.currentTimeMillis()}.csv")
                        file.writeText(csv)
                        shareFile(context, file, "text/csv")
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("分享清单") }
            }

            Spacer(Modifier.height(24.dp))

            Text("用料清单", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    usage.forEach { (idx, count) ->
                        val c = palette.colors[idx]
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(24.dp)
                                    .background(Color.rgb(c.r, c.g, c.b))
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(c.code, modifier = Modifier.width(80.dp))
                            Text(c.name, modifier = Modifier.weight(1f))
                            Text("$count")
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
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
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mime
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "分享"))
}
