package io.github.glassbeat

import io.github.glassbeat.importer.MediaNetwork
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.nio.file.Files

class MediaRedirectTest {
    private class Response(url: String, private val status: Int, private val location: String? = null, private val body: ByteArray = byteArrayOf()) : HttpURLConnection(URL(url)) {
        override fun connect() {}
        override fun disconnect() {}
        override fun usingProxy() = false
        override fun getResponseCode() = status
        override fun getHeaderField(name: String?) = if (name == "Location") location else null
        override fun getHeaderFields(): Map<String, List<String>> = emptyMap()
        override fun getContentLengthLong() = body.size.toLong()
        override fun getInputStream() = ByteArrayInputStream(body)
    }
    @Test fun followsNewRegionalCdnRedirectAndPreservesDownloadedBytes() = runBlocking {
        val source = "https://new-origin.example.com/video"
        val edge = "https://regional-new-cdn.example.net/file"
        val bytes = ByteArray(100000) { (it % 251).toByte() }
        val requested = mutableListOf<String>()
        val resolved = mutableListOf<String>()
        val network = MediaNetwork(resolveAddresses = { resolved += it; listOf(InetAddress.getByName("1.1.1.1")) }, openConnection = {
            requested += it
            when (it) { source -> Response(it, 302, edge); edge -> Response(it, 200, body = bytes); else -> error("unexpected URL") }
        })
        val directory = Files.createTempDirectory("media-test").toFile()
        val output = File(directory, "video")
        try {
            network.download(source, output) {}
            assertArrayEquals(bytes, output.readBytes())
            assertEquals(listOf(source, edge), requested)
            assertEquals(listOf("new-origin.example.com", "regional-new-cdn.example.net"), resolved)
        } finally { output.delete(); directory.delete() }
    }
    @Test fun blocksRedirectToLocalNetworkBeforeOpeningConnection() = runBlocking {
        val source = "https://public-cdn.example.com/video"
        val requested = mutableListOf<String>()
        val network = MediaNetwork(resolveAddresses = { listOf(InetAddress.getByName(if (it == "127.0.0.1") it else "1.1.1.1")) }, openConnection = {
            requested += it
            Response(it, 302, "https://127.0.0.1/private")
        })
        val directory = Files.createTempDirectory("media-local-test").toFile()
        val output = File(directory, "video")
        try {
            try { network.download(source, output) {}; fail("must reject local redirect") }
            catch (e: IllegalArgumentException) { assertTrue(e.message!!.contains("本机或局域网")) }
            assertEquals(listOf(source), requested)
            assertFalse(output.exists())
        } finally { output.delete(); directory.delete() }
    }
}
