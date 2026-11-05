package io.github.glassbeat

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import io.github.glassbeat.ui.FavSodaApp

class MainActivity : ComponentActivity() {
    private val model: LibraryModel by viewModels()
    private var controller by mutableStateOf<MediaController?>(null)
    private var future: com.google.common.util.concurrent.ListenableFuture<MediaController>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) model.receive(intent)
        setContent { FavSodaApp(model, controller) }
    }
    override fun onStart() {
        super.onStart()
        val pending = MediaController.Builder(this, SessionToken(this, ComponentName(this, PlaybackService::class.java))).buildAsync()
        future = pending
        pending.addListener({
            if (future === pending) {
                try { controller = pending.get() } catch (_: Exception) { model.message = "播放服务连接失败，请重启应用。" }
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
