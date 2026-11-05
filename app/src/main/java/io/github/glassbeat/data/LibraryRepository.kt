package io.github.glassbeat.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.room.withTransaction
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class LibraryRepository(private val context: Context) {
    val db = LibraryDatabase.get(context)
    val dao = db.library()
    suspend fun migrateLegacy() = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, "library.json")
        if (file.exists()) {
            val array = JSONArray(file.readText())
            val songs = (0 until array.length()).map { array.getJSONObject(it) }.map {
                Song(it.getString("id"), it.getString("title"), it.getString("path"))
            }.filter { File(it.path).isFile }
            db.withTransaction { dao.importLegacy(songs) }
            check(file.renameTo(File(context.filesDir, "library.json.migrated"))) { "旧音乐库备份失败" }
        }
    }
    fun newAudioFile(extension: String = "media") = File(File(context.filesDir, "music").apply { mkdirs() }, "${UUID.randomUUID()}.$extension")
    fun displayName(uri: Uri): String = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
        if (it.moveToFirst()) it.getString(0) else null
    } ?: "未命名音频"
    suspend fun copyUri(uri: Uri, target: File, maxBytes: Long = 2L * 1024 * 1024 * 1024) = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri) ?: error("无法读取文件")
        input.use { src -> target.outputStream().use { copyCancellable(src, it, maxBytes) } }
    }
    suspend fun commitAudio(file: File, fallback: String, source: String): Song = withContext(Dispatchers.IO) {
        require(file.length() > 0) { "音频文件为空" }
        val id = UUID.randomUUID().toString()
        val meta = MediaMetadataRetriever()
        var cover = ""
        try {
            meta.setDataSource(file.absolutePath)
            require(meta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes") { "文件中没有可识别的音轨" }
            val title = meta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() } ?: fallback.substringBeforeLast('.').take(200)
            val artist = meta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() } ?: "本地音乐"
            val duration = meta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0
            val mime = meta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "audio/mpeg"
            meta.embeddedPicture?.takeIf { it.size <= 12 * 1024 * 1024 }?.let { bytes ->
                val target = File(File(context.filesDir, "covers").apply { mkdirs() }, "$id.jpg")
                target.writeBytes(bytes); cover = target.absolutePath
            }
            val song = Song(id, title.ifBlank { "未命名音频" }, file.absolutePath, artist, duration, mime, cover, source = source)
            dao.save(song)
            song
        } catch (e: Exception) { withContext(NonCancellable + Dispatchers.IO) { if (cover.isNotEmpty() && dao.references(file.absolutePath) == 0) File(cover).delete() }; throw e }
        finally { meta.release() }
    }
    suspend fun importAudio(uri: Uri): Song {
        val target = newAudioFile()
        try { copyUri(uri, target); return commitAudio(target, withContext(Dispatchers.IO) { displayName(uri) }, "本地导入") }
        catch (e: Exception) { discardIfUnreferenced(target); throw e }
    }
    suspend fun discardIfUnreferenced(file: File) = withContext(NonCancellable + Dispatchers.IO) {
        if (dao.references(file.absolutePath) == 0) file.delete()
    }
    suspend fun export(song: Song, uri: Uri) = withContext(Dispatchers.IO) {
        val output = context.contentResolver.openOutputStream(uri, "w") ?: error("无法写入目标文件")
        output.use { out -> File(song.path).inputStream().use { copyCancellable(it, out) } }
    }
    suspend fun delete(song: Song) = withContext(Dispatchers.IO) {
        dao.delete(song.id)
        File(song.path).delete()
        if (song.cover.isNotEmpty()) File(song.cover).delete()
    }
}
suspend fun copyCancellable(input: InputStream, output: OutputStream, maxBytes: Long = Long.MAX_VALUE, progress: (Long) -> Unit = {}) {
    val buffer = ByteArray(64 * 1024)
    var count = 0L
    while (true) {
        currentCoroutineContext().ensureActive()
        val n = input.read(buffer)
        if (n < 0) break
        count += n
        require(count <= maxBytes) { "文件超出允许大小" }
        output.write(buffer, 0, n)
        progress(count)
    }
}
