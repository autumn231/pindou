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

    /** 导出图纸 PNG: 每格一个色块, 带网格线, 透明格画棋盘格 */
    fun exportPatternPng(grid: PixelGrid, cellSize: Int = 16): Bitmap {
        val palette = paletteRegistry.load(grid.paletteKey)
        // 钳制输出尺寸, 避免大网格 OOM
        val safeCell = cellSize.coerceAtMost(
            (MAX_OUTPUT_PIXELS / (grid.width.toLong() * grid.height)).toInt().coerceAtLeast(1)
        )
        val w = grid.width * safeCell
        val h = grid.height * safeCell
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)

        val fillPaint = Paint().apply { style = Paint.Style.FILL }
        val linePaint = Paint().apply {
            style = Paint.Style.STROKE
            color = Color.LTGRAY
            strokeWidth = 1f
        }
        val rect = RectF()
        val colors = palette.colors
        val lastIdx = colors.lastIndex

        for (y in 0 until grid.height) {
            for (x in 0 until grid.width) {
                val idx = grid.get(x, y)
                rect.set(
                    x * safeCell.toFloat(),
                    y * safeCell.toFloat(),
                    (x + 1) * safeCell.toFloat(),
                    (y + 1) * safeCell.toFloat()
                )
                if (idx >= 0 && idx <= lastIdx) {
                    val c = colors[idx]
                    fillPaint.color = Color.rgb(c.r, c.g, c.b)
                } else {
                    fillPaint.color = if ((x + y) % 2 == 0) LIGHT_CHECKER else Color.WHITE
                }
                canvas.drawRect(rect, fillPaint)
                canvas.drawRect(rect, linePaint)
            }
        }
        return bmp
    }

    /** 导出用料清单 CSV */
    fun exportUsageCsv(grid: PixelGrid): String {
        val palette = paletteRegistry.load(grid.paletteKey)
        val colors = palette.colors
        val lastIdx = colors.lastIndex
        val sb = StringBuilder()
        sb.append("\uFEFF色号,颜色名,数量,RGB\n")
        for ((idx, count) in grid.usageCounts()) {
            if (idx !in 0..lastIdx) continue
            val c = colors[idx]
            sb.append("${csv(c.code)},${csv(c.name)},$count,${c.r}-${c.g}-${c.b}\n")
        }
        sb.append("\n总数,${grid.colorIndices.count { it >= 0 }},,\n")
        return sb.toString()
    }

    private fun csv(field: String): String {
        // CSV 转义: 含逗号/引号/换行则用双引号包裹, 内部引号双写
        return if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else field
    }

    companion object {
        private const val MAX_OUTPUT_PIXELS = 4_000_000
        private val LIGHT_CHECKER = Color.parseColor("#F5F5F5")
    }
}
