package io.github.glassbeat

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.glassbeat.data.*
import io.github.glassbeat.importer.AudioExtractor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class LibraryIntegrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: LibraryDatabase
    @Before fun open() { db = Room.inMemoryDatabaseBuilder(context, LibraryDatabase::class.java).build() }
    @After fun close() { db.close() }
    @Test fun playlistCascadeAndFavoriteSurviveMutations() = runBlocking {
        val dao = db.library()
        dao.save(Song("a", "First", "/music/a"))
        dao.save(Song("b", "Second", "/music/b"))
        dao.save(Playlist("p", "Favorites"))
        dao.add(PlaylistSong("p", "a")); dao.add(PlaylistSong("p", "b"))
        dao.favorite("a"); dao.rename("a", "Renamed")
        assertEquals("Renamed", dao.song("a")!!.title)
        assertTrue(dao.song("a")!!.favorite)
        dao.delete("a")
        assertEquals(listOf(PlaylistSong("p", "b")), dao.memberships().first())
        dao.deletePlaylist("p")
        assertTrue(dao.memberships().first().isEmpty())
        assertNotNull(dao.song("b"))
    }
    @Test fun legacyJsonMigrationIsIdempotentAndPreservesAudio() = runBlocking {
        val repo = LibraryRepository(context)
        val file = File(context.filesDir, "legacy-test-audio").apply { writeText("fixture") }
        val index = File(context.filesDir, "library.json")
        index.writeText("""[{"id":"legacy-test","title":"Old song","path":"${file.absolutePath}"}]""")
        try {
            repo.migrateLegacy()
            repo.dao.rename("legacy-test", "Edited after migration")
            repo.migrateLegacy()
            assertEquals("Edited after migration", repo.dao.song("legacy-test")!!.title)
            assertTrue(file.exists())
            assertTrue(File(context.filesDir, "library.json.migrated").exists())
        } finally { repo.dao.delete("legacy-test"); file.delete(); File(context.filesDir, "library.json.migrated").delete() }
    }
    @Test fun themeSettingsPersistAcrossStoreInstances() = runBlocking {
        val store = ThemeStore(context)
        val original = store.settings.first()
        try {
            val expected = original.copy(light = true, accent = 0xFF74CFC8, opacity = .3f, blur = 9f, lowPower = true)
            store.save(expected)
            assertEquals(expected, ThemeStore(context).settings.first())
        } finally { store.save(original) }
    }
    @Test fun videoAudioExtractionProducesPlayableAudioOnlyFile() = runBlocking {
        val input = File(context.cacheDir, "fixture.mp4")
        val output = File(context.cacheDir, "extracted.m4a")
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().context.assets.open("test-video.mp4").use { source -> input.outputStream().use { source.copyTo(it) } }
        output.delete()
        try {
            AudioExtractor(context).extract(Uri.fromFile(input), output) {}
            val retriever = android.media.MediaMetadataRetriever()
            try {
                retriever.setDataSource(output.absolutePath)
                assertEquals("yes", retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO))
                assertNull(retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO))
                assertTrue(retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong() > 500)
            } finally { retriever.release() }
            val repo = LibraryRepository(context)
            val cover = io.github.glassbeat.importer.VideoArtwork(context).save(input)
            assertNotNull(cover)
            val song = repo.commitAudio(output, "Extracted test", "设备测试", cover!!.path)
            assertEquals(cover.path, song.cover)
            val coverBitmap = android.graphics.BitmapFactory.decodeFile(song.cover)
            assertNotNull(coverBitmap)
            assertTrue(maxOf(coverBitmap.width, coverBitmap.height) <= 720)
            coverBitmap.recycle()
            val exported = File(context.cacheDir, "exported-test.m4a")
            try {
                repo.export(song, Uri.fromFile(exported))
                assertArrayEquals(output.readBytes(), exported.readBytes())
                repo.delete(song)
                assertFalse(output.exists())
                assertFalse(cover.exists())
                assertTrue(input.exists())
            } finally { repo.dao.delete(song.id); exported.delete() }
        } finally { input.delete(); output.delete() }
    }
}
