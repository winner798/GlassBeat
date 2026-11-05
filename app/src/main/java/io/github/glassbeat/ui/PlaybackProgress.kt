package io.github.glassbeat.ui

import android.os.SystemClock
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.delay
import kotlin.math.abs

/** Keep per-frame reads and scrubbing local so artwork and glass controls stay untouched. */
@Composable
internal fun PlaybackProgress(
    mediaId: String,
    duration: Long,
    playing: Boolean,
    lowPower: Boolean,
    readPosition: () -> Long,
    onSeek: (Long) -> Unit
) {
    val end = duration.coerceAtLeast(0)
    var dragging by remember(mediaId, end) { mutableStateOf(false) }
    var position by remember(mediaId, end) { mutableLongStateOf(readPosition().coerceIn(0, end)) }
    var pending by remember(mediaId, end) { mutableStateOf<Pair<Long, Long>?>(null) }
    val latestPosition by rememberUpdatedState(readPosition)
    val latestSeek by rememberUpdatedState(onSeek)
    LaunchedEffect(mediaId, end, playing, lowPower) {
        while (true) {
            if (playing && !lowPower) withInfiniteAnimationFrameNanos { } else delay(100)
            if (!dragging) {
                val actual = latestPosition().coerceIn(0, end)
                val requested = pending
                // MediaController may acknowledge a seek after its next position update.
                if (requested == null || abs(actual - requested.first) <= 500 ||
                    SystemClock.uptimeMillis() - requested.second >= 1500) {
                    pending = null
                    position = actual
                }
            }
        }
    }
    val elapsedLabel = remember(position / 1000) { formatTime(position) }
    val durationLabel = remember(end / 1000) { formatTime(end) }
    Column {
        Slider(
            value = position.toFloat().coerceIn(0f, end.toFloat().coerceAtLeast(1f)),
            onValueChange = { dragging = true; pending = null; position = it.toLong().coerceIn(0, end) },
            onValueChangeFinished = {
                if (dragging) {
                    pending = position to SystemClock.uptimeMillis()
                    latestSeek(position)
                    dragging = false
                }
            },
            valueRange = 0f..end.toFloat().coerceAtLeast(1f),
            enabled = end > 0,
            modifier = Modifier.fillMaxWidth().testTag("playback-progress")
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(elapsedLabel, style = MaterialTheme.typography.labelSmall)
            Text(durationLabel, style = MaterialTheme.typography.labelSmall)
        }
    }
}
