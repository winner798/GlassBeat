package io.github.glassbeat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun ImportSheet(initialText: String, busy: Boolean, dismiss: () -> Unit, audio: () -> Unit, video: () -> Unit, resolve: (String) -> Unit) {
    var text by remember(initialText) { mutableStateOf(initialText) }
    val clipboard = LocalClipboardManager.current
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * .76f
    GlassSheet(dismiss) {
        SheetHeader("导入音乐", "把喜欢的声音，收进你的音乐库", "关闭导入", dismiss)
        Column(Modifier.heightIn(max = maxHeight).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SourceCard("导入本地音频", "MP3 · FLAC · M4A 等，支持多选", Icons.Rounded.LibraryMusic, !busy, audio)
            SourceCard("本地视频提取音轨", "保留声音，保存为本地音乐", Icons.Rounded.VideoLibrary, !busy, video)
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Link, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Text("抖音分享", Modifier.weight(1f).padding(start = 8.dp), style = MaterialTheme.typography.titleSmall)
                SoftTextButton(onClick = { clipboard.getText()?.text?.let { text = it } }, enabled = !busy) { Text("粘贴") }
            }
            OutlinedTextField(text, { text = it }, modifier = Modifier.fillMaxWidth(), enabled = !busy,
                placeholder = { Text("粘贴完整分享文案或视频链接", style = MaterialTheme.typography.bodyMedium) },
                minLines = 2, maxLines = 4, shape = RoundedCornerShape(18.dp))
            Text("也可在抖音系统分享中选择 FavSoda，先预览再保存。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SoftButton(onClick = { resolve(text) }, enabled = text.isNotBlank() && !busy, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(17.dp)) {
                Text("解析并预览", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp)); Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(18.dp))
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lock, null, Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("本地保存 · 仅导入你有权使用的内容", Modifier.padding(start = 5.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SourceCard(title: String, detail: String, icon: ImageVector, enabled: Boolean, click: () -> Unit) {
    SoftSurface(onClick = click, enabled = enabled, shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = .045f)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            OverlayIcon(icon)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 5.dp))
            }
            Icon(Icons.Rounded.ChevronRight, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
