package io.github.glassbeat

import android.content.Context
import android.graphics.Typeface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.glassbeat.fonts.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest

@RunWith(AndroidJUnit4::class)
class FontDownloadTest {
    @Test fun downloadedFontLoadsOnAndroidAndCanBeReusedOffline() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        val root = File(context.cacheDir, "font-download-test")
        val bytes = context.resources.openRawResource(R.font.favsoda_bubble).use { it.readBytes() }
        val license = "fixture license".toByteArray()
        fun hash(data: ByteArray) = MessageDigest.getInstance("SHA-256").digest(data).joinToString("") { "%02x".format(it) }
        val font = DownloadFont("fixture", "Fixture", "", "font.ttf", bytes.size.toLong(), hash(bytes), hash(license))
        var requests = 0
        val store = FontDownloads(root, { url, file, _, _ -> requests++; file.writeBytes(if (url.endsWith("OFL.txt")) license else bytes) }, { Typeface.createFromFile(it) })
        try {
            val saved = store.acquire(font) {}
            assertNotNull(Typeface.createFromFile(saved))
            assertEquals(saved, store.acquire(font) {})
            assertEquals(2, requests)
            store.delete(font)
            assertFalse(saved.exists())
        } finally { root.deleteRecursively() }
    }
}
