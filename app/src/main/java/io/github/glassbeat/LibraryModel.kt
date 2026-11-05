package io.github.glassbeat

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class Song(val id: String, val title: String, val path: String)

class LibraryModel(app: Application) : AndroidViewModel(app) {
    private val index = File(app.filesDir, "library.json")
    var songs by mutableStateOf<List<Song>>(emptyList()); private set
    var message by mutableStateOf<String?>(null)
    var importing by mutableStateOf(false); private set
    private val prefs = app.getSharedPreferences("theme", 0)
    var light by mutableStateOf(prefs.getBoolean("light", false)); private set
    init {
        viewModelScope.launch {
            try {
                songs = withContext(Dispatchers.IO) {
                    if (!index.exists()) emptyList() else JSONArray(index.readText()).let { a ->
                        (0 until a.length()).map { a.getJSONObject(it) }.map {
                            Song(it.getString("id"), it.getString("title"), it.getString("path"))
                        }.filter { File(it.path).exists() }
                    }
                }
            } catch (_: Exception) { message = "音乐库读取失败，请保留应用数据并反馈问题。" }
        }
    }
    fun toggleTheme() { light = !light; prefs.edit().putBoolean("light", light).apply() }
    fun receive(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND) {
            message = "已接收分享。当前版本尚未接入抖音链接解析，请先将有权保存的音频导出，再使用本地导入。"
        }
    }
    fun import(uris: List<Uri>) {
        if (importing || uris.isEmpty()) return
        importing = true
        viewModelScope.launch {
            var failures = 0
            val imported = withContext(Dispatchers.IO) {
                uris.mapNotNull { uri ->
                    var target: File? = null
                    try {
                        val app = getApplication<Application>()
                        val resolver = app.contentResolver
                        val name = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                            if (it.moveToFirst()) it.getString(0) else null
                        } ?: "未命名音频"
                        val id = UUID.randomUUID().toString()
                        val dir = File(app.filesDir, "music").apply { mkdirs() }
                        val destination = File(dir, id)
                        target = destination
                        val input = resolver.openInputStream(uri) ?: error("Cannot read file")
                        input.use { source -> destination.outputStream().use { source.copyTo(it) } }
                        require(destination.length() > 0)
                        Song(id, name.substringBeforeLast('.'), destination.absolutePath)
                    } catch (_: Exception) { target?.delete(); failures++; null }
                }
            }
            val next = songs + imported
            try {
                withContext(Dispatchers.IO) {
                    val array = JSONArray()
                    next.forEach { array.put(JSONObject().put("id", it.id).put("title", it.title).put("path", it.path)) }
                    val temp = File(index.parentFile, "library.tmp")
                    temp.writeText(array.toString())
                    check(temp.renameTo(index))
                }
                songs = next
                message = "已导入 ${imported.size} 首" + if (failures > 0) "，${failures} 个文件失败" else ""
            } catch (_: Exception) {
                withContext(Dispatchers.IO) { imported.forEach { File(it.path).delete() } }
                message = "保存失败，请检查存储空间。"
            } finally { importing = false }
        }
    }
}
