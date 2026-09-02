package com.example.bjjtimer.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

interface SoundPlayer {
    fun playFightStart()
    fun playRoundEnd()
    fun playTenSecondsWarning()
    fun playPrepBeep()
    fun playWorkoutComplete()
}

class NoOpSoundPlayer : SoundPlayer {
    override fun playFightStart() {}
    override fun playRoundEnd() {}
    override fun playTenSecondsWarning() {}
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
            kotlinx.coroutines.delay(280)
            playBellTone()
        }
        vibrateFightStart()
    }

    /**
     * Toca a buzina/gongo longo de tatame (Fim do Rola / Descanso)
     */
    override fun playRoundEnd() {
        audioScope.launch {
            playGymBuzzer(durationMs = 900)
        }
        vibrateRoundEnd()
    }

    /**
     * Toca o sinal de aviso dos últimos 10 segundos
     */
    override fun playTenSecondsWarning() {
        audioScope.launch {
            playHighBeep(durationMs = 120, freq = 950.0)
            kotlinx.coroutines.delay(160)
            playHighBeep(durationMs = 120, freq = 950.0)
        }
        vibrateWarning()
    }

    /**
     * Beep curto para contagem regressiva de preparação (3, 2, 1...)
     */
    override fun playPrepBeep() {
        audioScope.launch {
            playHighBeep(durationMs = 90, freq = 750.0)
        }
    }

    /**
     * Som de finalização total do treino
     */
    override fun playWorkoutComplete() {
        audioScope.launch {
            playBellTone()
            kotlinx.coroutines.delay(250)
            playBellTone()
            kotlinx.coroutines.delay(250)
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
            val decay = exp(-4.5 * t) // decaimento exponencial realista

            // Harmônicos metálicos típicos de gongo/sino
            val s1 = sin(2.0 * PI * fundamental * t)
            val s2 = 0.6 * sin(2.0 * PI * (fundamental * 2.15) * t)
            val s3 = 0.35 * sin(2.0 * PI * (fundamental * 3.7) * t)

            val mixed = (s1 + s2 + s3) * decay * 0.7
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
            // Envelope trapezoidal para evitar cliques no início e fim
            val envelope = when {
                i < 400 -> i.toDouble() / 400.0
                i > numSamples - 600 -> (numSamples - i).toDouble() / 600.0
                else -> 1.0
            }

            // Mistura de onda dente-de-serra e harmônicos ímpares
            val h1 = sin(2.0 * PI * buzzerFreq * t)
            val h2 = 0.7 * sin(2.0 * PI * (buzzerFreq * 2.0) * t)
            val h3 = 0.5 * sin(2.0 * PI * (buzzerFreq * 3.0) * t)
            val h4 = 0.4 * sin(2.0 * PI * (buzzerFreq * 5.0) * t)

            val mixed = (h1 + h2 + h3 + h4) * envelope * 0.45
            samples[i] = (mixed * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
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
            val sample = sin(2.0 * PI * freq * t) * envelope * 0.8
            samples[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        playPcmTrack(samples, sampleRate)
    }

    private fun playPcmTrack(samples: ShortArray, sampleRate: Int) {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
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

            // Libera a memória após término
            track.setNotificationMarkerPosition(samples.size)
            track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(t: AudioTrack?) {
                    t?.release()
                }

                override fun onPeriodicNotification(t: AudioTrack?) {}
            })
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
}
