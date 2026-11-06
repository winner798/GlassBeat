package io.github.glassbeat.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.session.MediaController
import coil.compose.AsyncImage
import dev.chrisbanes.haze.HazeState
import io.github.glassbeat.LibraryModel
import io.github.glassbeat.data.*
import java.io.File

@Composable
fun FavSodaApp(model: LibraryModel, player: MediaController?) {
    val songs by model.songs.collectAsStateWithLifecycle()
    LaunchedEffect(songs, player) { syncQueueMetadata(player, songs) }
    val playlists by model.playlists.collectAsStateWithLifecycle()
    val memberships by model.memberships.collectAsStateWithLifecycle()
    val theme by model.theme.collectAsStateWithLifecycle()
    val task by model.task.collectAsStateWithLifecycle()
    LaunchedEffect(songs, player) {
        if (player != null) for (i in 0 until player.mediaItemCount) {
            val existing = player.getMediaItemAt(i)
            songs.firstOrNull { it.id == existing.mediaId }?.let { song ->
                val updated = song.mediaItem()
                if (updated.mediaMetadata != existing.mediaMetadata) player.replaceMediaItem(i, updated)
            }
        }
    }
    val playback = rememberPlayback(player) { model.message = it }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var full by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var favorites by rememberSaveable { mutableStateOf(false) }
    var playlistId by rememberSaveable { mutableStateOf<String?>(null) }
    var importDialog by remember { mutableStateOf(false) }
    var songMenu by remember { mutableStateOf<Song?>(null) }
    var renaming by remember { mutableStateOf<Song?>(null) }
    var deleting by remember { mutableStateOf<Song?>(null) }
    var addingToPlaylist by remember { mutableStateOf<Song?>(null) }
    var playlistDialog by remember { mutableStateOf(false) }
    var editingPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var deletingPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var coverTarget by remember { mutableStateOf<Song?>(null) }
    var exportTarget by rememberSaveable { mutableStateOf<String?>(null) }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { model.import(it) }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { model.import(it, true) }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { model.setArtwork(it, coverTarget) } }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val song = songs.firstOrNull { it.id == exportTarget }
        if (uri != null && song != null) model.export(song, uri)
        exportTarget = null
    }
    val accent = Color(theme.accent)
    val colors = if (theme.light) lightColorScheme(primary = androidx.compose.ui.graphics.lerp(accent, Color.Black, .4f), background = Color(0xFFF4F1FB), surface = Color(0xFFF8F6FF))
        else darkColorScheme(primary = accent, background = Color(0xFF11121E), surface = Color(0xFF222333))
    val family = remember(theme.font) { if (theme.font.isBlank()) FontFamily.Default else runCatching { FontFamily(Font(File(theme.font))) }.getOrDefault(FontFamily.Default) }
    val typography = remember(family) {
        Typography().let { it.copy(displayLarge = it.displayLarge.copy(fontFamily = family), headlineLarge = it.headlineLarge.copy(fontFamily = family),
            headlineMedium = it.headlineMedium.copy(fontFamily = family), headlineSmall = it.headlineSmall.copy(fontFamily = family),
            titleLarge = it.titleLarge.copy(fontFamily = family), titleMedium = it.titleMedium.copy(fontFamily = family), titleSmall = it.titleSmall.copy(fontFamily = family),
            bodyLarge = it.bodyLarge.copy(fontFamily = family), bodyMedium = it.bodyMedium.copy(fontFamily = family), bodySmall = it.bodySmall.copy(fontFamily = family),
            labelLarge = it.labelLarge.copy(fontFamily = family), labelMedium = it.labelMedium.copy(fontFamily = family), labelSmall = it.labelSmall.copy(fontFamily = family)) }
    }
    val haze = remember { HazeState() }
    val selectedPlaylist = playlists.firstOrNull { it.id == playlistId }
    val visible = songs.filter { song ->
        (!favorites || tab != 0 || song.favorite) && (tab != 1 || playlistId == null || memberships.any { it.playlistId == playlistId && it.songId == song.id }) &&
            (song.title.contains(query, true) || song.artist.contains(query, true))
    }
    BackHandler(full || selectedPlaylist != null) { if (full) full = false else playlistId = null }
    MaterialTheme(colorScheme = colors, typography = typography) {
        CompositionLocalProvider(LocalGlass provides haze, LocalAppearance provides theme) {
            Surface(Modifier.fillMaxSize(), color = colors.background) {
                Box(Modifier.fillMaxSize()) {
                    GlassBackground(theme, haze)
                    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 20.dp)) {
                        if (full && playback.id.isNotBlank()) {
                            PlayerScreen(playback, songs.firstOrNull { it.id == playback.id }, player, { full = false }, model::favorite)
                        } else {
                            Row(Modifier.padding(top = 22.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    if (tab == 0) BrandWordmark()
                                    else Text(if (tab == 1) selectedPlaylist?.name ?: "你的歌单" else "外观工作室", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                                    Text(if (tab == 2) "让音乐拥有你的样子" else "离线收藏 · 自由聆听", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                }
                                if (selectedPlaylist != null && tab == 1) SoftIconButton(onClick = { playlistId = null }) { Icon(Icons.Rounded.ArrowBack, "返回歌单") }
                                else SoftIconButton(onClick = { importDialog = true }, enabled = task == null) { Icon(Icons.Rounded.Add, "导入音乐") }
                            }
                            if (tab == 2) {
                                Box(Modifier.weight(1f)) { ThemeScreen(theme, model) }
                            } else if (tab == 1 && selectedPlaylist == null) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("${playlists.size} 个歌单", color = colors.onSurfaceVariant)
                                    SoftTextButton(onClick = { playlistDialog = true }) { Text("＋ 新建歌单") }
                                }
                                if (playlists.isEmpty()) EmptyLibrary("把喜欢的歌放在一起", "新建歌单后，在歌曲菜单中添加音乐。", Modifier.weight(1f))
                                else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                                    items(playlists, key = { it.id }) { list ->
                                        GlassListCard {
                                            Row(Modifier.fillMaxWidth().softClickable { playlistId = list.id; query = "" }.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Rounded.QueueMusic, null, Modifier.size(32.dp), tint = accent)
                                                Column(Modifier.weight(1f).padding(start = 14.dp)) { Text(list.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis); Text("${memberships.count { it.playlistId == list.id }} 首", style = MaterialTheme.typography.bodySmall) }
                                                var menu by remember { mutableStateOf(false) }
                                                Box { SoftIconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "歌单菜单") }
                                                    DropdownMenu(menu, { menu = false }) {
                                                        DropdownMenuItem(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp).listCard(), leadingIcon = { Icon(Icons.Rounded.Edit, null) }, text = { Text("重命名") }, onClick = { menu = false; editingPlaylist = list })
                                                        DropdownMenuItem(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp).listCard(), leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) }, text = { Text("删除歌单", color = MaterialTheme.colorScheme.error) }, onClick = { menu = false; deletingPlaylist = list })
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), placeholder = { Text("搜索歌曲、艺术家") }, leadingIcon = { Icon(Icons.Rounded.Search, null) }, singleLine = true, shape = RoundedCornerShape(24.dp))
                                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    if (tab == 0) {
                                        SoftFilterChip(!favorites, { favorites = false }, label = { Text("全部 ${songs.size}") })
                                        SoftFilterChip(favorites, { favorites = true }, label = { Text("喜欢 ${songs.count { it.favorite }}") }, leadingIcon = { Icon(Icons.Rounded.Favorite, null, Modifier.size(15.dp)) })
                                    } else Text("${visible.size} 首歌曲", Modifier.weight(1f))
                                    Spacer(Modifier.weight(1f))
                                    SoftIconButton(onClick = { visible.firstOrNull()?.let { playQueue(player, visible.shuffled(), it); player?.shuffleModeEnabled = true; full = true } }, enabled = visible.isNotEmpty() && player != null) { Icon(Icons.Rounded.Shuffle, "随机播放列表") }
                                }
                                if (visible.isEmpty()) EmptyLibrary(if (songs.isEmpty()) "音乐，从这里开始" else "这里还没有音乐", if (songs.isEmpty()) "点右上角 ＋，导入音频或提取视频音轨。" else "试试其他搜索词，或向歌单添加歌曲。", Modifier.weight(1f))
                                else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp)) {
                                    items(visible, key = { it.id }) { song ->
                                        SongRow(song, song.id == playback.id, { if (player != null) { playQueue(player, visible, song); full = true } }, { songMenu = song })
                                    }
                                }
                            }
                            if (playback.id.isNotBlank()) MiniPlayer(playback, player) { full = true }
                            GlassNavigation(tab, { tab = it; query = ""; playlistId = null }, Modifier.fillMaxWidth().padding(vertical = 12.dp))
                        }
                    }
                    task?.let { current ->
                        GlassPanel(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 100.dp).fillMaxWidth()) {
                            Column(Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) { Text(current.label, Modifier.weight(1f)); SoftTextButton(onClick = model::cancelImport) { Text("取消") } }
                                if (current.progress in 0f..1f) LinearProgressIndicator(progress = { current.progress }, modifier = Modifier.fillMaxWidth()) else LinearProgressIndicator(Modifier.fillMaxWidth())
                                Text("切换页面不会中断导入", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                            }
                        }
                    }
                }
            }
            AppUpdateDialog(model)
            if (importDialog) ImportDialog(model, task != null, { importDialog = false }, { importDialog = false; audioPicker.launch(arrayOf("audio/*")) }, { importDialog = false; videoPicker.launch(arrayOf("video/*")) })
            model.preview?.let { preview ->
                var allowed by remember(preview) { mutableStateOf(false) }
                GlassDialog(onDismissRequest = { if (task == null) model.preview = null }, title = { Text("发现新的音轨") }, text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Artwork("", Modifier.size(58.dp))
                            Text(preview.title, Modifier.weight(1f).padding(start = 12.dp), style = MaterialTheme.typography.titleMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                        Text("来源：抖音视频 · ${if (preview.durationMs > 0) formatTime(preview.durationMs) else "时长将在导入后识别"}")
                        Text(if (task == null) "保存视频的完整音轨到本地音乐库。" else task!!.label)
                        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(allowed, { allowed = it }); Text("我有权保存这段内容", style = MaterialTheme.typography.bodySmall) }
                    }
                }, confirmButton = { SoftTextButton(onClick = model::savePreview, enabled = allowed && task == null) { Text("保存到音乐库") } }, dismissButton = { SoftTextButton(onClick = { model.preview = null }, enabled = task == null) { Text("取消") } })
            }
            songMenu?.let { song ->
                SongMenuSheet(song, { songMenu = null },
                    favorite = { model.favorite(song); songMenu = null },
                    playlist = { addingToPlaylist = song; songMenu = null },
                    removeFromPlaylist = if (tab == 1 && selectedPlaylist != null) ({ model.setMembership(song, selectedPlaylist, false); songMenu = null }) else null,
                    rename = { renaming = song; songMenu = null },
                    cover = { coverTarget = song; coverPicker.launch(arrayOf("image/*")); songMenu = null },
                    export = { exportTarget = song.id; exporter.launch(exportName(song)); songMenu = null },
                    delete = { deleting = song; songMenu = null })
            }
            renaming?.let { song -> TextEntryDialog("重命名歌曲", song.title, { renaming = null }) { name ->
                model.rename(song, name)
                renaming = null
            } }
            deleting?.let { song -> ConfirmDialog("删除音乐？", "将删除“${song.title}”在 FavSoda 中的副本及歌单记录。原始文件不受影响。", { deleting = null }) {
                if (player != null) for (i in player.mediaItemCount - 1 downTo 0) if (player.getMediaItemAt(i).mediaId == song.id) player.removeMediaItem(i)
                model.delete(song); deleting = null
            } }
            if (playlistDialog) TextEntryDialog("新建歌单", "", { playlistDialog = false }) { model.createPlaylist(it); playlistDialog = false }
            editingPlaylist?.let { list -> TextEntryDialog("重命名歌单", list.name, { editingPlaylist = null }) { model.renamePlaylist(list, it); editingPlaylist = null } }
            deletingPlaylist?.let { list -> ConfirmDialog("删除歌单？", "仅删除歌单“${list.name}”，音乐仍保留在音乐库。", { deletingPlaylist = null }) { model.deletePlaylist(list); playlistId = null; deletingPlaylist = null } }
            addingToPlaylist?.let { song -> GlassDialog(onDismissRequest = { addingToPlaylist = null }, title = { Text("添加到歌单") }, text = {
                if (playlists.isEmpty()) Text("先到“歌单”页面创建一个歌单。") else Column {
                    playlists.forEach { list -> val checked = memberships.any { it.playlistId == list.id && it.songId == song.id }
                        GlassListCard(Modifier.fillMaxWidth().padding(vertical = 4.dp), selected = checked) {
                            Row(Modifier.fillMaxWidth().softClickable { model.setMembership(song, list, !checked) }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) { Checkbox(checked, { model.setMembership(song, list, it) }); Text(list.name, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                        }
                    }
                }
            }, confirmButton = { SoftTextButton(onClick = { addingToPlaylist = null }) { Text("完成") } }) }
            model.message?.let { text -> GlassDialog(onDismissRequest = { model.message = null }, title = { Text("FavSoda") }, text = { Text(text) }, confirmButton = { SoftTextButton(onClick = { model.message = null }) { Text("知道了") } }, dismissButton = {
                if (text.contains("抖音") || text.contains("媒体资源")) SoftTextButton(onClick = { model.message = null; videoPicker.launch(arrayOf("video/*")) }) { Text("本地视频提取") }
            }) }
        }
    }
}

@Composable private fun EmptyLibrary(title: String, detail: String, modifier: Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Rounded.GraphicEq, null, Modifier.size(70.dp), tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 18.dp))
        Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp, start = 12.dp, end = 12.dp))
    }
}
@Composable fun Artwork(path: String, modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = .18f)) {
        if (path.isNotBlank()) AsyncImage(if (path.startsWith("file:")) path else File(path), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.MusicNote, null, Modifier.fillMaxSize(.45f), tint = MaterialTheme.colorScheme.primary) }
    }
}
@Composable internal fun SongRow(song: Song, current: Boolean, onPlay: () -> Unit, onMenu: () -> Unit) {
    GlassListCard(Modifier.fillMaxWidth(), selected = current) {
        Row(Modifier.fillMaxWidth().softClickable(onClick = onPlay).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                Artwork(song.cover, Modifier.size(54.dp))
                if (current) Box(Modifier.align(Alignment.BottomEnd).size(19.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.GraphicEq, "当前播放", Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontWeight = if (current) FontWeight.Bold else FontWeight.Medium)
                Text("${song.artist} · ${formatTime(song.durationMs)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp))
            }
            if (song.favorite) Icon(Icons.Rounded.Favorite, "已喜欢", Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
            SoftIconButton(onClick = onMenu) { Icon(Icons.Rounded.MoreHoriz, "${song.title} 的菜单") }
        }
    }
}
@Composable fun TextEntryDialog(title: String, initial: String, dismiss: () -> Unit, confirm: (String) -> Unit) {
    var value by remember(initial) { mutableStateOf(initial) }
    GlassDialog(onDismissRequest = dismiss, title = { Text(title) }, text = { OutlinedTextField(value, { value = it.take(200) }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(16.dp), label = { Text("名称") }) }, confirmButton = { SoftTextButton(onClick = { confirm(value.trim()) }, enabled = value.isNotBlank()) { Text("保存") } }, dismissButton = { SoftTextButton(onClick = dismiss) { Text("取消") } })
}
@Composable private fun ConfirmDialog(title: String, detail: String, dismiss: () -> Unit, confirm: () -> Unit) {
    GlassDialog(onDismissRequest = dismiss, title = { Text(title) }, text = { Text(detail) }, confirmButton = { SoftTextButton(onClick = confirm) { Text("确认删除") } }, dismissButton = { SoftTextButton(onClick = dismiss) { Text("取消") } })
}
private fun exportName(song: Song): String {
    val ext = when { song.mime.contains("flac") -> "flac"; song.mime.contains("wav") -> "wav"; song.mime.contains("ogg") -> "ogg"; song.mime.contains("mp4") -> "m4a"; song.mime.contains("aac") -> "aac"; else -> "mp3" }
    return song.title.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(100) + ".$ext"
}
