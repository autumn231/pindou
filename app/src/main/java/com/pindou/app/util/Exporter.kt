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
        val w = grid.width * cellSize
        val h = grid.height * cellSize
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
                    x * cellSize.toFloat(),
                    y * cellSize.toFloat(),
                    (x + 1) * cellSize.toFloat(),
                    (y + 1) * cellSize.toFloat()
                )
                if (idx >= 0) {
                    val c = palette.colors[idx]
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
            val c = palette.colors[idx]
            sb.append("${c.code},${c.name},$count,${c.r}-${c.g}-${c.b}\n")
        }
        sb.append("\n总数,${grid.colorIndices.count { it >= 0 }},,\n")
        return sb.toString()
    }
}
