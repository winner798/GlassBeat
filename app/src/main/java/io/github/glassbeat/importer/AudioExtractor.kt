package io.github.glassbeat.importer

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.*
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.io.File

@androidx.annotation.OptIn(UnstableApi::class)
class AudioExtractor(private val context: Context) {
    suspend fun extract(uri: Uri, output: File, progress: (Int) -> Unit) = withContext(Dispatchers.Main.immediate) {
        // Transformer is created, used and cancelled on its application looper.
        var transformer: Transformer? = null
        try {
            coroutineScope {
                val monitor = launch {
                    while (isActive) {
                        val holder = ProgressHolder()
                        if (transformer?.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) progress(holder.progress)
                        delay(300)
                    }
                }
                try {
                    suspendCancellableCoroutine<Unit> { cont ->
                        val t = Transformer.Builder(context).setAudioMimeType(MimeTypes.AUDIO_AAC)
                            .addListener(object : Transformer.Listener {
                                override fun onCompleted(composition: Composition, result: ExportResult) {
                                    if (cont.isActive) cont.resume(Unit)
                                }
                                override fun onError(composition: Composition, result: ExportResult, exception: ExportException) {
                                    if (cont.isActive) cont.resumeWithException(IllegalStateException("无法提取音轨：视频可能无音频、格式不兼容或空间不足", exception))
                                }
                            }).build()
                        transformer = t
                        cont.invokeOnCancellation { Handler(Looper.getMainLooper()).post { t.cancel() } }
                        try { t.start(EditedMediaItem.Builder(MediaItem.fromUri(uri)).setRemoveVideo(true).build(), output.absolutePath) }
                        catch (e: Exception) { if (cont.isActive) cont.resumeWithException(e) }
                    }
                } finally { monitor.cancel() }
            }
        } catch (e: Exception) { transformer?.cancel(); output.delete(); throw e }
    }
}
