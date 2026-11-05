package io.github.glassbeat.importer

import io.github.glassbeat.data.copyCancellable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

class MediaNetwork {
    data class Page(val body: String, val url: String)
    // Cookies are short-lived, held in memory for this import only, and scoped by host.
    private val cookies = mutableMapOf<String, String>()
    private fun connect(url: String, page: Boolean): HttpURLConnection {
        var current = url
        repeat(7) {
            val uri = URI(current)
            require(uri.scheme == "https" && uri.userInfo == null && (uri.port == -1 || uri.port == 443)) { "仅支持安全 HTTPS 链接" }
            val host = uri.host?.lowercase() ?: error("无效链接")
            require(if (page) DouyinPageParser.isDouyin(host) else isAllowedMediaHost(host)) { "不支持的媒体地址" }
            val connection = URL(current).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15000; connection.readTimeout = 20000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            connection.setRequestProperty("Referer", "https://www.douyin.com/")
            cookies[host]?.let { connection.setRequestProperty("Cookie", it) }
            val code = connection.responseCode
            connection.headerFields.filterKeys { it?.equals("Set-Cookie", true) == true }.values.flatten().takeIf { it.isNotEmpty() }?.let {
                cookies[host] = it.joinToString("; ") { cookie -> cookie.substringBefore(';') }
            }
            if (code in 300..399) {
                val next = connection.getHeaderField("Location")
                connection.disconnect()
                require(!next.isNullOrBlank()) { "分享链接跳转失败" }
                current = URI(current).resolve(next).toString()
            } else {
                if (code !in 200..299) { connection.disconnect(); error("网络请求失败（HTTP $code），可稍后重试或导入本地视频。") }
                return connection
            }
        }
        error("分享链接跳转次数过多")
    }
    internal fun isAllowedMediaHost(host: String) = listOf("douyin.com", "iesdouyin.com", "douyinvod.com", "zjcdn.com", "bytecdn.cn", "byteimg.com", "snssdk.com", "pstatp.com", "ibytedtos.com", "bytecdntp.com").any { host == it || host.endsWith(".$it") }
    suspend fun page(url: String): Page = withContext(Dispatchers.IO) {
        val connection = connect(url, true)
        try {
            val bytes = ByteArrayOutputStream()
            connection.inputStream.use { copyCancellable(it, bytes, 8L * 1024 * 1024) }
            Page(bytes.toString("UTF-8"), connection.url.toString())
        } finally { connection.disconnect() }
    }
    suspend fun download(url: String, target: File, progress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        val connection = connect(url, false)
        try {
            val total = connection.contentLengthLong
            require(total <= 2L * 1024 * 1024 * 1024) { "媒体文件过大，请改用本地视频" }
            connection.inputStream.use { input -> target.outputStream().use { output ->
                copyCancellable(input, output, 2L * 1024 * 1024 * 1024) { bytes -> progress(if (total > 0) bytes.toFloat() / total else -1f) }
            } }
        } catch (e: Exception) { target.delete(); throw e }
        finally { connection.disconnect() }
    }
}
