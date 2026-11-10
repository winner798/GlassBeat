package io.github.glassbeat.importer

import io.github.glassbeat.data.copyCancellable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URI
import java.net.URL

class MediaNetwork(
    private val resolveAddresses: (String) -> List<InetAddress> = { InetAddress.getAllByName(it).toList() },
    private val openConnection: (String) -> HttpURLConnection = { URL(it).openConnection() as HttpURLConnection }
) {
    data class Page(val body: String, val url: String)
    // Cookies are short-lived, held in memory for this import only, and scoped by host.
    private val cookies = mutableMapOf<String, String>()
    private fun connect(url: String, page: Boolean): HttpURLConnection {
        var current = url
        repeat(7) {
            val uri = URI(current)
            require(uri.scheme == "https" && uri.userInfo == null && (uri.port == -1 || uri.port == 443)) { "仅支持安全 HTTPS 链接" }
            val host = uri.host?.lowercase() ?: error("无效链接")
            if (page) require(DouyinPageParser.isDouyin(host)) { "不支持的分享页面：$host" }
            else requireRemoteMediaHost(host)
            val connection = openConnection(current)
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
    // Media URLs originate from the validated official page/API. CDN names vary by region,
    // so validate destinations rather than maintaining an inevitably incomplete vendor list.
    internal fun requireRemoteMediaHost(host: String) {
        val addresses = resolveAddresses(host)
        require(addresses.isNotEmpty() && addresses.all(::isRemoteAddress)) {
            "媒体地址指向本机或局域网，无法下载（$host）"
        }
    }
    internal fun isRemoteAddress(address: InetAddress): Boolean {
        if (address.isAnyLocalAddress || address.isLoopbackAddress || address.isLinkLocalAddress ||
            address.isSiteLocalAddress || address.isMulticastAddress) return false
        val bytes = address.address.map { it.toInt() and 255 }
        if (bytes.size == 4) return bytes[0] != 0 && bytes[0] < 224 &&
            !(bytes[0] == 100 && bytes[1] in 64..127)
        if (bytes.size == 16) {
            if ((bytes[0] and 0xfe) == 0xfc) return false // IPv6 unique-local addresses
            if (bytes.take(12).all { it == 0 }) return false // IPv4-compatible/unspecified
        }
        return true
    }
    suspend fun page(url: String): Page = withContext(Dispatchers.IO) {
        val connection = connect(url, true)
        try {
            val bytes = ByteArrayOutputStream()
            connection.inputStream.use { copyCancellable(it, bytes, 8L * 1024 * 1024) }
            Page(bytes.toString("UTF-8"), connection.url.toString())
        } finally { connection.disconnect() }
    }
    suspend fun download(url: String, target: File, maxBytes: Long = 2L * 1024 * 1024 * 1024, progress: (Float) -> Unit) = withContext(Dispatchers.IO) {
        val connection = connect(url, false)
        try {
            val total = connection.contentLengthLong
            require(total <= maxBytes) { "媒体文件过大，请改用本地视频" }
            connection.inputStream.use { input -> target.outputStream().use { output ->
                copyCancellable(input, output, maxBytes) { bytes -> progress(if (total > 0) bytes.toFloat() / total else -1f) }
            } }
        } catch (e: Exception) { target.delete(); throw e }
        finally { connection.disconnect() }
    }
}
