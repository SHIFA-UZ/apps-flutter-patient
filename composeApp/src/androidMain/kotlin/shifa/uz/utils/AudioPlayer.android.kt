package shifa.uz.utils

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import shifa.uz.data.api.httpLogger

private class AndroidAudioPlayer : AudioPlayer {
    override var playback: Playback? by mutableStateOf(null)
        private set

    private var player: MediaPlayer? = null

    override fun toggle(url: String) {
        val current = playback
        val p = player
        if (p != null && current?.url == url) {
            if (current.playing) p.pause() else p.start()
            playback = current.copy(playing = !current.playing)
            return
        }
        release()
        playback = Playback(url, playing = true)
        player =
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build(),
                )
                setOnPreparedListener {
                    runCatching {
                        it.start()
                        playback = playback?.copy(duration = it.duration / 1000)
                    }.onFailure { release() }
                }
                setOnCompletionListener { release() }
                setOnErrorListener { _, what, extra ->
                    httpLogger.log("FAILED to play $url: $what/$extra")
                    release()
                    true
                }
                runCatching {
                    setDataSource(url)
                    prepareAsync()
                }.onFailure {
                    httpLogger.log("FAILED to play $url: $it")
                    release()
                }
            }
    }

    // getCurrentPosition throws once the player left a playable state (an error, or just as it ends).
    override fun tick() {
        val p = player ?: return
        runCatching { playback = playback?.copy(position = p.currentPosition / 1000) }.onFailure { release() }
    }

    override fun release() {
        player?.release()
        player = null
        playback = null
    }
}

actual fun createAudioPlayer(): AudioPlayer = AndroidAudioPlayer()
