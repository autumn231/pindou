package com.pindou.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.pindou.app.domain.model.PixelGrid
import com.pindou.app.domain.model.ProjectMeta
import com.pindou.app.util.scaledToMax
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.io.FileOutputStream

/**
 * 项目持久化: 保存/加载/列出/删除项目
 * 存储位置: app filesDir/projects/
 * 每个项目包含: meta.json (元数据) + source.png (原图) + grid.json (网格数据)
 */
class ProjectRepository(context: Context) {

    private val rootDir = File(context.filesDir, "projects").apply { if (!exists()) mkdirs() }
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val metaAdapter = moshi.adapter(ProjectMeta::class.java)
    private val gridAdapter = moshi.adapter(PixelGrid::class.java)

    /** 列出所有已保存项目 (按修改时间降序) */
    fun listProjects(): List<ProjectMeta> {
        return rootDir.listFiles { f -> f.isDirectory }?.mapNotNull { dir ->
            val metaFile = File(dir, "meta.json")
            if (!metaFile.exists()) return@mapNotNull null
            try {
                metaAdapter.fromJson(metaFile.readText())?.copy(id = dir.name.toLongOrNull() ?: 0L)
            } catch (e: Exception) {
                null
            }
        }?.sortedByDescending { it.updatedAt } ?: emptyList()
    }

    /** 保存项目 (原图 + 网格 + 元数据) */
    fun save(meta: ProjectMeta, source: Bitmap?, grid: PixelGrid?): ProjectMeta {
        val projectId = meta.id
        val dir = File(rootDir, projectId.toString()).apply { if (!exists()) mkdirs() }

        // 保存原图
        if (source != null) {
            val srcFile = File(dir, "source.png")
            FileOutputStream(srcFile).use { source.compress(Bitmap.CompressFormat.PNG, 90, it) }
        }

        // 保存网格 (grid 为 null 时删除旧的 grid.json, 避免加载到过期数据)
        val gridFile = File(dir, "grid.json")
        if (grid != null) {
            gridFile.writeText(gridAdapter.toJson(grid))
        } else {
            gridFile.delete()
        }

        // 保存元数据
        val updatedMeta = meta.copy(updatedAt = System.currentTimeMillis())
        File(dir, "meta.json").writeText(metaAdapter.toJson(updatedMeta))
        return updatedMeta
    }

    /** 加载项目原图 */
    fun loadSource(projectId: Long): Bitmap? {
        val srcFile = File(rootDir, "$projectId/source.png")
        if (!srcFile.exists()) return null
        return BitmapFactory.decodeFile(srcFile.absolutePath)
    }

    /** 加载项目网格 */
    fun loadGrid(projectId: Long): PixelGrid? {
        val gridFile = File(rootDir, "$projectId/grid.json")
        if (!gridFile.exists()) return null
        return try {
            gridAdapter.fromJson(gridFile.readText())
        } catch (e: Exception) {
            null
        }
    }

    /** 删除项目 */
    fun delete(projectId: Long) {
        File(rootDir, projectId.toString()).deleteRecursively()
    }

    /** 获取项目缩略图 (原图的压缩版, 用于列表展示), 内部切换到 IO 线程 */
    suspend fun loadThumbnail(projectId: Long): Bitmap? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val src = loadSource(projectId) ?: return@withContext null
            val thumb = src.scaledToMax(200)
            if (thumb !== src) src.recycle()
            thumb
        }
    }
}
