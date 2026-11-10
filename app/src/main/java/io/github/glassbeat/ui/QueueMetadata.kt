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
        val artwork = song.cover.takeIf { it.isNotBlank() }?.let { android.net.Uri.fromFile(java.io.File(it)) }
        if (old.title?.toString() != song.title ||
            old.artist?.toString() != song.artist ||
            old.artworkUri != artwork ||
            old.extras?.getLong("durationMs") != song.durationMs) {
            player.replaceMediaItem(index, item.buildUpon().setMediaMetadata(song.mediaItem().mediaMetadata).build())
        }
    }
}
