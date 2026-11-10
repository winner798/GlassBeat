package io.github.glassbeat.importer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Optional artwork: prefer the video poster, then a bounded local video frame. */
class VideoArtwork(private val context: Context) {
    suspend fun save(video: File, poster: String? = null): File? = withContext(Dispatchers.IO) {
        val temporary = File(context.cacheDir, "poster-${UUID.randomUUID()}.tmp")
        val target = File(File(context.filesDir, "covers").apply { mkdirs() }, "${UUID.randomUUID()}.jpg")
        var bitmap: Bitmap? = null
        try {
            if (!poster.isNullOrBlank()) {
                try {
                    MediaNetwork().download(poster, temporary, maxBytes = 8L * 1024 * 1024) {}
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(temporary.path, bounds)
                    if (bounds.outWidth > 0 && bounds.outHeight > 0 && bounds.outWidth.toLong() * bounds.outHeight <= 16_000_000) {
                        val options = BitmapFactory.Options()
                        while (maxOf(bounds.outWidth, bounds.outHeight) / options.inSampleSize > 1440) options.inSampleSize *= 2
                        bitmap = BitmapFactory.decodeFile(temporary.path, options)
                    }
                } catch (e: CancellationException) { throw e } catch (_: Exception) { /* Use the downloaded video instead. */ }
            }
            if (bitmap == null) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(video.path)
                    bitmap = if (Build.VERSION.SDK_INT >= 27) retriever.getScaledFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 720, 720)
                    else {
                        val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toLongOrNull() ?: 0
                        val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toLongOrNull() ?: 0
                        if (width * height in 1..4_000_000) retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) else null
                    }
                } finally { retriever.release() }
            }
            val original = bitmap ?: return@withContext null
            val ratio = minOf(1f, 720f / maxOf(original.width, original.height))
            val scaled = if (ratio < 1f) Bitmap.createScaledBitmap(original, (original.width * ratio).toInt().coerceAtLeast(1), (original.height * ratio).toInt().coerceAtLeast(1), true) else original
            try { target.outputStream().use { require(scaled.compress(Bitmap.CompressFormat.JPEG, 88, it)) } }
            finally { if (scaled !== original) scaled.recycle() }
            target
        } catch (e: CancellationException) { target.delete(); throw e }
        catch (_: Exception) { target.delete(); null }
        finally { bitmap?.recycle(); temporary.delete() }
    }
}
