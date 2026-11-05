package io.github.glassbeat

import android.app.Application
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.runtime.*
import androidx.core.content.IntentCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.glassbeat.data.*
import io.github.glassbeat.importer.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.UUID

data class ImportTask(val label: String, val progress: Float = -1f)
class LibraryModel(app: Application) : AndroidViewModel(app) {
    val updater = io.github.glassbeat.update.AppUpdater(app, viewModelScope)
    val repository = LibraryRepository(app)
    private val themes = ThemeStore(app)
    val songs = repository.dao.songs().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val playlists = repository.dao.playlists().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val memberships = repository.dao.memberships().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val theme = themes.settings.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeSettings())
    var message by mutableStateOf<String?>(null)
    var preview by mutableStateOf<ImportPreview?>(null)
    var sharedText by mutableStateOf("")
    private val _task = MutableStateFlow<ImportTask?>(null)
    val task = _task.asStateFlow()
    private var importJob: Job? = null
    private val ready = CompletableDeferred<Unit>()
    private val mutation = Mutex()
    init {
        viewModelScope.launch {
            try { repository.migrateLegacy(); ready.complete(Unit) }
            catch (e: Exception) { ready.completeExceptionally(e); message = "旧版音乐库迁移失败，原文件已保留：${e.message}" }
        }
    }
    private fun action(block: suspend () -> Unit) = viewModelScope.launch {
        try { ready.await(); mutation.withLock { block() } }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { message = e.message ?: "操作失败，请重试" }
    }
    fun updateTheme(update: (ThemeSettings) -> ThemeSettings) = action {
        val current = themes.settings.first()
        val next = update(current)
        themes.save(next)
        if (current.background.isNotBlank() && current.background != next.background)
            withContext(Dispatchers.IO) { File(current.background).delete() }
    }
    fun favorite(song: Song) = action { repository.dao.favorite(song.id) }
    fun rename(song: Song, title: String) = action {
        require(title.isNotBlank()) { "歌曲名不能为空" }
        repository.dao.rename(song.id, title.trim().take(200))
    }
    fun delete(song: Song) = action { repository.delete(song); message = "已删除应用内副本，原始文件未受影响。" }
    fun createPlaylist(name: String) = action {
        require(name.isNotBlank()) { "歌单名不能为空" }
        repository.dao.save(Playlist(UUID.randomUUID().toString(), name.trim().take(80)))
    }
    fun renamePlaylist(playlist: Playlist, name: String) = action {
        require(name.isNotBlank()) { "歌单名不能为空" }
        repository.dao.save(playlist.copy(name = name.trim().take(80)))
    }
    fun deletePlaylist(playlist: Playlist) = action { repository.dao.deletePlaylist(playlist.id) }
    fun setMembership(song: Song, playlist: Playlist, enabled: Boolean) = action {
        val entry = PlaylistSong(playlist.id, song.id)
        if (enabled) repository.dao.add(entry) else repository.dao.remove(entry)
    }
    fun export(song: Song, target: Uri) = action { repository.export(song, target); message = "音乐已导出" }
    fun setArtwork(uri: Uri, song: Song? = null) = action {
        val folder = File(getApplication<Application>().filesDir, if (song == null) "backgrounds" else "covers").apply { mkdirs() }
        val target = File(folder, "${UUID.randomUUID()}.image")
        try {
            repository.copyUri(uri, target, 20L * 1024 * 1024)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(target.absolutePath, bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "无法识别图片，请选择 JPG、PNG 或 WebP" }
            val old = if (song == null) themes.settings.first().background else repository.dao.song(song.id)?.cover.orEmpty()
            if (song == null) themes.save(themes.settings.first().copy(background = target.absolutePath))
            else repository.dao.cover(song.id, target.absolutePath)
            if (old.isNotBlank()) withContext(Dispatchers.IO) { File(old).delete() }
        } catch (e: Exception) { target.delete(); throw e }
    }
    fun setFont(uri: Uri) = action {
        val target = File(File(getApplication<Application>().filesDir, "fonts").apply { mkdirs() }, "${UUID.randomUUID()}.ttf")
        try {
            repository.copyUri(uri, target, 10L * 1024 * 1024)
            withContext(Dispatchers.IO) { Typeface.createFromFile(target) }
            val old = themes.settings.first().font
            themes.save(themes.settings.first().copy(font = target.absolutePath))
            if (old.isNotBlank()) withContext(Dispatchers.IO) { File(old).delete() }
        } catch (e: Exception) { target.delete(); throw IllegalArgumentException("字体导入失败，请选择有效的 TTF 或 OTF 文件", e) }
    }
    fun clearBackground() = action {
        val old = themes.settings.first().background; themes.save(themes.settings.first().copy(background = ""))
        if (old.isNotBlank()) withContext(Dispatchers.IO) { File(old).delete() }
    }
    fun clearFont() = action {
        val old = themes.settings.first().font; themes.save(themes.settings.first().copy(font = ""))
        if (old.isNotBlank()) withContext(Dispatchers.IO) { File(old).delete() }
    }
    fun receive(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                if (uri != null && uri.scheme == "content") import(listOf(uri), intent.type?.startsWith("video/") == true)
                else intent.getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() }?.let { resolve(it) }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java).orEmpty().filter { it.scheme == "content" }
                import(uris, intent.type?.startsWith("video/") == true)
            }
        }
    }
    fun cancelImport() { importJob?.cancel() }
    private fun startTask(block: suspend () -> Unit) {
        if (importJob?.isActive == true) { message = "请先等待当前任务完成，或取消任务。"; return }
        importJob = viewModelScope.launch {
            try { ready.await(); block() }
            catch (e: CancellationException) { message = "导入已取消，已完成的歌曲仍保留。"; throw e }
            catch (e: Exception) { message = e.message ?: "导入失败，请检查文件和存储空间。" }
            finally { _task.value = null }
        }
    }
    fun resolve(text: String) {
        sharedText = text
        startTask {
            preview = null
            _task.value = ImportTask("正在解析抖音分享…")
            try { preview = DouyinResolver().resolve(text) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { throw IllegalStateException("抖音解析失败：${e.message ?: "网络不可用"}\n可重试，或使用本地视频提取。", e) }
        }
    }
    fun import(uris: List<Uri>, video: Boolean = false) {
        if (uris.isEmpty()) return
        startTask {
            var completed = 0
            val failures = mutableListOf<String>()
            for ((i, uri) in uris.withIndex()) {
                _task.value = ImportTask("导入 ${i + 1}/${uris.size}")
                try {
                    val isVideo = video || getApplication<Application>().contentResolver.getType(uri)?.startsWith("video/") == true
                    if (isVideo) importVideo(uri, withContext(Dispatchers.IO) { repository.displayName(uri) }, "本地视频")
                    else repository.importAudio(uri)
                    completed++
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { failures += (e.message ?: "文件不兼容") }
            }
            message = "已导入 $completed 首" + if (failures.isNotEmpty()) "，${failures.size} 个失败：${failures.first()}" else ""
        }
    }
    private suspend fun importVideo(uri: Uri, name: String, source: String) {
        val app = getApplication<Application>()
        val input = File(app.cacheDir, "video-${UUID.randomUUID()}.tmp")
        val output = repository.newAudioFile("m4a")
        try {
            _task.value = ImportTask("正在读取视频…")
            repository.copyUri(uri, input)
            extractAndSave(input, output, name, source)
        } catch (e: Exception) { repository.discardIfUnreferenced(output); throw e }
        finally { input.delete() }
    }
    private suspend fun extractAndSave(input: File, output: File, name: String, source: String) {
        _task.value = ImportTask("正在提取音轨…", 0f)
        AudioExtractor(getApplication()).extract(Uri.fromFile(input), output) { _task.value = ImportTask("正在提取音轨…", it / 100f) }
        repository.commitAudio(output, name, source)
    }
    fun savePreview() {
        val item = preview ?: return
        startTask {
            val input = File(getApplication<Application>().cacheDir, "download-${UUID.randomUUID()}.mp4")
            val output = repository.newAudioFile("m4a")
            try {
                _task.value = ImportTask("正在下载视频音轨…")
                MediaNetwork().download(item.mediaUrl, input) { _task.value = ImportTask("正在下载视频…", it) }
                extractAndSave(input, output, item.title, "抖音分享")
                preview = null; message = "音轨已保存到音乐库"
            } catch (e: Exception) { repository.discardIfUnreferenced(output); throw e }
            finally { input.delete() }
        }
    }
}
