package io.github.glassbeat

import io.github.glassbeat.fonts.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.security.MessageDigest

class FontDownloadsTest {
    @get:Rule val temporary = TemporaryFolder()
    private val bytes = "test font data".toByteArray()
    private val license = "SIL OPEN FONT LICENSE".toByteArray()
    private fun digest(data: ByteArray) = MessageDigest.getInstance("SHA-256").digest(data).joinToString("") { "%02x".format(it) }
    private val font get() = DownloadFont("demo", "Demo", "Test", "demo.ttf", bytes.size.toLong(), digest(bytes), digest(license))

    @Test fun completeDownloadKeepsLicenseAndReappliesWithoutNetwork() = runBlocking {
        var requests = 0
        val store = FontDownloads(temporary.root, { url, file, limit, progress ->
            requests++
            val data = if (url.endsWith("OFL.txt")) license else bytes
            assertTrue(data.size <= limit)
            file.writeBytes(data); progress(1f)
        }, { assertArrayEquals(bytes, it.readBytes()) })
        val saved = store.acquire(font) {}
        assertEquals(2, requests)
        assertArrayEquals(license, File(saved.parentFile, "OFL.txt").readBytes())
        assertEquals(saved, store.acquire(font) {})
        assertEquals(2, requests)
        store.delete(font)
        assertFalse(store.contains(font))
    }

    @Test fun corruptedDownloadDoesNotPublishOrAffectExistingFont() = runBlocking {
        val existing = File(temporary.root, "previous.ttf").apply { writeText("previous") }
        val store = FontDownloads(temporary.root, { url, file, _, _ ->
            file.writeBytes(if (url.endsWith("OFL.txt")) license else ByteArray(bytes.size))
        }, {})
        try { store.acquire(font) {}; fail("corrupt font accepted") } catch (_: IllegalArgumentException) { }
        assertEquals("previous", existing.readText())
        assertFalse(store.contains(font))
        assertEquals(listOf("previous.ttf"), temporary.root.list()!!.toList())
    }

    @Test fun cancellationCleansPartialFilesAndPropagates() = runBlocking {
        val store = FontDownloads(temporary.root, { url, file, _, _ ->
            if (url.endsWith("OFL.txt")) file.writeBytes(license)
            else { file.writeText("partial"); throw CancellationException("cancelled") }
        }, {})
        try { store.acquire(font) {}; fail("cancellation swallowed") } catch (_: CancellationException) { }
        assertTrue(temporary.root.list()!!.isEmpty())
    }

    @Test fun invalidLicensePreventsFontDownload() = runBlocking {
        var requests = 0
        val store = FontDownloads(temporary.root, { _, file, _, _ -> requests++; file.writeText("unexpected license") }, {})
        try { store.acquire(font) {}; fail("invalid license accepted") } catch (_: IllegalArgumentException) { }
        assertEquals(1, requests)
        assertTrue(temporary.root.list()!!.isEmpty())
    }

    @Test fun platformValidationFailureDoesNotPublishCache() = runBlocking {
        val store = FontDownloads(temporary.root, { url, file, _, _ -> file.writeBytes(if (url.endsWith("OFL.txt")) license else bytes) }, { error("Invalid font") })
        try { store.acquire(font) {}; fail("invalid font accepted") } catch (_: IllegalStateException) { }
        assertTrue(temporary.root.list()!!.isEmpty())
    }
}
