package io.github.glassbeat

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.glassbeat.ui.QueueSheet
import io.github.glassbeat.ui.queueUi
import io.github.glassbeat.ui.syncQueueMetadata
import io.github.glassbeat.data.Song
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@RunWith(AndroidJUnit4::class)
class QueueOrderTest {
    @get:Rule val compose = createComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private fun items() = listOf("One", "Two", "Three").map { title ->
        MediaItem.Builder().setMediaId(title).setUri("file:///unused-$title.mp3")
            .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build()).build()
    }
    @Test fun snapshotTracksShuffleAndRepeatWithoutLosingCommandIndices() {
        compose.runOnUiThread {
            val player = ExoPlayer.Builder(context).build()
            try {
                player.setMediaItems(items(), 0, 0)
                player.setShuffleOrder(DefaultShuffleOrder(intArrayOf(2, 0, 1), 123L))
                player.shuffleModeEnabled = true
                player.repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
                val snapshot = player.queueUi()
                assertEquals(listOf("Three", "One", "Two"), snapshot.entries.map { it.item.mediaId })
                assertEquals(listOf(2, 0, 1), snapshot.entries.map { it.timelineIndex })
                assertEquals(1, snapshot.currentRow)
                player.shuffleModeEnabled = false
                assertEquals(listOf(0, 1, 2), player.queueUi().entries.map { it.timelineIndex })
                player.clearMediaItems()
                assertTrue(player.queueUi().entries.isEmpty())
                assertEquals(-1, player.queueUi().currentRow)
            } finally { player.release() }
        }
    }
    @Test fun editedMetadataPreservesQueueOrderAndPlaybackPosition() {
        compose.runOnUiThread {
            val player = ExoPlayer.Builder(context).build()
            try {
                player.setMediaItems(items(), 0, 12345)
                player.setShuffleOrder(DefaultShuffleOrder(intArrayOf(2, 0, 1), 123L))
                player.shuffleModeEnabled = true
                player.playWhenReady = true
                val before = player.queueUi().entries.map { it.item.mediaId }
                val local = player.currentMediaItem!!.localConfiguration
                syncQueueMetadata(player, listOf(Song("One", "Updated title", "/unused-One.mp3", "Updated artist", 45678, cover = "/new-cover.png")))
                assertEquals("Updated title", player.currentMediaItem!!.mediaMetadata.title.toString())
                assertEquals("Updated artist", player.currentMediaItem!!.mediaMetadata.artist.toString())
                assertTrue(player.currentMediaItem!!.mediaMetadata.artworkUri.toString().endsWith("/new-cover.png"))
                assertEquals(local, player.currentMediaItem!!.localConfiguration)
                assertEquals(12345L, player.currentPosition)
                assertTrue(player.playWhenReady)
                assertEquals(before, player.queueUi().entries.map { it.item.mediaId })
                assertEquals(listOf("One", "Two", "Three"), (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId })
            } finally { player.release() }
        }
    }
    @Test fun shuffledRowsSelectAndRemoveTheDisplayedSong() {
        lateinit var player: ExoPlayer
        compose.runOnUiThread {
            player = ExoPlayer.Builder(context).build()
            player.setMediaItems(items(), 0, 0)
            player.setShuffleOrder(DefaultShuffleOrder(intArrayOf(2, 0, 1), 123L))
            player.shuffleModeEnabled = true
        }
        try {
            compose.setContent { MaterialTheme { QueueSheet(player) {} } }
            val threeY = compose.onNodeWithText("Three").fetchSemanticsNode().boundsInRoot.top
            val oneY = compose.onNodeWithText("One").fetchSemanticsNode().boundsInRoot.top
            assertTrue(threeY < oneY)
            compose.onNodeWithText("Three").performClick()
            compose.runOnIdle { assertEquals("Three", player.currentMediaItem?.mediaId) }
            compose.onNodeWithContentDescription("移出队列：Two").performClick()
            compose.waitUntil(3000) { compose.onAllNodesWithText("2 首音乐 · 随机播放").fetchSemanticsNodes().isNotEmpty() }
            compose.runOnIdle {
                assertEquals(listOf("One", "Three"), (0 until player.mediaItemCount).map { player.getMediaItemAt(it).mediaId })
                assertEquals("Three", player.currentMediaItem?.mediaId)
            }
        } finally { compose.runOnUiThread { player.release() } }
    }
}
