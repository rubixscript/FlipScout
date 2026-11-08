package com.rubixscript.flipscout.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.LruCache
import java.io.File

/**
 * Item photos live as small JPEGs in the app's private storage; an item only remembers the file name.
 * Picked images are downscaled once on import, and list thumbnails are decoded small and cached.
 */
object Photos {
    private const val MAX_EDGE = 1280
    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    private fun dir(context: Context) = File(context.filesDir, "photos").apply { mkdirs() }

    fun file(context: Context, name: String) = File(dir(context), name)

    /** Copies [uri] into private storage, downscaled and upright. Returns the new file name, or null on failure. Call off the main thread. */
    fun import(context: Context, uri: Uri, itemId: String): String? = runCatching {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MAX_EDGE) }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
        val rotation = resolver.openInputStream(uri)?.use { rotationDegrees(ExifInterface(it)) } ?: 0f
        val upright = if (rotation == 0f) decoded else
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, Matrix().apply { postRotate(rotation) }, true)
        val name = "$itemId-${System.currentTimeMillis()}.jpg"
        file(context, name).outputStream().use { upright.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        name
    }.getOrNull()

    /** A cached thumbnail whose longest edge is about [targetPx]. Call off the main thread. */
    fun thumb(context: Context, name: String, targetPx: Int): Bitmap? {
        val key = "$name@$targetPx"
        cache.get(key)?.let { return it }
        val path = file(context, name).absolutePath
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, targetPx) }
        return BitmapFactory.decodeFile(path, opts)?.also { cache.put(key, it) }
    }

    private fun rotationDegrees(exif: ExifInterface): Float =
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
}

/** Largest power-of-two sample size that keeps the longest edge at or above [target]. */
internal fun sampleSize(width: Int, height: Int, target: Int): Int {
    var size = 1
    val longest = maxOf(width, height)
    while (longest / (size * 2) >= target) size *= 2
    return size
}
