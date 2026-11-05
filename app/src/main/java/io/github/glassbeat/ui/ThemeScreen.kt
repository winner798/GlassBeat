package io.github.glassbeat.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import io.github.glassbeat.LibraryModel
import io.github.glassbeat.data.ThemeSettings

@Composable
fun ThemeScreen(theme: ThemeSettings, model: LibraryModel) {
    val image = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let { uri -> model.setArtwork(uri) } }
    val font = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(model::setFont) }
    var opacity by remember(theme.opacity) { mutableFloatStateOf(theme.opacity) }
    var blur by remember(theme.blur) { mutableFloatStateOf(theme.blur) }
    val uriHandler = LocalUriHandler.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        GlassPanel {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("玻璃预设", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(!theme.light && theme.background.isEmpty(), { model.updateTheme { current -> current.copy(light = false, background = "") } }, label = { Text("深色玻璃") })
                    FilterChip(theme.light && theme.background.isEmpty(), { model.updateTheme { current -> current.copy(light = true, background = "") } }, label = { Text("浅色玻璃") })
                }
                OutlinedButton(onClick = { image.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) { Text(if (theme.background.isBlank()) "＋ 自定义背景图片" else "更换背景图片") }
                if (theme.background.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("浅色文字模式", Modifier.weight(1f)); Switch(!theme.light, { model.updateTheme { current -> current.copy(light = !it) } }) }
                    TextButton(onClick = model::clearBackground) { Text("移除背景") }
                }
            }
        }
        GlassPanel {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("主题颜色", style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf(0xFFB8A4FF, 0xFF74CFC8, 0xFFF0A6C2, 0xFFE8BD77, 0xFF82B4F8, 0xFFBFCD82).forEach { color ->
                        Box(Modifier.size(36.dp).background(Color(color), CircleShape).then(if (theme.accent == color) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier).clickable { model.updateTheme { current -> current.copy(accent = color) } })
                    }
                }
                var customColor by remember(theme.accent) { mutableStateOf("#%06X".format(theme.accent and 0xFFFFFF)) }
                OutlinedTextField(customColor, { customColor = it.take(7) }, label = { Text("自定义 HEX 色值") }, singleLine = true, modifier = Modifier.fillMaxWidth(), trailingIcon = {
                    TextButton(onClick = { runCatching { android.graphics.Color.parseColor(customColor).toLong() and 0xFFFFFFFFL }.onSuccess { model.updateTheme { current -> current.copy(accent = it) } }.onFailure { model.message = "请输入 #RRGGBB 格式的颜色。" } }) { Text("应用") }
                })
                Text("玻璃不透明度 ${(opacity * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium)
                Slider(opacity, { opacity = it }, onValueChangeFinished = { model.updateTheme { current -> current.copy(opacity = opacity) } }, valueRange = .15f.. .95f)
                Text("背景模糊 ${blur.toInt()}", style = MaterialTheme.typography.bodyMedium)
                Slider(blur, { blur = it }, onValueChangeFinished = { model.updateTheme { current -> current.copy(blur = blur) } }, valueRange = 0f..40f, enabled = !theme.lowPower)
                Text("Android 12 及以上支持实时背景模糊；旧设备自动使用半透明材质。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        GlassPanel {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("字体与动效", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = { font.launch(arrayOf("font/*", "application/x-font-ttf", "application/x-font-opentype", "application/octet-stream")) }, modifier = Modifier.fillMaxWidth()) { Text(if (theme.font.isBlank()) "导入 TTF / OTF 字体" else "更换自定义字体") }
                if (theme.font.isNotBlank()) TextButton(onClick = model::clearFont) { Text("恢复系统字体") }
                Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("流动背景与交互动效"); Text("低性能模式下自动关闭", style = MaterialTheme.typography.bodySmall) }; Switch(theme.motion, { model.updateTheme { current -> current.copy(motion = it) } }, enabled = !theme.lowPower) }
                Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("低性能模式"); Text("关闭实时模糊与背景动画", style = MaterialTheme.typography.bodySmall) }; Switch(theme.lowPower, { model.updateTheme { current -> current.copy(lowPower = it) } }) }
            }
        }
        GlassPanel {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("GlassBeat 1.0 RC1", style = MaterialTheme.typography.titleMedium)
                Text("无账号 · 无广告 · 无自建服务器", style = MaterialTheme.typography.bodyMedium)
                Text("本地音乐始终保存在设备。只有解析分享链接和下载媒体时才联网，不上传音乐库。卸载会清除应用内音乐，请先导出重要文件。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { uriHandler.openUri("https://github.com/winner798/GlassBeat") }) { Text("源码、隐私说明与开源许可 ↗") }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun ImportDialog(model: LibraryModel, busy: Boolean, dismiss: () -> Unit, audio: () -> Unit, video: () -> Unit) {
    var text by remember { mutableStateOf(model.sharedText) }
    AlertDialog(onDismissRequest = dismiss, title = { Text("让喜欢的声音留下来") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = audio, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("导入本地音频") }
            OutlinedButton(onClick = video, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("本地视频提取音轨") }
            HorizontalDivider()
            Text("抖音分享", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(text, { text = it }, modifier = Modifier.fillMaxWidth(), label = { Text("粘贴完整分享内容或链接") }, minLines = 2, maxLines = 4)
            Text("也可以在抖音中通过系统分享选择 GlassBeat。公开资源可访问时会显示导入预览。", style = MaterialTheme.typography.bodySmall)
            Text("仅保存你有权使用的内容。", style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = { TextButton(onClick = { dismiss(); model.resolve(text) }, enabled = text.isNotBlank() && !busy) { Text("解析链接") } }, dismissButton = { TextButton(onClick = dismiss) { Text("关闭") } })
}
