package io.github.glassbeat.ui

import android.net.Uri
import androidx.compose.runtime.*
import androidx.media3.common.*
import androidx.media3.session.MediaController
import io.github.glassbeat.data.Song
import kotlinx.coroutines.delay
import java.io.File

data class PlaybackUi(val id: String = "", val title: String = "", val artist: String = "", val cover: String = "", val playing: Boolean = false,
    val buffering: Boolean = false, val position: Long = 0, val duration: Long = 0, val shuffle: Boolean = false, val repeat: Boolean = false)
@Composable
fun rememberPlayback(player: MediaController?, onError: (String) -> Unit): PlaybackUi {
    var state by remember { mutableStateOf(PlaybackUi()) }
    val errorHandler by rememberUpdatedState(onError)
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) { errorHandler("播放失败：文件损坏、文件不存在或设备不支持此编码。可尝试重新导入。") }
        }
        player?.addListener(listener)
        onDispose { player?.removeListener(listener) }
    }
    LaunchedEffect(player) {
        while (true) {
            state = if (player == null) PlaybackUi() else PlaybackUi(player.currentMediaItem?.mediaId.orEmpty(),
                player.mediaMetadata.title?.toString().orEmpty(), player.mediaMetadata.artist?.toString().orEmpty(),
                player.mediaMetadata.artworkUri?.toString().orEmpty(), player.isPlaying, player.playbackState == Player.STATE_BUFFERING,
                player.currentPosition.coerceAtLeast(0), player.duration.coerceAtLeast(0), player.shuffleModeEnabled, player.repeatMode == Player.REPEAT_MODE_ONE)
            delay(400)
        }
    }
    return state
}
fun Song.mediaItem(): MediaItem = MediaItem.Builder().setMediaId(id).setUri(Uri.fromFile(File(path)))
    .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist(artist)
        .setArtworkUri(cover.takeIf { it.isNotBlank() }?.let { Uri.fromFile(File(it)) }).build()).build()
fun playQueue(player: MediaController?, songs: List<Song>, selected: Song) {
    if (player == null) return
    if (player.currentMediaItem?.mediaId == selected.id) { if (!player.isPlaying) { if (player.playbackState == Player.STATE_ENDED) player.seekToDefaultPosition(); player.play() }; return }
    val index = songs.indexOfFirst { it.id == selected.id }
    if (index < 0) return
    player.setMediaItems(songs.map { it.mediaItem() }, index, 0)
    player.prepare(); player.play()
}
fun formatTime(ms: Long): String = "%d:%02d".format(ms.coerceAtLeast(0) / 60000, ms.coerceAtLeast(0) / 1000 % 60)

fun togglePlayback(player: Player?) {
    if (player == null) return
    if (player.isPlaying) player.pause()
    else {
        if (player.playbackState == Player.STATE_ENDED) player.seekToDefaultPosition()
        player.play()
    }
}
