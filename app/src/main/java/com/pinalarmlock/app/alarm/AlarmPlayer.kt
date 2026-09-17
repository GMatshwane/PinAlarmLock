package com.pinalarmlock.app.alarm

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class AlarmPlayer(private val context: Context) {
    private var toneGenerator: ToneGenerator? = null
    private val handler = Handler(Looper.getMainLooper())
    private val stopRunnable = Runnable { stop() }

    fun start() {
        stop()
        val generator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        toneGenerator = generator
        generator.startTone(
            ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK,
            AlarmDuration.ALARM_DURATION_MS.toInt(),
        )
        vibrate()
        handler.postDelayed(stopRunnable, AlarmDuration.ALARM_DURATION_MS)
    }

    fun stop() {
        handler.removeCallbacks(stopRunnable)
        runCatching {
            toneGenerator?.stopTone()
            toneGenerator?.release()
        }
        toneGenerator = null
        runCatching { vibrator()?.cancel() }
    }

    private fun vibrate() {
        val vibrator = vibrator() ?: return
        if (!vibrator.hasVibrator()) return
        val effect =
            VibrationEffect.createWaveform(
                longArrayOf(
                    0, 500, 250, 500, 250, 500, 250, 500, 250, 500,
                    250, 500, 250, 500, 250, 500, 250, 500, 250, 500,
                ),
                -1,
            )
        runCatching { vibrator.vibrate(effect) }
    }

    private fun vibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            context.getSystemService(Vibrator::class.java)
        }
    }
}
