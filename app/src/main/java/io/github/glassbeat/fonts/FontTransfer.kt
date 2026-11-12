package io.github.glassbeat.fonts

import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Independent blocking worker: cancellation closes the socket even while read() is blocked. */
internal object FontTransfer {
    private val workers = Executors.newCachedThreadPool { task -> Thread(task, "font-transfer").apply { isDaemon = true } }
    suspend fun download(url: String, target: File, limit: Long, progress: (Float) -> Unit, open: (String) -> HttpURLConnection = { URL(it).openConnection() as HttpURLConnection }): Unit = suspendCancellableCoroutine { continuation ->
        val active = AtomicReference<HttpURLConnection?>()
        val future = workers.submit {
            var succeeded = false
            var failure: Exception? = null
            try {
                val connection = open(url)
                active.set(connection)
                if (!continuation.isActive) return@submit
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("Accept-Encoding", "identity")
                require(connection.responseCode == 200) { "字体服务器暂不可用" }
                val length = connection.contentLengthLong
                require(length <= limit) { "字体文件超出大小限制" }
                connection.inputStream.use { input -> target.outputStream().use { output ->
                    val buffer = ByteArray(32 * 1024)
                    var count = 0L
                    while (continuation.isActive) {
                        val n = input.read(buffer)
                        if (!continuation.isActive) throw kotlinx.coroutines.CancellationException()
                        if (n < 0) break
                        count += n
                        require(count <= limit) { "字体文件超出大小限制" }
                        output.write(buffer, 0, n)
                        progress(if (length > 0) count.toFloat() / length else -1f)
                    }
                } }
                succeeded = continuation.isActive
            } catch (e: Exception) {
                failure = e
            } finally {
                active.getAndSet(null)?.disconnect()
                if (!succeeded) target.delete()
            }
            if (continuation.isActive) {
                val error = failure
                if (error != null) continuation.resumeWithException(error) else continuation.resume(Unit)
            }
        }
        continuation.invokeOnCancellation {
            future.cancel(true)
            workers.execute { active.getAndSet(null)?.disconnect() }
        }
    }
}
