package com.example.bjjtimer.model

enum class TimerPhase(val displayName: String, val subtitle: String) {
    IDLE("PRONTO", "Selecione o tempo e inicie"),
    PREPARATION("PREPARE-SE", "Entrem em posição"),
    FIGHT("COMBATE!", "Tempo de luta / rola"),
    REST("INTERVALO", "Descanso / Troca de duplas"),
    FINISHED("OSS! FINALIZADO", "Treino concluído!")
}

data class TimerSettings(
    val fightDurationSec: Int = 300, // 5 min padrão
    val restDurationSec: Int = 60,   // 1 min intervalo padrão
    val totalRounds: Int = 1,        // Sempre inicia no 1
    val preparationSec: Int = 5,
    val warningSec: Int = 10,        // aviso aos 10s finais
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true
)

data class TimerUiState(
    val phase: TimerPhase = TimerPhase.IDLE,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val currentRound: Int = 1,
    val totalRounds: Int = 1,        // Sempre inicia no 1
    val remainingSeconds: Int = 300,
    val totalSecondsInPhase: Int = 300,
    val totalElapsedWorkoutSec: Int = 0,
    val settings: TimerSettings = TimerSettings(),
    val selectedMinutes: Int = 5
)

object TimerDefaults {
    val availableMinutes = listOf(1, 3, 4, 5)
    val availableIntervalsSec = listOf(15, 30, 45, 60, 90, 120, 180)
}
