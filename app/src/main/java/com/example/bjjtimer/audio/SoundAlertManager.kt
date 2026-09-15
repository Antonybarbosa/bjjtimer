package com.example.bjjtimer.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

interface SoundPlayer {
    fun playFightStart()
    fun playRoundEnd()
    fun playTenSecondsWarning()
    fun playCountdownTick(remainingSec: Int)
    fun playPrepBeep()
    fun playWorkoutComplete()
}

class NoOpSoundPlayer : SoundPlayer {
    override fun playFightStart() {}
    override fun playRoundEnd() {}
    override fun playTenSecondsWarning() {}
    override fun playCountdownTick(remainingSec: Int) {}
    override fun playPrepBeep() {}
    override fun playWorkoutComplete() {}
}

class SoundAlertManager(private val context: Context) : SoundPlayer {

    private val audioScope = CoroutineScope(Dispatchers.Default)

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    /**
     * Toca o sino duplo de ringue/tatame (Início do Rola / Combate)
     */
    override fun playFightStart() {
        audioScope.launch {
            playBellTone()
            delay(280)
            playBellTone()
        }
        vibrateFightStart()
    }

    /**
     * Toca a buzina/gongo longo de tatame (Fim do Rola / Descanso)
     */
    override fun playRoundEnd() {
        audioScope.launch {
            playGymBuzzer(durationMs = 950)
        }
        vibrateRoundEnd()
    }

    /**
     * Alerta dos últimos 10 segundos: Duplo toque característico de madeira (Wood-Clap de tatame) + vibração
     */
    override fun playTenSecondsWarning() {
        audioScope.launch {
            playWoodClap()
            delay(180)
            playWoodClap()
        }
        vibrateWarning()
    }

    /**
     * Bip audível para cada segundo da contagem regressiva final (9 até 1)
     * Nos últimos 3 segundos (3, 2, 1), o tom fica mais agudo com vibração
     */
    override fun playCountdownTick(remainingSec: Int) {
        audioScope.launch {
            if (remainingSec in 1..3) {
                // Últimos 3 segundos: bip agudo (1200Hz) de urgência
                playHighBeep(durationMs = 150, freq = 1200.0)
            } else {
                // De 4 a 9 segundos: bip claro de contagem (850Hz)
                playHighBeep(durationMs = 120, freq = 850.0)
            }
        }
        if (remainingSec in 1..3) {
            vibrateShortTick()
        }
    }

    /**
     * Beep curto para contagem regressiva de preparação (3, 2, 1...)
     */
    override fun playPrepBeep() {
        audioScope.launch {
            playHighBeep(durationMs = 110, freq = 750.0)
        }
    }

    /**
     * Som de finalização total do treino (Sino duplo + buzina final)
     */
    override fun playWorkoutComplete() {
        audioScope.launch {
            playBellTone()
            delay(250)
            playBellTone()
            delay(300)
            playGymBuzzer(durationMs = 1200)
        }
        vibrateRoundEnd()
    }

    // ==========================================
    // SÍNTESE DE ÁUDIO NATIVA (Zero dependências)
    // ==========================================

    private fun playBellTone() {
        val sampleRate = 44100
        val durationMs = 700
        val numSamples = (durationMs * sampleRate) / 1000
        val samples = ShortArray(numSamples)

        val fundamental = 640.0 // Frequência harmônica de sino metálico
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val decay = exp(-4.5 * t)

            val s1 = sin(2.0 * PI * fundamental * t)
            val s2 = 0.6 * sin(2.0 * PI * (fundamental * 2.15) * t)
            val s3 = 0.35 * sin(2.0 * PI * (fundamental * 3.7) * t)

            val mixed = (s1 + s2 + s3) * decay * 0.75
            samples[i] = (mixed * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        playPcmTrack(samples, sampleRate)
    }

    private fun playGymBuzzer(durationMs: Int) {
        val sampleRate = 44100
        val numSamples = (durationMs * sampleRate) / 1000
        val samples = ShortArray(numSamples)

        val buzzerFreq = 220.0 // Buzina grave estrondosa de ginásio
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = when {
                i < 400 -> i.toDouble() / 400.0
                i > numSamples - 600 -> (numSamples - i).toDouble() / 600.0
                else -> 1.0
            }

            val h1 = sin(2.0 * PI * buzzerFreq * t)
            val h2 = 0.7 * sin(2.0 * PI * (buzzerFreq * 2.0) * t)
            val h3 = 0.5 * sin(2.0 * PI * (buzzerFreq * 3.0) * t)
            val h4 = 0.4 * sin(2.0 * PI * (buzzerFreq * 5.0) * t)

            val mixed = (h1 + h2 + h3 + h4) * envelope * 0.55
            samples[i] = (mixed * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        playPcmTrack(samples, sampleRate)
    }

    private fun playWoodClap() {
        val sampleRate = 44100
        val durationMs = 120
        val numSamples = (durationMs * sampleRate) / 1000
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val decay = exp(-35.0 * t)
            val s = (sin(2.0 * PI * 880.0 * t) + 0.6 * sin(2.0 * PI * 1320.0 * t)) * decay * 0.9
            samples[i] = (s * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        playPcmTrack(samples, sampleRate)
    }

    private fun playHighBeep(durationMs: Int, freq: Double) {
        val sampleRate = 44100
        val numSamples = (durationMs * sampleRate) / 1000
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = when {
                i < 200 -> i.toDouble() / 200.0
                i > numSamples - 200 -> (numSamples - i).toDouble() / 200.0
                else -> 1.0
            }
            val sample = sin(2.0 * PI * freq * t) * envelope * 0.95
            samples[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        playPcmTrack(samples, sampleRate)
    }

    private fun playPcmTrack(samples: ShortArray, sampleRate: Int) {
        try {
            // USAGE_MEDIA permite que o botão físico de volume do celular controle o som
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val track = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(samples, 0, samples.size)
            track.play()

            // Liberação segura e garantida da memória após o áudio terminar de tocar
            val durationMs = (samples.size * 1000L) / sampleRate
            audioScope.launch {
                delay(durationMs + 120)
                try {
                    track.stop()
                    track.release()
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ==========================================
    // VIBRAÇÃO HÁPTICA
    // ==========================================

    private fun vibrateFightStart() {
        val vib = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 150, 100, 300)
            val amplitudes = intArrayOf(0, 200, 0, 255)
            vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(longArrayOf(0, 150, 100, 300), -1)
        }
    }

    private fun vibrateRoundEnd() {
        val vib = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createOneShot(700, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(700)
        }
    }

    private fun vibrateWarning() {
        val vib = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timings = longArrayOf(0, 100, 80, 100)
            val amplitudes = intArrayOf(0, 180, 0, 180)
            vib.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(longArrayOf(0, 100, 80, 100), -1)
        }
    }

    private fun vibrateShortTick() {
        val vib = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createOneShot(70, 160))
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(70)
        }
    }
}
