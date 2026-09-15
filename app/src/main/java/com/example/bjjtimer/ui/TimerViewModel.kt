package com.example.bjjtimer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bjjtimer.audio.SoundAlertManager
import com.example.bjjtimer.audio.SoundPlayer
import com.example.bjjtimer.model.TimerPhase
import com.example.bjjtimer.model.TimerSettings
import com.example.bjjtimer.model.TimerUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TimerViewModel @JvmOverloads constructor(
    application: Application,
    private val soundAlertManager: SoundPlayer = SoundAlertManager(application)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(
        TimerUiState(
            phase = TimerPhase.IDLE,
            isRunning = false,
            isPaused = false,
            currentRound = 1,
            totalRounds = 1,
            remainingSeconds = 300,
            totalSecondsInPhase = 300,
            selectedMinutes = 5,
            settings = TimerSettings(
                fightDurationSec = 300,
                restDurationSec = 60,
                totalRounds = 1,
                preparationSec = 5,
                warningSec = 10,
                soundEnabled = true,
                vibrationEnabled = true
            )
        )
    )
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    /**
     * Define o tempo da luta em minutos (1, 3, 4, 5)
     */
    fun setFightMinutes(minutes: Int) {
        val wasRunning = _uiState.value.isRunning
        timerJob?.cancel()
        timerJob = null

        val seconds = minutes * 60
        _uiState.update {
            val newSettings = it.settings.copy(fightDurationSec = seconds)
            it.copy(
                phase = TimerPhase.IDLE,
                isRunning = false,
                isPaused = false,
                currentRound = 1,
                remainingSeconds = seconds,
                totalSecondsInPhase = seconds,
                totalElapsedWorkoutSec = 0,
                settings = newSettings,
                selectedMinutes = minutes
            )
        }

        if (wasRunning) {
            startTimer()
        }
    }

    /**
     * Incrementa o número de repetições / rounds (+1)
     */
    fun incrementRounds() {
        _uiState.update {
            val nextRounds = if (it.totalRounds >= 50) 1 else it.totalRounds + 1
            val updatedSettings = it.settings.copy(totalRounds = nextRounds)
            it.copy(
                totalRounds = nextRounds,
                settings = updatedSettings
            )
        }
    }

    /**
     * Retorna o número de rounds para 1 (quando segura o botão)
     */
    fun resetRoundsToOne() {
        _uiState.update {
            val updatedSettings = it.settings.copy(totalRounds = 1)
            it.copy(
                totalRounds = 1,
                currentRound = 1,
                settings = updatedSettings
            )
        }
    }

    /**
     * Ajusta o tempo de intervalo entre rounds (em segundos)
     */
    fun setRestDuration(restSec: Int) {
        val validRest = restSec.coerceIn(5, 600)
        _uiState.update {
            val updated = it.settings.copy(restDurationSec = validRest)
            it.copy(
                settings = updated,
                totalSecondsInPhase = if (it.phase == TimerPhase.REST) validRest else it.totalSecondsInPhase,
                remainingSeconds = if (it.phase == TimerPhase.REST && !it.isRunning) validRest else it.remainingSeconds
            )
        }
    }

    fun adjustRestDuration(deltaSec: Int) {
        val currentRest = _uiState.value.settings.restDurationSec
        setRestDuration(currentRest + deltaSec)
    }

    fun togglePlayPause() {
        val current = _uiState.value
        if (current.isRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    fun startTimer() {
        if (_uiState.value.isRunning) return

        val state = _uiState.value
        if (state.phase == TimerPhase.IDLE || state.phase == TimerPhase.FINISHED) {
            val prepSec = state.settings.preparationSec
            if (prepSec > 0) {
                _uiState.update {
                    it.copy(
                        phase = TimerPhase.PREPARATION,
                        isRunning = true,
                        isPaused = false,
                        currentRound = 1,
                        remainingSeconds = prepSec,
                        totalSecondsInPhase = prepSec
                    )
                }
                if (state.settings.soundEnabled) {
                    soundAlertManager.playPrepBeep()
                }
            } else {
                startFightPhase(round = 1)
                return
            }
        } else {
            // Retomando de onde pausou
            _uiState.update { it.copy(isRunning = true, isPaused = false) }
        }

        launchTimerLoop()
    }

    fun pauseTimer() {
        timerJob?.cancel()
        timerJob = null
        _uiState.update { it.copy(isRunning = false, isPaused = true) }
    }

    fun resetTimer() {
        timerJob?.cancel()
        timerJob = null
        val settings = _uiState.value.settings
        _uiState.update {
            it.copy(
                phase = TimerPhase.IDLE,
                isRunning = false,
                isPaused = false,
                currentRound = 1,
                remainingSeconds = settings.fightDurationSec,
                totalSecondsInPhase = settings.fightDurationSec,
                totalElapsedWorkoutSec = 0
            )
        }
    }

    fun skipToNextPhase() {
        val current = _uiState.value
        when (current.phase) {
            TimerPhase.PREPARATION -> {
                startFightPhase(current.currentRound)
            }
            TimerPhase.FIGHT -> {
                if (current.currentRound >= current.totalRounds) {
                    finishWorkout()
                } else {
                    startRestPhase()
                }
            }
            TimerPhase.REST -> {
                val nextRound = current.currentRound + 1
                if (nextRound <= current.totalRounds) {
                    startFightPhase(nextRound)
                } else {
                    finishWorkout()
                }
            }
            TimerPhase.IDLE, TimerPhase.FINISHED -> {
                startTimer()
            }
        }
    }

    fun previousRound() {
        val current = _uiState.value
        if (current.currentRound > 1) {
            val prevRound = current.currentRound - 1
            startFightPhase(prevRound)
        } else {
            resetTimer()
        }
    }

    fun toggleSound() {
        _uiState.update {
            val updated = it.settings.copy(soundEnabled = !it.settings.soundEnabled)
            it.copy(settings = updated)
        }
    }

    fun toggleVibration() {
        _uiState.update {
            val updated = it.settings.copy(vibrationEnabled = !it.settings.vibrationEnabled)
            it.copy(settings = updated)
        }
    }

    private fun startFightPhase(round: Int) {
        val settings = _uiState.value.settings
        _uiState.update {
            it.copy(
                phase = TimerPhase.FIGHT,
                isRunning = true,
                isPaused = false,
                currentRound = round,
                remainingSeconds = settings.fightDurationSec,
                totalSecondsInPhase = settings.fightDurationSec
            )
        }

        if (settings.soundEnabled) {
            soundAlertManager.playFightStart()
        }

        launchTimerLoop()
    }

    private fun startRestPhase() {
        val settings = _uiState.value.settings
        if (settings.restDurationSec <= 0) {
            val nextRound = _uiState.value.currentRound + 1
            if (nextRound <= _uiState.value.totalRounds) {
                startFightPhase(nextRound)
            } else {
                finishWorkout()
            }
            return
        }

        _uiState.update {
            it.copy(
                phase = TimerPhase.REST,
                isRunning = true,
                isPaused = false,
                remainingSeconds = settings.restDurationSec,
                totalSecondsInPhase = settings.restDurationSec
            )
        }

        if (settings.soundEnabled) {
            soundAlertManager.playRoundEnd()
        }

        launchTimerLoop()
    }

    private fun finishWorkout() {
        timerJob?.cancel()
        timerJob = null
        val sound = _uiState.value.settings.soundEnabled
        _uiState.update {
            it.copy(
                phase = TimerPhase.FINISHED,
                isRunning = false,
                isPaused = false,
                remainingSeconds = 0
            )
        }
        if (sound) {
            soundAlertManager.playWorkoutComplete()
        }
    }

    private fun launchTimerLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val state = _uiState.value
                if (!state.isRunning) break

                val newRemaining = state.remainingSeconds - 1
                val newElapsed = state.totalElapsedWorkoutSec + 1

                if (newRemaining > 0) {
                    _uiState.update {
                        it.copy(
                            remainingSeconds = newRemaining,
                            totalElapsedWorkoutSec = newElapsed
                        )
                    }

                    // Alertas intermediários de contagem regressiva
                    if (state.phase == TimerPhase.FIGHT) {
                        if (newRemaining == state.settings.warningSec) {
                            // Alerta principal dos 10 segundos finais (Wood-Clap duplo de tatame + vibração)
                            if (state.settings.soundEnabled) {
                                soundAlertManager.playTenSecondsWarning()
                            }
                        } else if (newRemaining in 1 until state.settings.warningSec) {
                            // Bips sonoros de contagem regressiva a cada segundo (9 até 1)
                            if (state.settings.soundEnabled) {
                                soundAlertManager.playCountdownTick(newRemaining)
                            }
                        }
                    } else if ((state.phase == TimerPhase.PREPARATION || state.phase == TimerPhase.REST) && newRemaining in 1..3) {
                        if (state.settings.soundEnabled) {
                            soundAlertManager.playPrepBeep()
                        }
                    }
                } else {
                    when (state.phase) {
                        TimerPhase.PREPARATION -> {
                            startFightPhase(round = 1)
                        }
                        TimerPhase.FIGHT -> {
                            if (state.currentRound >= state.totalRounds) {
                                finishWorkout()
                            } else {
                                startRestPhase()
                            }
                        }
                        TimerPhase.REST -> {
                            val nextRound = state.currentRound + 1
                            if (nextRound <= state.totalRounds) {
                                startFightPhase(nextRound)
                            } else {
                                finishWorkout()
                            }
                        }
                        TimerPhase.IDLE, TimerPhase.FINISHED -> {
                            break
                        }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
