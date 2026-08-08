package com.pindou.app.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore

/** Bitmap 工具函数 */

fun Bitmap.scaledToMax(maxDim: Int): Bitmap {
    val w = width
    val h = height
    val scale = if (w >= h) maxDim.toFloat() / w else maxDim.toFloat() / h
    if (scale >= 1f) return this
    val newW = (w * scale).toInt().coerceAtLeast(1)
    val newH = (h * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(this, newW, newH, true)
}

/** 把 mask (长度 = w*h, 0 = 透明) 应用到 source, 返回透明背景的 Bitmap */
fun IntArray.applyToBitmap(source: Bitmap): Bitmap {
    val w = source.width
    val h = source.height
    if (size != w * h) {
        throw IllegalArgumentException("mask size ${size} != bitmap ${w * h}")
    }
    val srcPixels = IntArray(w * h)
    source.getPixels(srcPixels, 0, w, 0, 0, w, h)
    val outPixels = IntArray(w * h)
    for (i in srcPixels.indices) {
        outPixels[i] = if (this[i] != 0) srcPixels[i] else 0
    }
    val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    result.setPixels(outPixels, 0, w, 0, 0, w, h)
    return result
}

/** 旋转 Bitmap (顺时针 90/180/270 度), 返回新 Bitmap */
fun Bitmap.rotate(degrees: Float): Bitmap {
    if (degrees == 0f) return this
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

/**
 * 保存 Bitmap 到 MediaStore (相册), 兼容 Android 10+ 和旧版本
 * @return 保存的 Uri, 失败返回 null
 */
fun Bitmap.saveToGallery(
    context: Context,
    displayName: String,
    mimeType: String = "image/png"
): Uri? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        saveToMediaStoreQ(context, displayName, mimeType)
    } else {
        saveToLegacy(context, displayName, mimeType)
    }
}

private fun Bitmap.saveToMediaStoreQ(
    context: Context,
    displayName: String,
    mimeType: String
): Uri? {
    val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        put(MediaStore.Images.Media.MIME_TYPE, mimeType)
        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/拼豆")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    val resolver = context.contentResolver
    val uri = resolver.insert(collection, values) ?: return null
    try {
        resolver.openOutputStream(uri)?.use { stream ->
            if (!compress(formatFromMime(mimeType), 100, stream)) return null
        }
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri
    } catch (e: Exception) {
        resolver.delete(uri, null, null)
        return null
    }
}

private fun Bitmap.saveToLegacy(
    context: Context,
    displayName: String,
    mimeType: String
): Uri? {
    val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    val subDir = java.io.File(dir, "拼豆").apply { if (!exists()) mkdirs() }
    val file = java.io.File(subDir, displayName)
    try {
        java.io.FileOutputStream(file).use { stream ->
            if (!compress(formatFromMime(mimeType), 100, stream)) return null
        }
        // 通知媒体扫描器
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(MediaStore.Images.Media.DATA, file.absolutePath)
        }
        return context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    } catch (e: Exception) {
        return null
    }
}

private fun formatFromMime(mimeType: String): Bitmap.CompressFormat =
    if (mimeType.contains("jpeg") || mimeType.contains("jpg")) {
        Bitmap.CompressFormat.JPEG
    } else {
        Bitmap.CompressFormat.PNG
    }
