package io.github.glassbeat.fonts

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import kotlin.coroutines.coroutineContext

/** Publish a complete, verified font + license directory, or keep the previous cache untouched. */
class FontDownloads(
    private val root: File,
    private val transfer: suspend (String, File, Long, (Float) -> Unit) -> Unit = { url, file, limit, progress ->
        try { withTimeout(90_000) {
            try { FontTransfer.download(url, file, limit, progress) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                file.delete()
                val fallback = url.replace("https://raw.githubusercontent.com/google/fonts/", "https://cdn.jsdelivr.net/gh/google/fonts@")
                FontTransfer.download(fallback, file, limit, progress)
            }
        } } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            throw java.io.IOException("字体下载超时，请检查网络后重试", e)
        }
    },
    private val validate: (File) -> Unit
) {
    fun file(font: DownloadFont) = File(File(root, font.id), "font.ttf")
    fun contains(font: DownloadFont) = file(font).length() == font.bytes && File(root, "${font.id}/OFL.txt").isFile
    fun installed(): Set<String> = FontCatalog.fonts.filter(::contains).mapTo(mutableSetOf()) { it.id }
    fun cleanInterruptedDownloads() { root.listFiles()?.filter { it.name.startsWith(".") }?.forEach { it.deleteRecursively() } }
    fun delete(font: DownloadFont) { File(root, font.id).deleteRecursively() }

    suspend fun acquire(font: DownloadFont, progress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        if (contains(font)) {
            verify(file(font), font.sha256)
            verify(File(root, "${font.id}/OFL.txt"), font.licenseSha256)
            validate(file(font))
            coroutineContext.ensureActive()
            return@withContext file(font)
        }
        root.mkdirs()
        val staging = File(root, ".${font.id}-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            val target = File(staging, "font.ttf")
            val license = File(staging, "OFL.txt")
            transfer(font.licenseUrl, license, 64 * 1024L) { }
            verify(license, font.licenseSha256)
            transfer(font.url, target, font.bytes, progress)
            require(target.length() == font.bytes) { "字体文件不完整，请重新下载" }
            verify(target, font.sha256)
            validate(target)
            coroutineContext.ensureActive()
            val destination = File(root, font.id)
            if (destination.exists()) destination.deleteRecursively()
            check(staging.renameTo(destination)) { "无法保存字体，请检查存储空间" }
            file(font)
        } finally { staging.deleteRecursively() }
    }

    private fun verify(file: File, expected: String) {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(32 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        require(actual == expected) { "字体校验失败，请删除后重新下载" }
    }
}
