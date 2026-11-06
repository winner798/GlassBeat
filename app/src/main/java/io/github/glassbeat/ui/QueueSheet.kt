package io.github.glassbeat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import kotlinx.coroutines.launch

@Composable
internal fun QueueSheet(player: Player?, dismiss: () -> Unit) {
    var queue by remember(player) { mutableStateOf(player.queueUi()) }
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * .55f
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) { queue = player.queueUi() }
        }
        player?.addListener(listener)
        onDispose { player?.removeListener(listener) }
    }
    LaunchedEffect(queue.currentRow, queue.entries.size) { if (queue.currentRow >= 0) list.animateScrollToItem(queue.currentRow) }
    GlassSheet(dismiss) {
        SheetHeader("播放队列", "${queue.entries.size} 首音乐 · ${if (queue.shuffle) "随机播放" else "顺序播放"}${if (queue.repeat) " · 单曲循环" else ""}", "关闭播放队列", dismiss)
        if (queue.entries.isEmpty()) {
            Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                OverlayIcon(Icons.Rounded.QueueMusic)
                Text("队列还是空的", Modifier.padding(top = 16.dp), style = MaterialTheme.typography.titleMedium)
                Text("从音乐库选择一首喜欢的音乐", Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else LazyColumn(state = list, modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight), contentPadding = PaddingValues(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(queue.entries, key = { _, entry -> "${entry.timelineIndex}:${entry.item.mediaId}" }) { _, entry ->
                val index = entry.timelineIndex
                val item = entry.item
                val current = index == queue.current
                val title = item.mediaMetadata.title?.toString()?.takeIf { it.isNotBlank() } ?: "未命名歌曲"
                val artist = item.mediaMetadata.artist?.toString()?.takeIf { it.isNotBlank() } ?: "本地音乐"
                val duration = item.mediaMetadata.extras?.getLong("durationMs") ?: 0L
                val primary = MaterialTheme.colorScheme.primary
                Row(Modifier.fillMaxWidth().listCard(current)
                    .softClickable { player?.seekToDefaultPosition(index); player?.play() }.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        Artwork(item.mediaMetadata.artworkUri?.toString().orEmpty(), Modifier.size(52.dp))
                        if (current) Box(Modifier.size(52.dp).background(primary.copy(alpha = .65f), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                            Icon(if (queue.playing) Icons.Rounded.GraphicEq else Icons.Rounded.Pause, if (queue.playing) "正在播放" else "当前歌曲已暂停", Modifier.size(23.dp), tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall, fontWeight = if (current) FontWeight.Bold else FontWeight.Medium, color = if (current) primary else MaterialTheme.colorScheme.onSurface)
                        Text("$artist${if (duration > 0) " · ${formatTime(duration)}" else ""}", maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 5.dp))
                    }
                    SoftIconButton(onClick = { player?.removeMediaItem(index) }) { Icon(Icons.Rounded.Close, "移出队列：$title", Modifier.size(17.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        }
        HorizontalDivider(Modifier.padding(horizontal = 22.dp, vertical = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = .08f))
        Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("移出队列不会删除音乐", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SoftTextButton(onClick = { if (queue.currentRow >= 0) scope.launch { list.animateScrollToItem(queue.currentRow) } }, enabled = queue.currentRow >= 0) { Text("定位当前") }
        }
    }
}
