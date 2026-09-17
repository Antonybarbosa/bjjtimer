package com.example.bjjtimer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bjjtimer.audio.NoOpSoundPlayer
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
import java.util.Calendar
import java.util.Locale

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

    init {
        _uiState.update { it.withRecalculatedTimes() }
    }

    private fun updateUiState(transform: (TimerUiState) -> TimerUiState) {
        _uiState.update { current ->
            transform(current).withRecalculatedTimes()
        }
    }

    /**
     * Define o tempo da luta em minutos (1, 3, 4, 5)
     */
    fun setFightMinutes(minutes: Int) {
        val wasRunning = _uiState.value.isRunning
        timerJob?.cancel()
        timerJob = null

        val seconds = minutes * 60
        updateUiState {
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
        updateUiState {
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
        updateUiState {
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
        updateUiState {
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
                updateUiState {
                    it.copy(
                        phase = TimerPhase.PREPARATION,
                        isRunning = true,
                        isPaused = false,
                        currentRound = 1,
                        remainingSeconds = prepSec,
                        totalSecondsInPhase = prepSec
                    )
                }
                if (state.settings.soundEnabled && state.settings.startSoundEnabled) {
                    soundAlertManager.playPrepBeep()
                }
            } else {
                startFightPhase(round = 1)
                return
            }
        } else {
            // Retomando de onde pausou
            updateUiState { it.copy(isRunning = true, isPaused = false) }
        }

        launchTimerLoop()
    }

    fun pauseTimer() {
        timerJob?.cancel()
        timerJob = null
        updateUiState { it.copy(isRunning = false, isPaused = true) }
    }

    fun resetTimer() {
        timerJob?.cancel()
        timerJob = null
        val settings = _uiState.value.settings
        updateUiState {
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

    fun skipPhase() {
        timerJob?.cancel()
        timerJob = null
        val state = _uiState.value
        when (state.phase) {
            TimerPhase.IDLE, TimerPhase.PREPARATION -> {
                startFightPhase(state.currentRound)
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
            TimerPhase.FINISHED -> {
                resetTimer()
            }
        }
    }

    fun skipToNextPhase() = skipPhase()

    fun previousRound() {
        timerJob?.cancel()
        timerJob = null
        val state = _uiState.value
        val prevRound = (state.currentRound - 1).coerceAtLeast(1)
        startFightPhase(prevRound)
    }

    fun toggleSound() {
        updateUiState {
            val current = it.settings.soundEnabled
            it.copy(settings = it.settings.copy(soundEnabled = !current))
        }
    }

    fun toggleVibration() {
        updateUiState {
            val current = it.settings.vibrationEnabled
            it.copy(settings = it.settings.copy(vibrationEnabled = !current))
        }
    }

    fun toggleStartSound() {
        updateUiState {
            val current = it.settings.startSoundEnabled
            it.copy(settings = it.settings.copy(startSoundEnabled = !current))
        }
    }

    fun toggleCountdownSound() {
        updateUiState {
            val current = it.settings.countdownSoundEnabled
            it.copy(settings = it.settings.copy(countdownSoundEnabled = !current))
        }
    }

    fun toggleIntervalSound() {
        updateUiState {
            val current = it.settings.intervalSoundEnabled
            it.copy(settings = it.settings.copy(intervalSoundEnabled = !current))
        }
    }

    // ==========================================
    // TRANSIÇÕES DE FASE
    // ==========================================

    private fun startFightPhase(round: Int) {
        val settings = _uiState.value.settings
        updateUiState {
            it.copy(
                phase = TimerPhase.FIGHT,
                isRunning = true,
                isPaused = false,
                currentRound = round,
                remainingSeconds = settings.fightDurationSec,
                totalSecondsInPhase = settings.fightDurationSec
            )
        }

        if (settings.soundEnabled && settings.startSoundEnabled) {
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

        updateUiState {
            it.copy(
                phase = TimerPhase.REST,
                isRunning = true,
                isPaused = false,
                remainingSeconds = settings.restDurationSec,
                totalSecondsInPhase = settings.restDurationSec
            )
        }

        if (settings.soundEnabled && settings.intervalSoundEnabled) {
            soundAlertManager.playRoundEnd()
        }

        launchTimerLoop()
    }

    private fun finishWorkout() {
        timerJob?.cancel()
        timerJob = null
        val sound = _uiState.value.settings.soundEnabled
        updateUiState {
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
                    updateUiState {
                        it.copy(
                            remainingSeconds = newRemaining,
                            totalElapsedWorkoutSec = newElapsed
                        )
                    }

                    // Alertas intermediários de contagem regressiva
                    if (state.phase == TimerPhase.FIGHT) {
                        if (newRemaining == state.settings.warningSec) {
                            // Alerta principal dos 10 segundos finais (Wood-Clap duplo de tatame + vibração)
                            if (state.settings.soundEnabled && state.settings.countdownSoundEnabled) {
                                soundAlertManager.playTenSecondsWarning()
                            }
                        } else if (newRemaining in 1 until state.settings.warningSec) {
                            // Bips sonoros de contagem regressiva a cada segundo (9 até 1)
                            if (state.settings.soundEnabled && state.settings.countdownSoundEnabled) {
                                soundAlertManager.playCountdownTick(newRemaining)
                            }
                        }
                    } else if (state.phase == TimerPhase.PREPARATION && newRemaining in 1..3) {
                        if (state.settings.soundEnabled && state.settings.startSoundEnabled) {
                            soundAlertManager.playPrepBeep()
                        }
                    } else if (state.phase == TimerPhase.REST && newRemaining in 1..3) {
                        if (state.settings.soundEnabled && state.settings.intervalSoundEnabled) {
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

    // ==========================================
    // CÁLCULOS DE DURAÇÃO TOTAL E FIM DO TREINO
    // ==========================================

    companion object {
        fun calculateTotalWorkoutDuration(fightSec: Int, totalRounds: Int, restSec: Int, prepSec: Int): Int {
            val fights = totalRounds * fightSec
            val rests = if (totalRounds > 1) (totalRounds - 1) * restSec else 0
            return prepSec + fights + rests
        }

        fun calculateRemainingWorkoutDuration(
            phase: TimerPhase,
            remainingInPhase: Int,
            currentRound: Int,
            totalRounds: Int,
            fightSec: Int,
            restSec: Int,
            prepSec: Int
        ): Int {
            return when (phase) {
                TimerPhase.IDLE -> calculateTotalWorkoutDuration(fightSec, totalRounds, restSec, prepSec)
                TimerPhase.PREPARATION -> {
                    val fights = totalRounds * fightSec
                    val rests = if (totalRounds > 1) (totalRounds - 1) * restSec else 0
                    remainingInPhase + fights + rests
                }
                TimerPhase.FIGHT -> {
                    val remainingFightsAfterThis = (totalRounds - currentRound).coerceAtLeast(0) * fightSec
                    val remainingRestsAfterThis = (totalRounds - currentRound).coerceAtLeast(0) * restSec
                    remainingInPhase + remainingFightsAfterThis + remainingRestsAfterThis
                }
                TimerPhase.REST -> {
                    val roundsLeft = (totalRounds - currentRound).coerceAtLeast(0)
                    val fightsLeft = roundsLeft * fightSec
                    val restsLeft = if (roundsLeft > 1) (roundsLeft - 1) * restSec else 0
                    remainingInPhase + fightsLeft + restsLeft
                }
                TimerPhase.FINISHED -> 0
            }
        }

        fun formatEstimatedEndTime(remainingSec: Int): String {
            val cal = Calendar.getInstance()
            cal.add(Calendar.SECOND, remainingSec)
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val min = cal.get(Calendar.MINUTE)
            return String.format(Locale.getDefault(), "%02d:%02d", hour, min)
        }
    }

    private fun TimerUiState.withRecalculatedTimes(): TimerUiState {
        val total = calculateTotalWorkoutDuration(
            fightSec = settings.fightDurationSec,
            totalRounds = totalRounds,
            restSec = settings.restDurationSec,
            prepSec = settings.preparationSec
        )
        val remaining = calculateRemainingWorkoutDuration(
            phase = phase,
            remainingInPhase = remainingSeconds,
            currentRound = currentRound,
            totalRounds = totalRounds,
            fightSec = settings.fightDurationSec,
            restSec = settings.restDurationSec,
            prepSec = settings.preparationSec
        )
        val endTime = formatEstimatedEndTime(remaining)
        return this.copy(
            totalWorkoutDurationSec = total,
            remainingWorkoutDurationSec = remaining,
            estimatedEndTimeFormatted = endTime
        )
    }
}
