package io.github.glassbeat.update

import io.github.glassbeat.data.copyCancellable
import kotlinx.coroutines.CancellationException
import java.io.File
import java.net.HttpURLConnection
import java.security.MessageDigest

internal class ReleaseDownloader(
    private val open: (String) -> HttpURLConnection = { GitHubReleaseNetwork.open(it, false) }
) {
    suspend fun download(release: ReleaseInfo, target: File, progress: (Float) -> Unit) {
        var lastError: Exception? = null
        for (url in release.downloadUrls) {
            try {
                require(ReleaseInfo.validAssetUrl(url)) { "更新来源无效" }
                val connection = open(url)
                try {
                    connection.inputStream.use { input -> target.outputStream().use { output ->
                        copyCancellable(input, output, 100L * 1024 * 1024) { copied -> progress((copied.toFloat() / release.size).coerceIn(0f, 1f)) }
                    } }
                } finally { connection.disconnect() }
                require(target.length() == release.size) { "更新文件下载不完整" }
                release.sha256?.let { require(sha256(target).equals(it, true)) { "更新文件校验失败" } }
                return
            } catch (e: CancellationException) { target.delete(); throw e }
            catch (e: Exception) { target.delete(); lastError = e }
        }
        error("更新下载失败，请检查网络或打开发布页。${lastError?.message.orEmpty()}")
    }
    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input -> val buffer = ByteArray(65536); while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) } }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
