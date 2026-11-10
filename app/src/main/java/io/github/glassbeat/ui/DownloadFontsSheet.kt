package io.github.glassbeat.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import io.github.glassbeat.LibraryModel
import io.github.glassbeat.data.ThemeSettings
import io.github.glassbeat.fonts.FontCatalog

@Composable
internal fun DownloadFontsSheet(theme: ThemeSettings, model: LibraryModel, dismiss: () -> Unit) {
    val links = LocalUriHandler.current
    val task = model.fontTask
    GlassSheet(dismiss) {
        SheetHeader("发现字体", "喜欢哪一款，就下载哪一款", "关闭字体列表", dismiss)
        Column(Modifier.fillMaxWidth().heightIn(max = LocalConfiguration.current.screenHeightDp.dp * .65f)
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("字体不随安装包内置。下载后可离线切换，不需要时可删除。", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            FontCatalog.fonts.forEach { font ->
                val downloaded = font.id in model.downloadedFonts
                val active = theme.font == model.fontPath(font)
                val downloading = task?.id == font.id
                GlassListCard(Modifier.fillMaxWidth(), selected = active) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(font.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            if (active) Text("使用中", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                        }
                        Text("${font.description} · ${font.sizeLabel}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (downloading) {
                            if (task.progress >= 0) LinearProgressIndicator(progress = { task.progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                            else LinearProgressIndicator(Modifier.fillMaxWidth())
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(if (task.progress >= 0) "下载中 ${(task.progress * 100).toInt()}%" else "正在准备字体…", style = MaterialTheme.typography.bodySmall)
                                SoftTextButton(onClick = model::cancelFontDownload) { Text("取消下载") }
                            }
                        } else {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                SoftTextButton(onClick = { links.openUri(font.licenseUrl) }) { Text("开源许可 ↗") }
                                Spacer(Modifier.weight(1f))
                                if (downloaded) SoftTextButton(onClick = { model.deleteDownloadFont(font) }, enabled = task == null) { Text("删除") }
                                SoftOutlinedButton(onClick = { model.useDownloadFont(font) }, enabled = !active && task == null) {
                                    Text(if (active) "已应用" else if (downloaded) "应用" else "下载并应用")
                                }
                            }
                        }
                    }
                }
            }
            Text("来源：Google Fonts · SIL 开放字体许可证。删除正在使用的下载字体会恢复系统字体。", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
        }
    }
}
