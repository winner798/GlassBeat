package io.github.glassbeat

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.chrisbanes.haze.HazeState
import io.github.glassbeat.data.ThemeSettings
import io.github.glassbeat.ui.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class GlassNavigationTest {
    @get:Rule val compose = createComposeRule()
    private fun content(appearance: ThemeSettings, selected: () -> Int, select: (Int) -> Unit) = compose.setContent {
        val haze = remember { HazeState() }
        MaterialTheme(if (appearance.light) lightColorScheme() else darkColorScheme()) {
            CompositionLocalProvider(LocalAppearance provides appearance, LocalGlass provides haze) {
                Box(Modifier.fillMaxSize()) {
                    GlassBackground(appearance.copy(motion = false), haze)
                    GlassPanel(Modifier.align(Alignment.Center).padding(24.dp).fillMaxWidth()) {
                        Column(Modifier.padding(28.dp)) {
                            Text("流动的声音", style = MaterialTheme.typography.headlineMedium)
                            Text("FavSoda", modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    GlassNavigation(selected(), select, Modifier.align(Alignment.BottomCenter).padding(20.dp))
                }
            }
        }
    }
    private fun screenshot(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val context: Context = ApplicationProvider.getApplicationContext()
        val directory = File(context.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Test fun navigationPreviewsLargeLensThenCommitsOnlyCompletedClick() {
        compose.mainClock.autoAdvance = false
        var selected by mutableIntStateOf(0)
        var clicks = 0
        content(ThemeSettings(), { selected }, { selected = it; clicks++ })
        compose.mainClock.advanceTimeBy(1000)
        val before = compose.onNodeWithTag("glass-navigation").captureToImage().asAndroidBitmap()
        screenshot("glass-depth-dark")
        compose.onNodeWithText("歌单").performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(200)
        compose.runOnIdle { assertEquals(0, selected); assertEquals(0, clicks) }
        val during = compose.onNodeWithTag("glass-navigation").captureToImage().asAndroidBitmap()
        val a = IntArray(before.width * before.height); val b = IntArray(a.size)
        before.getPixels(a, 0, before.width, 0, 0, before.width, before.height)
        during.getPixels(b, 0, during.width, 0, 0, during.width, during.height)
        assertTrue("The navigation lens should visibly move and expand", a.indices.count { a[it] != b[it] } > a.size / 10)
        screenshot("navigation-pressed")
        compose.onNodeWithText("歌单").performTouchInput { cancel() }
        compose.mainClock.advanceTimeBy(1000)
        compose.runOnIdle { assertEquals(0, selected); assertEquals(0, clicks) }
        compose.onNodeWithText("外观").performTouchInput { click(center) }
        compose.mainClock.advanceTimeBy(1000)
        compose.runOnIdle { assertEquals(2, selected); assertEquals(1, clicks) }
        compose.onNodeWithText("外观").assertIsSelected()
    }
    @Test fun lightGlassAndReducedMotionKeepNavigationUsable() {
        var selected by mutableIntStateOf(0)
        content(ThemeSettings(light = true, motion = false, lowPower = true), { selected }, { selected = it })
        compose.onNodeWithText("歌单").performClick()
        compose.runOnIdle { assertEquals(1, selected) }
        compose.onNodeWithText("歌单").assertIsSelected()
        screenshot("glass-depth-light-low-power")
    }
}
