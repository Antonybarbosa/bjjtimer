package com.example.bjjtimer

import android.app.Application
import com.example.bjjtimer.audio.NoOpSoundPlayer
import com.example.bjjtimer.model.TimerPhase
import com.example.bjjtimer.ui.TimerViewModel
import com.example.bjjtimer.ui.formatTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TimerViewModelTest {

    private lateinit var viewModel: TimerViewModel

    private class TestApplication : Application()

    @Before
    fun setup() {
        val app = TestApplication()
        viewModel = TimerViewModel(app, NoOpSoundPlayer())
    }

    @Test
    fun testInitialState() {
        val state = viewModel.uiState.value
        assertEquals(TimerPhase.IDLE, state.phase)
        assertFalse(state.isRunning)
        assertFalse(state.isPaused)
        assertEquals(1, state.currentRound)
        assertEquals(1, state.totalRounds) // Agora inicia no 1
        assertEquals(300, state.remainingSeconds) // 5 min padrão
        assertEquals(60, state.settings.restDurationSec) // 1 min intervalo padrão
    }

    @Test
    fun testSetFightMinutes() {
        viewModel.setFightMinutes(1)
        assertEquals(60, viewModel.uiState.value.remainingSeconds)
        assertEquals(1, viewModel.uiState.value.selectedMinutes)

        viewModel.setFightMinutes(2)
        assertEquals(120, viewModel.uiState.value.remainingSeconds)
        assertEquals(2, viewModel.uiState.value.selectedMinutes)

        viewModel.setFightMinutes(3)
        assertEquals(180, viewModel.uiState.value.remainingSeconds)
        assertEquals(3, viewModel.uiState.value.selectedMinutes)

        viewModel.setFightMinutes(4)
        assertEquals(240, viewModel.uiState.value.remainingSeconds)
        assertEquals(4, viewModel.uiState.value.selectedMinutes)

        viewModel.setFightMinutes(5)
        assertEquals(300, viewModel.uiState.value.remainingSeconds)
        assertEquals(5, viewModel.uiState.value.selectedMinutes)
    }

    @Test
    fun testIncrementAndResetRounds() {
        assertEquals(1, viewModel.uiState.value.totalRounds)
        viewModel.incrementRounds()
        assertEquals(2, viewModel.uiState.value.totalRounds)
        viewModel.incrementRounds()
        assertEquals(3, viewModel.uiState.value.totalRounds)

        viewModel.resetRoundsToOne()
        assertEquals(1, viewModel.uiState.value.totalRounds)
    }

    @Test
    fun testSetRestDuration() {
        viewModel.setRestDuration(90)
        assertEquals(90, viewModel.uiState.value.settings.restDurationSec)

        viewModel.adjustRestDuration(-15)
        assertEquals(75, viewModel.uiState.value.settings.restDurationSec)
    }

    @Test
    fun testToggles() {
        assertTrue(viewModel.uiState.value.settings.soundEnabled)
        viewModel.toggleSound()
        assertFalse(viewModel.uiState.value.settings.soundEnabled)

        assertTrue(viewModel.uiState.value.settings.vibrationEnabled)
        viewModel.toggleVibration()
        assertFalse(viewModel.uiState.value.settings.vibrationEnabled)

        // Toggles de sons específicos
        assertTrue(viewModel.uiState.value.settings.startSoundEnabled)
        viewModel.toggleStartSound()
        assertFalse(viewModel.uiState.value.settings.startSoundEnabled)

        assertTrue(viewModel.uiState.value.settings.countdownSoundEnabled)
        viewModel.toggleCountdownSound()
        assertFalse(viewModel.uiState.value.settings.countdownSoundEnabled)

        assertTrue(viewModel.uiState.value.settings.intervalSoundEnabled)
        viewModel.toggleIntervalSound()
        assertFalse(viewModel.uiState.value.settings.intervalSoundEnabled)
    }

    @Test
    fun testFormatTime() {
        assertEquals("05:00", formatTime(300))
        assertEquals("01:00", formatTime(60))
        assertEquals("00:45", formatTime(45))
        assertEquals("03:00", formatTime(180))
        assertEquals("04:00", formatTime(240))
    }

    @Test
    fun testCalculateWorkoutDuration() {
        // 1 round de 5 min (300s) + 5s prep, sem descanso subsequente = 305s
        val duration1Round = TimerViewModel.calculateTotalWorkoutDuration(
            fightSec = 300,
            totalRounds = 1,
            restSec = 60,
            prepSec = 5
        )
        assertEquals(305, duration1Round)

        // 5 rounds de 5 min (1500s) + 4 intervalos de 1 min (240s) + 5s prep = 1745s
        val duration5Rounds = TimerViewModel.calculateTotalWorkoutDuration(
            fightSec = 300,
            totalRounds = 5,
            restSec = 60,
            prepSec = 5
        )
        assertEquals(1745, duration5Rounds)
    }

    @Test
    fun testEstimatedEndTimeFormattedIsNotEmpty() {
        val state = viewModel.uiState.value
        assertTrue(state.estimatedEndTimeFormatted.isNotBlank())
        assertTrue(state.estimatedEndTimeFormatted.contains(":"))
    }
}
