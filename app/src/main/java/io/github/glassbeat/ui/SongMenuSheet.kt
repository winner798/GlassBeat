package io.github.glassbeat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.glassbeat.data.Song

@Composable
internal fun SongMenuSheet(song: Song, dismiss: () -> Unit, favorite: () -> Unit, playlist: () -> Unit,
    removeFromPlaylist: (() -> Unit)?, rename: () -> Unit, cover: () -> Unit, export: () -> Unit, delete: () -> Unit) {
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * .76f
    GlassSheet(dismiss) {
        SheetHeader("歌曲选项", "让每一首音乐，都有你的样子", "关闭歌曲选项", dismiss)
        Column(Modifier.heightIn(max = maxHeight).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Artwork(song.cover, Modifier.size(68.dp))
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(song.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${song.artist} · ${formatTime(song.durationMs)}", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MenuTile(if (song.favorite) "取消喜欢" else "添加到喜欢", Icons.Rounded.Favorite, Modifier.weight(1f), favorite)
                MenuTile("添加到歌单", Icons.Rounded.PlaylistAdd, Modifier.weight(1f), playlist)
            }
            Spacer(Modifier.height(4.dp))
            MenuLine("重命名", "修改歌曲显示名称", Icons.Rounded.Edit, click = rename)
            MenuLine("编辑封面", "为音乐挑选一张图片", Icons.Rounded.Image, click = cover)
            MenuLine("导出音乐文件", "保存到你选择的文件夹", Icons.Rounded.SaveAlt, click = export)
            removeFromPlaylist?.let { MenuLine("从当前歌单移除", "音乐仍保留在音乐库", Icons.Rounded.PlaylistRemove, click = it) }
            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = .08f))
            MenuLine("删除音乐", "删除应用内副本，需再次确认", Icons.Rounded.DeleteOutline, danger = true, click = delete)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MenuTile(title: String, icon: ImageVector, modifier: Modifier, click: () -> Unit) {
    Surface(onClick = click, modifier = modifier, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = .10f)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, null, Modifier.size(23.dp), tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun MenuLine(title: String, detail: String, icon: ImageVector, danger: Boolean = false, click: () -> Unit) {
    Surface(onClick = click, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = .025f)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OverlayIcon(icon, danger = danger)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
            }
            Icon(Icons.Rounded.ChevronRight, null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
