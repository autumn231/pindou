package com.pindou.app.ui.screen

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pindou.app.domain.model.ProjectMeta
import com.pindou.app.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    vm: MainViewModel,
    onBack: () -> Unit,
    onProjectLoaded: () -> Unit = {}
) {
    var projects by remember { mutableStateOf<List<ProjectMeta>>(emptyList()) }
    var deleteTarget by remember { mutableStateOf<ProjectMeta?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }

    // 加载项目列表 (refreshTrigger 变化时重新加载)
    LaunchedEffect(refreshTrigger) {
        projects = vm.listProjects()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的项目") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        if (projects.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("还没有保存的项目", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "在编辑界面点击保存即可创建项目",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(projects, key = { it.id }) { meta ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        vm.loadProject(meta.id)
                        onProjectLoaded()
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 缩略图
                        var thumb by remember(meta.id) { mutableStateOf<android.graphics.Bitmap?>(null) }
                        LaunchedEffect(meta.id) {
                            thumb = vm.projectRepository.loadThumbnail(meta.id)
                        }
                        val t = thumb
                        if (t != null) {
                            Image(
                                bitmap = t.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Folder, contentDescription = null)
                            }
                        }

                        Spacer(Modifier.size(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                meta.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                .format(Date(meta.updatedAt))
                            Text(
                                "$dateStr · ${meta.gridSize}格",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { deleteTarget = meta }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除")
                        }
                    }
                }
            }
        }
    }

    // 删除确认对话框
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除项目") },
            text = { Text("确定要删除 \"${target.name}\" 吗? 此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteProject(target.id)
                    deleteTarget = null
                    refreshTrigger++ // 触发列表刷新
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            }
        )
    }
}
