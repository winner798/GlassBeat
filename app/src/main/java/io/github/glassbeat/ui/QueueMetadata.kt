package io.github.glassbeat.ui

import androidx.media3.common.Player
import io.github.glassbeat.data.Song

/** Refresh metadata without rebuilding the queue or changing its selected position. */
internal fun syncQueueMetadata(player: Player?, songs: List<Song>) {
    if (player == null || songs.isEmpty()) return
    val byId = songs.associateBy { it.id }
    for (index in 0 until player.mediaItemCount) {
        val item = player.getMediaItemAt(index)
        val song = byId[item.mediaId] ?: continue
        val old = item.mediaMetadata
        val updated = song.mediaItem().mediaMetadata
        if (old.title?.toString() != updated.title?.toString() ||
            old.artist?.toString() != updated.artist?.toString() ||
            old.artworkUri != updated.artworkUri ||
            old.extras?.getLong("durationMs") != song.durationMs) {
            player.replaceMediaItem(index, item.buildUpon().setMediaMetadata(updated).build())
        }
    }
}
