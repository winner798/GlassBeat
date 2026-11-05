package io.github.glassbeat.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import io.github.glassbeat.data.Song

@Composable
fun ColumnScope.PlayerScreen(state: PlaybackUi, song: Song?, player: MediaController?, back: () -> Unit, favorite: (Song) -> Unit) {
    val t = LocalAppearance.current
    var queue by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (state.playing && t.motion && !t.lowPower) 1f else .96f, label = "artwork")
    BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
        val artSize = (maxHeight * .43f).coerceIn(140.dp, 340.dp)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                SoftIconButton(onClick = back) { Icon(Icons.Rounded.KeyboardArrowDown, "收起播放器") }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("正在播放", style = MaterialTheme.typography.labelLarge)
                    Text(song?.source ?: "本地音乐", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                SoftIconButton(onClick = { queue = true }) { Icon(Icons.Rounded.QueueMusic, "播放队列") }
            }
            Artwork(song?.cover ?: state.cover, Modifier.size(artSize).graphicsLayer { scaleX = scale; scaleY = scale })
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(song?.title ?: state.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(song?.artist ?: state.artist, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp), maxLines = 1)
                }
                SoftIconButton(onClick = { song?.let(favorite) }, enabled = song != null) { Icon(if (song?.favorite == true) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "切换喜欢", tint = MaterialTheme.colorScheme.primary) }
            }
            Spacer(Modifier.height(18.dp))
            GlassPanel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
                    PlaybackProgress(
                        mediaId = state.id, duration = state.duration, playing = state.playing,
                        lowPower = t.lowPower,
                        readPosition = { player?.currentPosition ?: state.position },
                        onSeek = { player?.seekTo(it) }
                    )
                    Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        SoftIconButton(onClick = { player?.shuffleModeEnabled = !state.shuffle }) { Icon(Icons.Rounded.Shuffle, if (state.shuffle) "关闭随机播放" else "开启随机播放", tint = if (state.shuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                        SoftIconButton(onClick = { player?.seekToPrevious() }) { Icon(Icons.Rounded.SkipPrevious, "上一首", Modifier.size(32.dp)) }
                        SoftFilledIconButton(onClick = { togglePlayback(player) }, modifier = Modifier.size(68.dp)) {
                            if (state.buffering) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            else Icon(if (state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (state.playing) "暂停" else "播放", Modifier.size(34.dp))
                        }
                        SoftIconButton(onClick = { player?.seekToNextMediaItem() }) { Icon(Icons.Rounded.SkipNext, "下一首", Modifier.size(32.dp)) }
                        SoftIconButton(onClick = { player?.repeatMode = if (state.repeat) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ONE }) { Icon(Icons.Rounded.RepeatOne, if (state.repeat) "关闭单曲循环" else "开启单曲循环", tint = if (state.repeat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
            Text("音乐属于此刻", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 22.dp))
        }
    }
    if (queue) QueueSheet(player) { queue = false }
}

@Composable
fun MiniPlayer(state: PlaybackUi, player: MediaController?, open: () -> Unit) {
    GlassPanel(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().softClickable(onClick = open).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Artwork(state.cover, Modifier.size(46.dp))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(state.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
                Text(state.artist, maxLines = 1, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SoftIconButton(onClick = { togglePlayback(player) }) { Icon(if (state.playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "播放或暂停") }
            SoftIconButton(onClick = { player?.seekToNextMediaItem() }) { Icon(Icons.Rounded.SkipNext, "下一首") }
        }
        LinearProgressIndicator(progress = { if (state.duration > 0) state.position.toFloat() / state.duration else 0f }, modifier = Modifier.fillMaxWidth().height(2.dp))
    }
}
