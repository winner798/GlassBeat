package io.github.glassbeat

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.glassbeat.importer.VideoArtwork
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class VideoArtworkTest {
    @Test fun rejectedPosterFallsBackToDownloadedVideo() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        val input = File(context.cacheDir, "poster-fallback.mp4")
        InstrumentationRegistry.getInstrumentation().context.assets.open("test-video.mp4").use { source -> input.outputStream().use { source.copyTo(it) } }
        var cover: File? = null
        try {
            cover = VideoArtwork(context).save(input, "https://127.0.0.1/poster.jpg")
            assertNotNull(cover)
            assertTrue(cover!!.length() > 0)
        } finally { input.delete(); cover?.delete() }
    }
    @Test fun brokenVideoDoesNotLeaveCoverOrBlockAudioFallback() = runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        val folder = File(context.filesDir, "covers").apply { mkdirs() }
        val before = folder.list()?.toSet().orEmpty()
        val input = File(context.cacheDir, "broken-cover-video.mp4").apply { writeText("broken") }
        try { assertNull(VideoArtwork(context).save(input)); assertEquals(before, folder.list()?.toSet().orEmpty()) }
        finally { input.delete() }
    }
}
