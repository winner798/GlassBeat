package io.github.glassbeat.importer

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.URLDecoder

data class ImportPreview(val title: String, val durationMs: Long, val mediaUrl: String, val sourceUrl: String)

/** Parses only public response data. It never executes scripts or attempts login challenges. */
object DouyinPageParser {
    fun sharedUrl(text: String): String {
        val urls = Regex("https?://[^\\s<>\"，。！？]+", RegexOption.IGNORE_CASE).findAll(text).map { it.value.trimEnd(')', '）', ']', '】', ';', '；') }
        return urls.firstOrNull { runCatching { isDouyin(URI(it).host.orEmpty()) }.getOrDefault(false) }
            ?.let { "https://" + it.substringAfter("://") }
            ?: throw IllegalArgumentException("未找到有效抖音链接，请粘贴包含 https:// 的完整分享内容。")
    }
    fun isDouyin(host: String): Boolean {
        val h = host.lowercase()
        return listOf("douyin.com", "iesdouyin.com").any { h == it || h.endsWith(".$it") }
    }
    fun parse(body: String, source: String): ImportPreview? {
        val candidates = mutableListOf<String>()
        if (body.trimStart().startsWith("{")) candidates += body
        Regex("<script[^>]*id=[\"']RENDER_DATA[\"'][^>]*>(.*?)</script>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
            .find(body)?.groupValues?.get(1)?.let { candidates += URLDecoder.decode(it, "UTF-8") }
        listOf("_ROUTER_DATA", "__INITIAL_STATE__", "SIGI_STATE").forEach { marker ->
            val start = body.indexOf(marker)
            if (start >= 0) balancedObject(body, body.indexOf('{', start)).let { if (it != null) candidates += it }
        }
        for (candidate in candidates) {
            val root = runCatching { JSONObject(candidate) }.getOrNull() ?: continue
            findVideo(root, 0)?.let { item ->
                val video = item.optJSONObject("video") ?: return@let
                val addr = video.optJSONObject("play_addr") ?: video.optJSONObject("playAddr") ?: video.optJSONObject("playApi")
                val urls = addr?.optJSONArray("url_list") ?: addr?.optJSONArray("urlList")
                val raw = urls?.optString(0)?.takeIf { it.isNotBlank() }
                    ?: video.optString("playAddr").takeIf { it.startsWith("http") }
                val media = raw?.replace("http://", "https://") ?: return@let
                if (runCatching { URI(media).scheme == "https" && !URI(media).host.isNullOrBlank() }.getOrDefault(false)) {
                    return ImportPreview(item.optString("desc").ifBlank { item.optString("description", "抖音视频音轨") }.take(200),
                        video.optLong("duration", item.optLong("duration", 0)), media, source)
                }
            }
        }
        return null
    }
    private fun findVideo(node: Any?, depth: Int): JSONObject? {
        if (depth > 24) return null
        if (node is JSONObject) {
            val video = node.optJSONObject("video")
            if (video != null && (video.has("play_addr") || video.has("playAddr") || video.has("playApi"))) return node
            node.keys().forEach { key -> findVideo(node.opt(key), depth + 1)?.let { return it } }
        } else if (node is JSONArray) {
            for (i in 0 until node.length()) findVideo(node.opt(i), depth + 1)?.let { return it }
        }
        return null
    }
    private fun balancedObject(text: String, start: Int): String? {
        if (start < 0) return null
        var depth = 0; var string = false; var escape = false
        for (i in start until text.length) {
            val c = text[i]
            if (string) { if (escape) escape = false else if (c == '\\') escape = true else if (c == '"') string = false }
            else when (c) { '"' -> string = true; '{' -> depth++; '}' -> { depth--; if (depth == 0) return text.substring(start, i + 1) } }
        }
        return null
    }
}
class DouyinResolver(private val network: MediaNetwork = MediaNetwork()) {
    suspend fun resolve(text: String): ImportPreview = withContext(Dispatchers.IO) {
        val shared = DouyinPageParser.sharedUrl(text)
        val page = network.page(shared)
        DouyinPageParser.parse(page.body, page.url)?.let { return@withContext it }
        val id = Regex("/(?:video|share/video)/(\\d+)").find(page.url)?.groupValues?.get(1)
            ?: Regex("(?:modal_id|aweme_id)=(\\d+)").find(page.url)?.groupValues?.get(1)
        if (id != null) {
            val alternatives = listOf("https://www.iesdouyin.com/share/video/$id/", "https://www.douyin.com/aweme/v1/web/aweme/detail/?aweme_id=$id")
            for (url in alternatives) {
                val result = try { network.page(url) } catch (e: CancellationException) { throw e } catch (_: Exception) { continue }
                DouyinPageParser.parse(result.body, page.url)?.let { return@withContext it }
            }
        }
        error("抖音未返回可访问的媒体资源，可能需要登录、验证或内容已失效。请保存有权使用的视频后，选择“本地视频提取”。")
    }
}
