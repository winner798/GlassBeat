package io.github.glassbeat.update

import org.json.JSONObject
import java.net.URI

data class ReleaseVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<ReleaseVersion> {
    override fun compareTo(other: ReleaseVersion) = compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })
    companion object {
        fun parse(value: String): ReleaseVersion? {
            val match = Regex("^v?(\\d+)\\.(\\d+)\\.(\\d+)$").matchEntire(value.trim()) ?: return null
            val parts = match.groupValues.drop(1).map { it.toIntOrNull() ?: return null }
            return ReleaseVersion(parts[0], parts[1], parts[2])
        }
    }
}
data class ReleaseInfo(val version: String, val notes: String, val url: String, val size: Long, val sha256: String?) {
    companion object {
        const val RELEASES = "https://github.com/winner798/GlassBeat/releases"
        fun fromJson(text: String): ReleaseInfo {
            val root = JSONObject(text)
            require(!root.optBoolean("draft") && !root.optBoolean("prerelease")) { "没有可用的正式版本" }
            val version = root.getString("tag_name")
            require(ReleaseVersion.parse(version) != null) { "发布版本号格式无效" }
            val assets = root.getJSONArray("assets")
            val candidates = (0 until assets.length()).map { assets.getJSONObject(it) }.filter {
                val name = it.optString("name")
                name.endsWith(".apk", true) && !name.contains("debug", true) && it.optLong("size") in 1..100L * 1024 * 1024
            }
            val asset = candidates.firstOrNull { it.optString("name").startsWith("GlassBeat-", true) } ?: candidates.singleOrNull()
                ?: error("此版本尚未提供可安装的正式 APK")
            val url = asset.optString("url").takeIf { it.isNotBlank() } ?: asset.getString("browser_download_url")
            require(validAssetUrl(url)) { "更新文件不是来自 GlassBeat 发布仓库" }
            val digest = asset.optString("digest").removePrefix("sha256:").takeIf { it.matches(Regex("[0-9a-fA-F]{64}")) }
            return ReleaseInfo(version.removePrefix("v"), root.optString("body").take(12000), url, asset.getLong("size"), digest)
        }
        fun validAssetUrl(url: String): Boolean = runCatching {
            val uri = URI(url)
            uri.scheme == "https" && uri.userInfo == null && (uri.port == -1 || uri.port == 443) && (
                (uri.host == "github.com" && uri.normalize().path.startsWith("/winner798/GlassBeat/releases/download/") && uri.path.endsWith(".apk", true)) ||
                (uri.host == "api.github.com" && uri.path.matches(Regex("/repos/winner798/GlassBeat/releases/assets/\\d+"))))
        }.getOrDefault(false)
    }
}
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class Latest(val message: String) : UpdateState
    data class Available(val release: ReleaseInfo) : UpdateState
    data class Downloading(val release: ReleaseInfo, val progress: Float) : UpdateState
    data class Ready(val release: ReleaseInfo, val path: String) : UpdateState
    data class Failed(val message: String) : UpdateState
}
