package io.github.glassbeat

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.delay
import java.io.File

class MainActivity : ComponentActivity() {
    private val model: LibraryModel by viewModels()
    private var controller by mutableStateOf<MediaController?>(null)
    private var future: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) model.receive(intent)
        setContent { GlassBeat(model, controller) }
    }
    override fun onStart() {
        super.onStart()
        val pending = MediaController.Builder(this, SessionToken(this, ComponentName(this, PlaybackService::class.java))).buildAsync()
        future = pending
        pending.addListener({
            if (future === pending) {
                try { controller = pending.get() } catch (_: Exception) { model.message = "播放服务连接失败，请重试。" }
            }
        }, ContextCompat.getMainExecutor(this))
    }
    override fun onStop() {
        controller = null
        future?.let { MediaController.releaseFuture(it) }
        future = null
        super.onStop()
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); model.receive(intent) }
}

@Composable
private fun GlassBeat(model: LibraryModel, player: MediaController?) {
    val colors = if (model.light) lightColorScheme(primary = Color(0xFF6251C8)) else darkColorScheme(primary = Color(0xFFC3B5FF))
    var query by remember { mutableStateOf("") }
    var full by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var playing by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var shuffle by remember { mutableStateOf(false) }
    var repeat by remember { mutableStateOf(false) }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { model.import(it) }
    LaunchedEffect(player) {
        while (true) {
            title = player?.mediaMetadata?.title?.toString().orEmpty()
            playing = player?.isPlaying == true
            position = player?.currentPosition ?: 0
            duration = (player?.duration ?: 0).coerceAtLeast(0)
            shuffle = player?.shuffleModeEnabled == true
            repeat = player?.repeatMode == Player.REPEAT_MODE_ONE
            delay(300)
        }
    }
    MaterialTheme(colorScheme = colors) {
        Surface(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(
                colors.primary.copy(alpha = .22f), colors.background, colors.tertiary.copy(alpha = .15f))))) {
                Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("GlassBeat", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                            Text("你的音乐，自由流动", color = colors.onSurfaceVariant)
                        }
                        IconButton(onClick = model::toggleTheme) { Icon(Icons.Rounded.Palette, "切换深浅主题") }
                    }
                    if (full && title.isNotEmpty()) {
                        TextButton(onClick = { full = false }) { Text("↓ 返回音乐库") }
                        Spacer(Modifier.weight(1f))
                        Card(Modifier.fillMaxWidth().aspectRatio(1f), shape = RoundedCornerShape(36.dp), colors = CardDefaults.cardColors(containerColor = colors.primary.copy(alpha = .18f))) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.MusicNote, null, Modifier.size(110.dp), tint = colors.primary) }
                        }
                        Text(title, style = MaterialTheme.typography.headlineSmall)
                        Slider(value = position.toFloat().coerceIn(0f, duration.toFloat().coerceAtLeast(1f)), onValueChange = { player?.seekTo(it.toLong()) }, valueRange = 0f..duration.toFloat().coerceAtLeast(1f))
                        Text("${time(position)} / ${time(duration)}")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { player?.shuffleModeEnabled = !shuffle }) { Icon(Icons.Rounded.Shuffle, "随机播放", tint = if (shuffle) colors.primary else colors.onSurfaceVariant) }
                            IconButton(onClick = { player?.seekToPreviousMediaItem() }) { Icon(Icons.Rounded.SkipPrevious, "上一首") }
                            FilledIconButton(onClick = { if (playing) player?.pause() else player?.play() }, modifier = Modifier.size(64.dp)) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "播放或暂停") }
                            IconButton(onClick = { player?.seekToNextMediaItem() }) { Icon(Icons.Rounded.SkipNext, "下一首") }
                            IconButton(onClick = { player?.repeatMode = if (repeat) Player.REPEAT_MODE_OFF else Player.REPEAT_MODE_ONE }) { Icon(Icons.Rounded.RepeatOne, "单曲循环", tint = if (repeat) colors.primary else colors.onSurfaceVariant) }
                        }
                        Spacer(Modifier.weight(1f))
                    } else {
                        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), placeholder = { Text("搜索你的音乐") }, singleLine = true, shape = RoundedCornerShape(24.dp))
                        Button(onClick = { importer.launch(arrayOf("audio/*")) }, enabled = !model.importing, modifier = Modifier.fillMaxWidth()) { Text(if (model.importing) "正在导入…" else "＋ 导入本地音乐") }
                        Text("我的音乐 · ${model.songs.size}", style = MaterialTheme.typography.titleMedium)
                        if (model.songs.isEmpty()) {
                            Column(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.LibraryMusic, null, Modifier.size(64.dp), tint = colors.primary)
                                Text("从第一首喜欢的歌开始", Modifier.padding(top = 16.dp))
                                Text("音频会保存到应用内，离线随时聆听", style = MaterialTheme.typography.bodySmall)
                            }
                        } else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(model.songs.filter { it.title.contains(query, true) }, key = { it.id }) { song ->
                                Card(onClick = {
                                    player?.let { p ->
                                        val queue = model.songs.map { MediaItem.Builder().setMediaId(it.id).setUri(android.net.Uri.fromFile(File(it.path))).setMediaMetadata(MediaMetadata.Builder().setTitle(it.title).setArtist("本地音乐").build()).build() }
                                        p.setMediaItems(queue, model.songs.indexOf(song), 0L); p.prepare(); p.play(); full = true
                                    }
                                }, enabled = player != null, colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = .65f))) {
                                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.MusicNote, null, tint = colors.primary)
                                        Text(song.title, Modifier.padding(start = 12.dp).weight(1f))
                                        Icon(Icons.Rounded.PlayArrow, "播放")
                                    }
                                }
                            }
                        }
                        if (title.isNotEmpty()) Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = colors.primaryContainer.copy(alpha = .65f))) {
                            Row(Modifier.clickable { full = true }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(title, Modifier.weight(1f), maxLines = 1)
                                IconButton(onClick = { if (playing) player?.pause() else player?.play() }) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "播放或暂停") }
                            }
                        }
                    }
                }
            }
        }
        model.message?.let { message -> AlertDialog(onDismissRequest = { model.message = null }, title = { Text("GlassBeat") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { model.message = null }) { Text("知道了") } }) }
    }
}
private fun time(ms: Long): String = "%d:%02d".format(ms / 60000, ms / 1000 % 60)
