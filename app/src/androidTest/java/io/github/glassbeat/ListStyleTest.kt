package io.github.glassbeat

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.chrisbanes.haze.HazeState
import io.github.glassbeat.data.Song
import io.github.glassbeat.data.ThemeSettings
import io.github.glassbeat.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ListStyleTest {
    @get:Rule val compose = createComposeRule()
    private fun content(body: @Composable () -> Unit) = compose.setContent {
        val appearance = ThemeSettings(motion = false)
        val haze = remember { HazeState() }
        MaterialTheme(darkColorScheme()) {
            Surface {
                CompositionLocalProvider(LocalAppearance provides appearance, LocalGlass provides haze) {
                    Box(Modifier.fillMaxSize()) { GlassBackground(appearance, haze); body() }
                }
            }
        }
    }
    @Test fun rowMenuDoesNotStartPlaybackAndSelectionRemainsDistinct() {
        val songs = listOf(Song("one", "道不尽离愁。#红尘客栈", "/one", "本地音乐", 358000),
            Song("two", "Coastline", "/two", "Duett", 228000), Song("three", "First Light", "/three", "FavSoda", 285000))
        var current by mutableStateOf("one")
        var plays = 0; var menus = 0
        content {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("我的音乐", style = MaterialTheme.typography.headlineLarge)
                songs.forEach { song -> SongRow(song, current == song.id, { current = song.id; plays++ }, { menus++ }) }
            }
        }
        compose.onNodeWithContentDescription("${songs[0].title} 的菜单").performClick()
        compose.runOnIdle { assertEquals(1, menus); assertEquals(0, plays) }
        compose.onAllNodesWithContentDescription("当前播放").assertCountEquals(1)
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val context: Context = ApplicationProvider.getApplicationContext()
        val folder = File(context.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        File(folder, "separated-list.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        compose.onNodeWithText("Coastline").performClick()
        compose.runOnIdle { assertEquals("two", current); assertEquals(1, plays); assertEquals(1, menus) }
        compose.onAllNodesWithContentDescription("当前播放").assertCountEquals(1)
    }
    @Test fun matchingFilterControlsKeepSelectionAndCallbacksCorrect() {
        var favorites by mutableStateOf(false)
        var changes = 0
        content {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SoftFilterChip(!favorites, { favorites = false; changes++ }, { Text("全部 3") })
                SoftFilterChip(favorites, { favorites = true; changes++ }, { Text("喜欢 1") })
            }
        }
        compose.onNodeWithText("全部 3").assertIsSelected()
        compose.onNodeWithText("喜欢 1").performClick()
        compose.runOnIdle { assertTrue(favorites); assertEquals(1, changes) }
        compose.onNodeWithText("喜欢 1").assertIsSelected()
        compose.onNodeWithText("全部 3").assertIsNotSelected()
    }
}
