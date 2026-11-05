package io.github.glassbeat

import io.github.glassbeat.update.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.security.MessageDigest

class ReleaseDownloaderTest {
    private val api = "https://api.github.com/repos/winner798/GlassBeat/releases/assets/123"
    private val raw = "https://raw.githubusercontent.com/winner798/GlassBeat/release-artifacts/v1.0.3/dist/GlassBeat-v1.0.3.apk"
    private class Response(url: String, private val bytes: ByteArray) : HttpURLConnection(URL(url)) {
        override fun connect() {}
        override fun disconnect() {}
        override fun usingProxy() = false
        override fun getInputStream() = ByteArrayInputStream(bytes)
    }
    @Test fun failedAssetApiFallsBackToVerifiedRawDownload() = runBlocking {
        val bytes = "verified APK fixture".toByteArray()
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        val release = ReleaseInfo("1.0.3", "", api, bytes.size.toLong(), hash, listOf(api, raw))
        val calls = mutableListOf<String>()
        val downloader = ReleaseDownloader { url -> calls += url; if (url == api) error("HTTP 404") else Response(url, bytes) }
        val dir = Files.createTempDirectory("update-test").toFile(); val file = File(dir, "update.apk")
        try {
            downloader.download(release, file) {}
            assertEquals(listOf(api, raw), calls)
            assertArrayEquals(bytes, file.readBytes())
        } finally { file.delete(); dir.delete() }
    }
    @Test fun corruptFallbackCannotPassChecksumAndLeavesNoPartialFile() = runBlocking {
        val bytes = "wrong binary".toByteArray()
        val release = ReleaseInfo("1.0.3", "", raw, bytes.size.toLong(), "0".repeat(64))
        val downloader = ReleaseDownloader { Response(it, bytes) }
        val dir = Files.createTempDirectory("update-bad-test").toFile(); val file = File(dir, "update.apk")
        try {
            try { downloader.download(release, file) {}; fail("must reject corrupt update") }
            catch (e: IllegalStateException) { assertTrue(e.message!!.contains("校验失败")) }
            assertFalse(file.exists())
        } finally { file.delete(); dir.delete() }
    }
}
