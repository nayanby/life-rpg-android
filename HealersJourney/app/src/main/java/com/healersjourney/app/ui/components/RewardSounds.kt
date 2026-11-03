package com.healersjourney.app.ui.components

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import com.healersjourney.app.R

/** Owns only transient sound effects; release with the composing screen. */
internal class RewardSounds(context: Context) {
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val pool = SoundPool.Builder().setMaxStreams(2)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
    private val ready = mutableSetOf<Int>()
    private val pending = mutableSetOf<Int>()
    private var closed = false
    private val task: Int
    private val unlock: Int
    init {
        pool.setOnLoadCompleteListener { _, id, status ->
            synchronized(this) {
                if (!closed && status == 0) {
                    ready.add(id)
                    if (pending.remove(id)) playReady(id)
                }
            }
        }
        task = pool.load(context, R.raw.task_complete, 1)
        unlock = pool.load(context, R.raw.item_unlock, 1)
    }
    private fun playReady(id: Int) {
        if (audio.ringerMode == AudioManager.RINGER_MODE_NORMAL) pool.play(id, .65f, .65f, 1, 0, 1f)
    }
    @Synchronized fun play(unlocked: Boolean) {
        if (closed) return
        val id = if (unlocked) unlock else task
        if (id in ready) playReady(id) else pending.add(id)
    }
    @Synchronized fun close() {
        closed = true
        pending.clear()
        pool.release()
    }
}
