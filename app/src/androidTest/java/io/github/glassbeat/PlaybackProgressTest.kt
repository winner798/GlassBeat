package io.github.glassbeat

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.glassbeat.ui.PlaybackProgress
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaybackProgressTest {
    @get:Rule val compose = createComposeRule()

    @Test fun dragPreviewsWithoutSeekingAndReleaseDoesNotJumpBack() {
        val seeks = mutableListOf<Long>()
        var actual = 10_000L
        compose.setContent {
            MaterialTheme { PlaybackProgress("song", 100_000, false, false, { actual }, { seeks += it }) }
        }
        val slider = compose.onNodeWithTag("playback-progress")
        slider.performTouchInput {
            down(Offset(width * .12f, centerY))
            moveTo(Offset(width * .75f, centerY), delayMillis = 300)
        }
        compose.runOnIdle { assertTrue(seeks.isEmpty()) }
        val preview = slider.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current
        assertTrue(preview > 60_000)
        slider.performTouchInput { up() }
        compose.runOnIdle { assertEquals(1, seeks.size) }
        // Let the polling loop observe the old player position before acknowledging the seek.
        compose.mainClock.advanceTimeBy(300)
        assertEquals(preview, slider.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current, 2f)
        compose.runOnIdle { actual = seeks.single() + 100 }
        compose.mainClock.advanceTimeBy(200)
        assertEquals(actual.toFloat(), slider.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current, 2f)
    }

    @Test fun changingTrackClearsUnacknowledgedSeek() {
        var id by mutableStateOf("first")
        var actual = 10_000L
        compose.setContent {
            MaterialTheme { PlaybackProgress(id, 100_000, false, true, { actual }, {}) }
        }
        val slider = compose.onNodeWithTag("playback-progress")
        slider.performSemanticsAction(SemanticsActions.SetProgress) { it(80_000f) }
        compose.runOnIdle { actual = 0; id = "second" }
        assertEquals(0f, slider.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current, 1f)
    }
}
