package io.github.glassbeat.update

import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

/** Official repository sources only; a transport 404 does not mean there are no releases. */
internal class ReleaseFeed(private val fetch: suspend (String) -> String) {
    companion object {
        const val LATEST = "https://api.github.com/repos/winner798/GlassBeat/releases/latest"
        const val LIST = "https://api.github.com/repos/winner798/GlassBeat/releases?per_page=10"
        const val MANIFEST = "https://raw.githubusercontent.com/winner798/GlassBeat/main/updates/stable.json"
        fun parseManifest(text: String): ReleaseInfo {
            val json = JSONObject(text)
            require(json.getString("packageName") == "io.github.glassbeat") { "更新清单包名无效" }
            val version = json.getString("version")
            require(ReleaseVersion.parse(version) != null) { "更新清单版本无效" }
            val digest = json.getString("sha256")
            require(digest.matches(Regex("[0-9a-fA-F]{64}"))) { "更新清单摘要无效" }
            val size = json.getLong("size")
            require(size in 1..100L * 1024 * 1024) { "更新文件大小无效" }
            val values = json.getJSONArray("downloadUrls")
            val urls = (0 until values.length()).map { values.getString(it) }.distinct()
            require(urls.isNotEmpty() && urls.all(ReleaseInfo::validAssetUrl)) { "更新清单来源无效" }
            return ReleaseInfo(version, json.optString("notes").take(12000), urls.first(), size, digest, urls)
        }
    }
    suspend fun latest(installedVersion: String): ReleaseInfo {
        val installed = ReleaseVersion.parse(installedVersion.substringBefore('-'))
        val candidates = mutableListOf<ReleaseInfo>()
        val failures = mutableListOf<String>()
        for ((name, url) in listOf("最新版本接口" to LATEST, "版本列表" to LIST, "仓库清单" to MANIFEST)) {
            try {
                val text = fetch(url)
                val found = when (url) {
                    LATEST -> listOf(ReleaseInfo.fromJson(text))
                    LIST -> JSONArray(text).let { array -> (0 until array.length()).mapNotNull { index ->
                        runCatching { ReleaseInfo.fromJson(array.getJSONObject(index).toString()) }.getOrNull()
                    } }
                    else -> listOf(parseManifest(text))
                }
                candidates += found
                val best = candidates.maxByOrNull { ReleaseVersion.parse(it.version)!! }
                // A newer valid release is enough; if this endpoint is stale, check the next one.
                if (best != null && installed != null && ReleaseVersion.parse(best.version)!! > installed) return best
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { failures += "$name：${e.message ?: "无法访问"}" }
        }
        return candidates.maxByOrNull { ReleaseVersion.parse(it.version)!! }
            ?: error("未能获取更新信息，不能据此判断仓库没有新版。请检查网络后重试，或打开发布页。\n${failures.joinToString("\n")}")
    }
}
