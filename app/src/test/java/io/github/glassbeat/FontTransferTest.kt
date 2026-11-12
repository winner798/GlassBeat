package io.github.glassbeat

import io.github.glassbeat.fonts.FontTransfer
import kotlinx.coroutines.*
import org.junit.Test
import org.junit.Assert.*
import java.net.*
import java.io.*
import java.util.concurrent.*

class FontTransferTest {
    @Test fun successfulTransferPreservesFile() = runBlocking {
        val connection = object : HttpURLConnection(URL("https://example.com/font")) {
            override fun connect() {}
            override fun usingProxy() = false
            override fun disconnect() {}
            override fun getResponseCode() = 200
            override fun getContentLengthLong() = 3L
            override fun getInputStream() = ByteArrayInputStream(byteArrayOf(1, 2, 3))
        }
        val file = File.createTempFile("font-success", ".tmp")
        try {
            FontTransfer.download("https://example.com/font", file, 3, {}, { connection })
            assertArrayEquals(byteArrayOf(1, 2, 3), file.readBytes())
        } finally { file.delete() }
    }

    @Test fun cancelDisconnectsBlockedRead() = runBlocking {
        val entered = CountDownLatch(1)
        val disconnected = CountDownLatch(1)
        val connection = object : HttpURLConnection(URL("https://example.com/font")) {
            override fun connect() {}
            override fun usingProxy() = false
            override fun disconnect() { disconnected.countDown() }
            override fun getResponseCode() = 200
            override fun getContentLengthLong() = -1L
            override fun getInputStream() = object : InputStream() {
                override fun read(): Int { entered.countDown(); disconnected.await(5, TimeUnit.SECONDS); return -1 }
            }
        }
        val file = File.createTempFile("font-cancel", ".tmp")
        try {
            val job = launch(Dispatchers.Default) { FontTransfer.download("https://example.com/font", file, 100, {}, { connection }) }
            assertTrue(withContext(Dispatchers.IO) { entered.await(3, TimeUnit.SECONDS) })
            withTimeout(1000) { job.cancelAndJoin() }
            assertTrue(withContext(Dispatchers.IO) { disconnected.await(1, TimeUnit.SECONDS) })
        } finally { file.delete() }
    }
}
