package com.pindou.app.domain.palette

import android.content.Context
import com.pindou.app.domain.model.Palette
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.IOException

/**
 * 调色板注册表: 从 assets/palettes 目录加载色卡
 */
class PaletteRegistry(private val context: Context) {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(Palette::class.java)

    private val cache = HashMap<String, Palette>()

    val availablePalettes: List<Pair<String, String>> = listOf(
        "artkal_c" to "Artkal C (2.6mm)",
        "artkal_s" to "Artkal S (5mm)",
        "mard" to "MARD (5mm)"
    )

    fun displayName(key: String): String =
        availablePalettes.firstOrNull { it.first == key }?.second ?: key

    fun load(key: String): Palette {
        cache[key]?.let { return it }
        val assetPath = "palettes/$key.json"
        val json = try {
            context.assets.open(assetPath).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            throw IllegalStateException("调色板不存在: $assetPath", e)
        }
        val palette = adapter.fromJson(json)
            ?: throw IllegalStateException("调色板解析失败: $assetPath")
        cache[key] = palette
        return palette
    }
}
