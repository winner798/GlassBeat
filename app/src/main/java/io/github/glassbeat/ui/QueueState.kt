package io.github.glassbeat.ui

import androidx.media3.common.MediaItem
import androidx.media3.common.Player

internal data class QueueEntry(val timelineIndex: Int, val item: MediaItem)
internal data class QueueUi(
    val entries: List<QueueEntry> = emptyList(),
    val current: Int = -1,
    val playing: Boolean = false,
    val shuffle: Boolean = false,
    val repeat: Boolean = false
) {
    val currentRow: Int get() = entries.indexOfFirst { it.timelineIndex == current }
}

/** Display the player's actual order, preserving original indices for commands. */
internal fun Player?.queueUi(): QueueUi {
    if (this == null) return QueueUi()
    val count = mediaItemCount
    val timeline = currentTimeline
    val order = ArrayList<Int>(count)
    val visited = BooleanArray(count)
    var index = timeline.getFirstWindowIndex(shuffleModeEnabled)
    while (index in 0 until count && !visited[index]) {
        visited[index] = true
        order += index
        index = timeline.getNextWindowIndex(index, Player.REPEAT_MODE_OFF, shuffleModeEnabled)
    }
    // A controller may briefly expose a partial timeline while its playlist changes.
    val indices = if (order.size == count) order else (0 until count).toList()
    return QueueUi(indices.map { QueueEntry(it, getMediaItemAt(it)) }, currentMediaItemIndex,
        isPlaying, shuffleModeEnabled, repeatMode == Player.REPEAT_MODE_ONE)
}
