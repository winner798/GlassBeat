package io.github.glassbeat

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.hypot

@RunWith(AndroidJUnit4::class)
class IconResourcesTest {
    @Test fun adaptiveIconLoadsAndArtworkStaysInsideMaskSafeZone() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val icon = context.packageManager.getApplicationIcon(context.packageName)
        assertTrue(icon is AdaptiveIconDrawable)
        icon as AdaptiveIconDrawable
        assertNotNull(icon.background); assertNotNull(icon.foreground)
        if (Build.VERSION.SDK_INT >= 33) assertNotNull(icon.monochrome)
        val density = context.resources.displayMetrics.density
        val size = (108 * density).toInt()
        val artwork = ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground)!!
        val foreground = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        artwork.setBounds(0, 0, size, size); artwork.draw(Canvas(foreground))
        val pixels = IntArray(size * size); foreground.getPixels(pixels, 0, size, 0, 0, size, size)
        var count = 0
        var radius = 0.0
        pixels.forEachIndexed { index, pixel ->
            if ((pixel ushr 24) > 32) {
                count++
                radius = maxOf(radius, hypot(index % size - size / 2.0, index / size - size / 2.0))
            }
        }
        assertTrue("Foreground should contain visible artwork", count > 100)
        assertTrue("Artwork should fit the adaptive-icon safe zone", radius <= 33.5 * density)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        icon.setBounds(0, 0, size, size); icon.draw(Canvas(bitmap))
        val directory = File(context.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        File(directory, "adaptive-icon.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        for (round in listOf(true, false)) {
            val previewSize = (72 * density).toInt()
            val preview = Bitmap.createBitmap(previewSize, previewSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(preview)
            val mask = android.graphics.Path().apply {
                val end = previewSize.toFloat()
                if (round) addCircle(end / 2, end / 2, end / 2, android.graphics.Path.Direction.CW)
                else addRoundRect(0f, 0f, end, end, end * .24f, end * .24f, android.graphics.Path.Direction.CW)
            }
            canvas.clipPath(mask)
            val overflow = (18 * density).toInt()
            for (resource in listOf(R.drawable.ic_launcher_background, R.drawable.ic_launcher_foreground)) {
                val layer = ContextCompat.getDrawable(context, resource)!!.mutate()
                layer.setBounds(-overflow, -overflow, previewSize + overflow, previewSize + overflow)
                layer.draw(canvas)
            }
            File(directory, if (round) "icon-circle.png" else "icon-rounded-square.png").outputStream().use {
                preview.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }

    }
}
