package io.github.glassbeat.update

import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

internal object GitHubReleaseNetwork {
    fun open(url: String, api: Boolean): HttpURLConnection {
        var current = url
        repeat(6) {
            val uri = URI(current)
            val host = uri.host.orEmpty().lowercase()
            require(uri.scheme == "https" && uri.userInfo == null && (uri.port == -1 || uri.port == 443) &&
                (if (api) (host == "api.github.com" || (host == "raw.githubusercontent.com" && uri.path == "/winner798/GlassBeat/main/updates/stable.json")) else host == "api.github.com" || host == "github.com" || host.endsWith(".githubusercontent.com"))) { "更新链接跳转到不受支持的来源" }
            val connection = URL(current).openConnection() as HttpURLConnection
            connection.useCaches = false
            connection.setRequestProperty("Cache-Control", "no-cache")
            connection.instanceFollowRedirects = false; connection.connectTimeout = 15000; connection.readTimeout = 20000
            connection.setRequestProperty("User-Agent", "GlassBeat-Android")
            connection.setRequestProperty("Accept", if (api) "application/vnd.github+json" else "application/octet-stream")
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            val code = connection.responseCode
            if (code in 300..399) {
                val next = connection.getHeaderField("Location"); connection.disconnect()
                require(!next.isNullOrBlank()) { "更新链接跳转失败" }
                current = URI(current).resolve(next).toString()
            } else {
                if (code !in 200..299) {
                    connection.disconnect()
                    error(when (code) { 404 -> "更新地址暂不可用（HTTP 404）"; 403, 429 -> "GitHub 暂时限制了请求，请稍后重试或打开发布页。"; else -> "更新请求失败（HTTP $code），请检查网络。" })
                }
                return connection
            }
        }
        error("更新下载重定向次数过多")
    }
}
