package io.github.glassbeat

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.chrisbanes.haze.HazeState
import io.github.glassbeat.data.Song
import io.github.glassbeat.data.ThemeSettings
import io.github.glassbeat.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class OverlayUiTest {
    @get:Rule val compose = createComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private fun content(body: @Composable () -> Unit) = compose.setContent {
        val haze = remember { HazeState() }
        val appearance = ThemeSettings(motion = false)
        MaterialTheme(darkColorScheme(primary = Color(0xFF9EB8FF), surface = Color(0xFF151B29), background = Color(0xFF0B1020))) {
            CompositionLocalProvider(LocalGlass provides haze, LocalAppearance provides appearance) {
                Box { GlassBackground(appearance, haze); body() }
            }
        }
    }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        // PixelCopy of a Compose dialog can select the activity window. Capture the
        // compositor instead, and wait until the dark fixture contains rendered text.
        compose.mainClock.advanceTimeBy(500)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.waitForIdleSync()
        var captured: Bitmap? = null
        compose.waitUntil(5000) {
            val raw = instrumentation.uiAutomation.takeScreenshot()
            if (raw == null) false else {
                val frame = raw.copy(Bitmap.Config.ARGB_8888, false)
                raw.recycle()
                val pixels = IntArray(frame.width * frame.height)
                frame.getPixels(pixels, 0, frame.width, 0, 0, frame.width, frame.height)
                var bright = 0
                // Exclude status/navigation bars; the plain fixture has no bright text.
                for (y in (frame.height * .15f).toInt() until (frame.height * .90f).toInt() step 3) {
                    for (x in (frame.width * .05f).toInt() until (frame.width * .95f).toInt() step 3) {
                        val color = pixels[y * frame.width + x]
                        if ((color shr 16 and 255) > 160 && (color shr 8 and 255) > 160 && (color and 255) > 160) bright++
                    }
                }
                if (bright > 60) { captured = frame; true } else { frame.recycle(); false }
            }
        }
        val bitmap = checkNotNull(captured)
        val directory = File(context.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Test fun buttonsOnlyActivateOnCompletedEnabledPresses() {
        var clicks = 0
        var enabled by mutableStateOf(true)
        content { SoftButton(onClick = { clicks++ }, enabled = enabled) { androidx.compose.material3.Text("触感按钮") } }
        val button = compose.onNodeWithText("触感按钮")
        button.performTouchInput { down(center) }
        compose.runOnIdle { assertEquals(0, clicks) }
        button.performTouchInput { cancel() }
        compose.runOnIdle { assertEquals(0, clicks) }
        button.performTouchInput { click(center) }
        compose.runOnIdle { assertEquals(1, clicks); enabled = false }
        button.performTouchInput { click(center) }
        compose.runOnIdle { assertEquals(1, clicks) }
    }
    @Test fun importOffersDistinctSourcesAndPreviewsEnteredLink() {
        var audio = 0; var video = 0; var resolved = ""
        content { ImportSheet("", false, {}, { audio++ }, { video++ }, { resolved = it }) }
        compose.onNodeWithText("解析并预览").assertIsNotEnabled()
        compose.onNodeWithText("导入本地音频").performClick()
        compose.onNodeWithText("本地视频提取音轨").performClick()
        compose.runOnIdle { assertEquals(1, audio); assertEquals(1, video) }
        screenshot("import-sheet")
        compose.onNode(hasSetTextAction()).performTextInput("https://v.douyin.com/example/")
        compose.onNodeWithText("解析并预览").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("https://v.douyin.com/example/", resolved) }
    }
    @Test fun queueHighlightsCurrentItemAndRemovesOnlyQueueEntry() {
        lateinit var player: ExoPlayer
        val songs = listOf(
            Song("one", "道不尽离愁，一首留给夜晚的歌", "/unused-one.mp3", "收藏的声音", 285000),
            Song("two", "Coastline", "/unused-two.mp3", "Duett", 228000),
            Song("three", "First Light", "/unused-three.mp3", "FavSoda", 192000))
        compose.runOnUiThread { player = ExoPlayer.Builder(context).build(); player.setMediaItems(songs.map { it.mediaItem() }, 0, 0) }
        try {
            content { QueueSheet(player) {} }
            compose.onNodeWithText("3 首音乐 · 顺序播放").assertIsDisplayed()
            compose.onNodeWithContentDescription("当前歌曲已暂停").assertIsDisplayed()
            compose.onNodeWithText("收藏的声音 · 4:45").assertIsDisplayed()
            screenshot("queue-sheet")
            compose.onNodeWithContentDescription("移出队列：Coastline").performClick()
            compose.waitUntil(3000) { compose.onAllNodesWithText("2 首音乐 · 顺序播放").fetchSemanticsNodes().isNotEmpty() }
            compose.runOnIdle { assertEquals(2, player.mediaItemCount); assertEquals("one", player.currentMediaItem?.mediaId) }
            compose.onNodeWithText("First Light").performClick()
            compose.runOnIdle { assertEquals("three", player.currentMediaItem?.mediaId); assertTrue(player.playWhenReady) }
        } finally { compose.runOnUiThread { player.release() } }
    }
    @Test fun songOptionsKeepLongTitleSeparateFromActions() {
        var action = ""
        val song = Song("one", "道不尽离愁。#红尘客栈 #直播录屏 #翻唱", "/unused.mp3", "收藏的声音", 285000)
        content { SongMenuSheet(song, {}, { action = "favorite" }, { action = "playlist" }, null, { action = "rename" }, { action = "cover" }, { action = "export" }, { action = "delete" }) }
        compose.onNodeWithText("歌曲选项").assertIsDisplayed()
        compose.onNodeWithText(song.title).assertIsDisplayed()
        compose.onNodeWithText("添加到喜欢").performClick()
        compose.runOnIdle { assertEquals("favorite", action) }
        screenshot("song-menu")
        compose.onNodeWithText("删除音乐").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("delete", action) }
    }
}
