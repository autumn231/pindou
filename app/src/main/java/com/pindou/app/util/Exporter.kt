package com.pindou.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.pindou.app.domain.model.PixelGrid
import com.pindou.app.domain.palette.PaletteRegistry

/** 导出图纸 PNG 和用量清单 CSV */
class Exporter(private val paletteRegistry: PaletteRegistry) {

    companion object {
        // 限制输出 PNG 最大边长, 防止 OOM (200 格 * 24px = 4800px, 约 92MB ARGB_8888)
        private const val MAX_OUTPUT_DIM = 4096
    }

    /** 导出图纸 PNG: 每格一个色块, 带网格线, 透明格画棋盘格 */
    fun exportPatternPng(grid: PixelGrid, cellSize: Int = 16): Bitmap {
        val palette = paletteRegistry.load(grid.paletteKey)
        // 动态调整 cellSize, 确保输出尺寸不超过上限
        val maxCells = maxOf(grid.width, grid.height)
        val effectiveCellSize = if (maxCells * cellSize > MAX_OUTPUT_DIM) {
            MAX_OUTPUT_DIM / maxCells
        } else {
            cellSize
        }.coerceAtLeast(1)

        val w = grid.width * effectiveCellSize
        val h = grid.height * effectiveCellSize
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)

        val fillPaint = Paint().apply { style = Paint.Style.FILL }
        val linePaint = Paint().apply {
            style = Paint.Style.STROKE
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        for (y in 0 until grid.height) {
            for (x in 0 until grid.width) {
                val idx = grid.get(x, y)
                val rectF = RectF(
                    x * effectiveCellSize.toFloat(),
                    y * effectiveCellSize.toFloat(),
                    (x + 1) * effectiveCellSize.toFloat(),
                    (y + 1) * effectiveCellSize.toFloat()
                )
                if (idx >= 0) {
                    // 安全获取颜色: 索引越界时跳过
                    val c = palette.colors.getOrNull(idx) ?: continue
                    fillPaint.color = Color.rgb(c.r, c.g, c.b)
                    canvas.drawRect(rectF, fillPaint)
                } else {
                    fillPaint.color = if ((x + y) % 2 == 0) Color.parseColor("#F5F5F5") else Color.WHITE
                    canvas.drawRect(rectF, fillPaint)
                }
                canvas.drawRect(rectF, linePaint)
            }
        }
        return bmp
    }

    /** 导出用料清单 CSV */
    fun exportUsageCsv(grid: PixelGrid): String {
        val palette = paletteRegistry.load(grid.paletteKey)
        val sb = StringBuilder()
        sb.append("色号,颜色名,数量,RGB\n")
        for ((idx, count) in grid.usageCounts()) {
            // 安全获取颜色: 索引越界时跳过
            val c = palette.colors.getOrNull(idx) ?: continue
            // CSV 转义: 含逗号/引号/换行的字段需用双引号包裹并转义内部引号
            sb.append("${csvEscape(c.code)},${csvEscape(c.name)},$count,${c.r}-${c.g}-${c.b}\n")
        }
        sb.append("\n总数,${grid.colorIndices.count { it >= 0 }},,\n")
        return sb.toString()
    }

    private fun csvEscape(field: String): String {
        return if (field.contains(',') || field.contains('"') || field.contains('\n')) {
            "\"${field.replace("\"", "\"\"")}\""
        } else {
            field
        }
    }
}
