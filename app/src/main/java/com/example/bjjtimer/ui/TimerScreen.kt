package com.example.bjjtimer.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bjjtimer.model.TimerDefaults
import com.example.bjjtimer.model.TimerPhase
import com.example.bjjtimer.model.TimerUiState
import com.example.bjjtimer.theme.BeltGold
import com.example.bjjtimer.theme.CardBackground
import com.example.bjjtimer.theme.CardBorder
import com.example.bjjtimer.theme.DarkBackground
import com.example.bjjtimer.theme.FightGreen
import com.example.bjjtimer.theme.FightGreenDark
import com.example.bjjtimer.theme.PausedGray
import com.example.bjjtimer.theme.PrepAmber
import com.example.bjjtimer.theme.PrepAmberDark
import com.example.bjjtimer.theme.RestRed
import com.example.bjjtimer.theme.RestRedDark
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    viewModel: TimerViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var showSettingsSheet by remember { mutableStateOf(false) }

    // Cores de fundo dinâmicas de tatame
    val targetBgColor = when {
        state.isPaused -> PausedGray.copy(alpha = 0.5f)
        state.phase == TimerPhase.FIGHT -> FightGreenDark
        state.phase == TimerPhase.REST -> RestRedDark
        state.phase == TimerPhase.PREPARATION -> PrepAmberDark
        else -> DarkBackground
    }

    val animatedBgColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = tween(durationMillis = 500),
        label = "BgAnimation"
    )

    val activeAccentColor = when {
        state.isPaused -> Color.White
        state.phase == TimerPhase.FIGHT -> FightGreen
        state.phase == TimerPhase.REST -> RestRed
        state.phase == TimerPhase.PREPARATION -> PrepAmber
        else -> BeltGold
    }

    AnimatedContent(
        targetState = state.isRunning,
        transitionSpec = {
            fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
        },
        label = "FullScreenTransition",
        modifier = modifier.fillMaxSize()
    ) { isRunning ->
        if (isRunning) {
            // =========================================================================
            // MODO FULL SCREEN EM EXECUÇÃO: SOBREPÕE TUDO, TOQUE PAUSA E VOLTA AO MODO ATUAL
            // =========================================================================
            FullScreenTimerDisplay(
                state = state,
                accentColor = activeAccentColor,
                backgroundColor = animatedBgColor,
                onTapToPause = { viewModel.pauseTimer() }
            )
        } else {
            // =========================================================================
            // MODO PARADO / PAUSADO / FINALIZADO: EXIBE O LAYOUT COM TODOS OS BOTÕES
            // =========================================================================
            Scaffold(
                containerColor = animatedBgColor,
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Topo: Título, Intervalo e Controles
                    TopHeaderSection(
                        state = state,
                        onToggleSound = { viewModel.toggleSound() },
                        onToggleVibration = { viewModel.toggleVibration() },
                        onOpenSettings = { showSettingsSheet = true }
                    )

                    // Centro: Retângulo do contador (tocar nele também inicia a contagem!)
                    BigFrameTimerDisplay(
                        state = state,
                        accentColor = activeAccentColor,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        onTapToStart = { viewModel.startTimer() }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Barra Inferior: 1m, 3m, 4m, 5m, +Round, Reset, Pular, Iniciar
                    StoppedLandscapeControls(
                        state = state,
                        accentColor = activeAccentColor,
                        onSelectMinutes = { viewModel.setFightMinutes(it) },
                        onIncrementRounds = { viewModel.incrementRounds() },
                        onResetRoundsToOne = { viewModel.resetRoundsToOne() },
                        onPlay = { viewModel.startTimer() },
                        onReset = { viewModel.resetTimer() },
                        onSkip = { viewModel.skipToNextPhase() }
                    )
                }
            }
        }
    }

    if (showSettingsSheet) {
        SettingsBottomSheet(
            state = state,
            onDismiss = { showSettingsSheet = false },
            onSetRestDuration = { viewModel.setRestDuration(it) },
            onAdjustRest = { viewModel.adjustRestDuration(it) },
            onToggleStartSound = { viewModel.toggleStartSound() },
            onToggleCountdownSound = { viewModel.toggleCountdownSound() },
            onToggleIntervalSound = { viewModel.toggleIntervalSound() }
        )
    }
}

/**
 * TELA CHEIA TOTAL (FULL SCREEN) QUANDO CONTANDO:
 * Sobrepõe toda a tela, dígitos gigantescos no máximo da tela e toque em qualquer ponto pausa
 */
@Composable
private fun FullScreenTimerDisplay(
    state: TimerUiState,
    accentColor: Color,
    backgroundColor: Color,
    onTapToPause: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .clickable { onTapToPause() }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Moldura com borda de alta visibilidade - toque em qualquer lugar pausa
        Card(
            onClick = onTapToPause,
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.95f)),
            border = androidx.compose.foundation.BorderStroke(4.dp, accentColor)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                val availableHeight = maxHeight.value
                val availableWidth = maxWidth.value

                // Dígitos em tamanho colossal aproveitando 100% da tela cheia
                val timerFontSize = min(
                    availableHeight * 0.72f,
                    availableWidth * 0.38f
                ).coerceIn(100f, 240f).sp

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Topo do Full Screen: Round, Previsão de Término e Fase
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = accentColor.copy(alpha = 0.25f),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, accentColor)
                        ) {
                            Text(
                                text = "ROUND ${state.currentRound} / ${state.totalRounds}",
                                color = accentColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                letterSpacing = 1.2.sp
                            )
                        }

                        // Previsão discreta de término do treino na tela cheia
                        if (state.estimatedEndTimeFormatted.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = CardBackground.copy(alpha = 0.85f),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, accentColor.copy(alpha = 0.6f))
                            ) {
                                Text(
                                    text = "🏁 FIM: ${state.estimatedEndTimeFormatted}",
                                    color = accentColor,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        Text(
                            text = state.phase.displayName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = accentColor,
                            letterSpacing = 1.5.sp
                        )
                    }

                    // DÍGITOS GIGANTESCOS DE TELA CHEIA
                    Text(
                        text = formatTime(state.remainingSeconds),
                        fontSize = timerFontSize,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        letterSpacing = (-3).sp,
                        lineHeight = timerFontSize,
                        maxLines = 1,
                        softWrap = false
                    )

                    // Rodapé do Full Screen: Barra de progresso e aviso de toque para pausar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val progress = if (state.totalSecondsInPhase > 0) {
                            (state.remainingSeconds.toFloat() / state.totalSecondsInPhase.toFloat()).coerceIn(0f, 1f)
                        } else 0f

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = accentColor,
                            trackColor = Color.DarkGray.copy(alpha = 0.5f),
                            strokeCap = StrokeCap.Round
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "👆 TOQUE EM QUALQUER LUGAR DA TELA PARA PAUSAR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.LightGray.copy(alpha = 0.8f),
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopHeaderSection(
    state: TimerUiState,
    onToggleSound: () -> Unit,
    onToggleVibration: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "🥋 BJJ TIMER",
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Indicador VISÍVEL do Tempo de Intervalo
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CardBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "INTERVALO: ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.LightGray
                    )
                    Text(
                        text = formatTime(state.settings.restDurationSec),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = RestRed
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Indicador de Término do Treino
            if (state.estimatedEndTimeFormatted.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CardBackground,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BeltGold.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🏁 TÉRMINO: ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.LightGray
                        )
                        Text(
                            text = state.estimatedEndTimeFormatted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BeltGold
                        )
                        Text(
                            text = " (${formatTime(state.remainingWorkoutDurationSec)})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Total: ${formatTime(state.totalElapsedWorkoutSec)}",
                fontSize = 12.sp,
                color = Color.LightGray
            )

            Spacer(modifier = Modifier.width(12.dp))

            IconButton(
                onClick = onToggleSound,
                modifier = Modifier
                    .size(32.dp)
                    .background(CardBackground, CircleShape)
                    .border(1.dp, CardBorder, CircleShape)
            ) {
                Text(
                    text = if (state.settings.soundEnabled) "🔊" else "🔇",
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = onToggleVibration,
                modifier = Modifier
                    .size(32.dp)
                    .background(CardBackground, CircleShape)
                    .border(1.dp, CardBorder, CircleShape)
            ) {
                Text(
                    text = if (state.settings.vibrationEnabled) "📳" else "📴",
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(32.dp)
                    .background(CardBackground, CircleShape)
                    .border(1.dp, CardBorder, CircleShape)
            ) {
                Text(text = "⚙️", fontSize = 14.sp)
            }
        }
    }
}

/**
 * Moldura retangular principal quando parado/pausado (toque inicia a contagem)
 */
@Composable
private fun BigFrameTimerDisplay(
    state: TimerUiState,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onTapToStart: () -> Unit
) {
    Card(
        onClick = onTapToStart,
        modifier = modifier.padding(vertical = 2.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground.copy(alpha = 0.92f)),
        border = androidx.compose.foundation.BorderStroke(3.dp, accentColor)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            val availableHeight = maxHeight.value
            val availableWidth = maxWidth.value

            val timerFontSize = min(
                availableHeight * 0.58f,
                availableWidth * 0.32f
            ).coerceIn(80f, 180f).sp

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceAround
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accentColor.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor)
                    ) {
                        Text(
                            text = "ROUND ${state.currentRound} / ${state.totalRounds}",
                            color = accentColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 3.dp),
                            letterSpacing = 1.sp
                        )
                    }

                    if (state.estimatedEndTimeFormatted.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = CardBackground,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BeltGold.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "🏁 TÉRMINO: ${state.estimatedEndTimeFormatted}",
                                color = BeltGold,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = if (state.isPaused) "PAUSADO" else state.phase.displayName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = accentColor,
                        letterSpacing = 1.sp
                    )
                }

                // DÍGITOS GIGANTES
                Text(
                    text = formatTime(state.remainingSeconds),
                    fontSize = timerFontSize,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    letterSpacing = (-2).sp,
                    lineHeight = timerFontSize,
                    maxLines = 1,
                    softWrap = false
                )

                // Barra fina de progresso + dica de toque
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val progress = if (state.totalSecondsInPhase > 0) {
                        (state.remainingSeconds.toFloat() / state.totalSecondsInPhase.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = accentColor,
                        trackColor = Color.DarkGray.copy(alpha = 0.5f),
                        strokeCap = StrokeCap.Round
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "👆 Toque no contador ou em INICIAR para começar",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

/**
 * Fileira única de botões na horizontal quando o cronômetro está parado
 */
@Composable
private fun StoppedLandscapeControls(
    state: TimerUiState,
    accentColor: Color,
    onSelectMinutes: (Int) -> Unit,
    onIncrementRounds: () -> Unit,
    onResetRoundsToOne: () -> Unit,
    onPlay: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1m, 2m, 3m, 4m, 5m
        val times = TimerDefaults.availableMinutes
        times.forEach { mins ->
            val isSelected = state.selectedMinutes == mins && state.phase == TimerPhase.IDLE
            LandscapeButton(
                text = "${mins} min",
                isSelected = isSelected,
                accentColor = BeltGold,
                modifier = Modifier.weight(1f),
                onClick = { onSelectMinutes(mins) }
            )
        }

        // Botão +Round (com toque simples para somar +1 e toque longo para voltar ao 1)
        RoundIncrementButton(
            totalRounds = state.totalRounds,
            modifier = Modifier.weight(1.3f),
            onIncrement = onIncrementRounds,
            onResetToOne = onResetRoundsToOne
        )

        // Botão Reiniciar
        LandscapeButton(
            text = "🔄",
            isSelected = false,
            accentColor = CardBorder,
            modifier = Modifier.weight(0.7f),
            onClick = onReset
        )

        // Botão Pular
        LandscapeButton(
            text = "⏭️",
            isSelected = false,
            accentColor = CardBorder,
            modifier = Modifier.weight(0.7f),
            onClick = onSkip
        )

        // Botão Principal INICIAR
        LandscapeButton(
            text = "▶ INICIAR",
            isSelected = true,
            accentColor = FightGreen,
            textColor = Color.Black,
            modifier = Modifier.weight(1.5f),
            onClick = onPlay
        )
    }
}

/**
 * Botão de Round com clique simples para incrementar e clique longo para voltar para o 1
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RoundIncrementButton(
    totalRounds: Int,
    modifier: Modifier = Modifier,
    onIncrement: () -> Unit,
    onResetToOne: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(
                onClick = onIncrement,
                onLongClick = onResetToOne
            ),
        color = CardBackground,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, BeltGold)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "+Round ($totalRounds)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "segure p/ 1",
                fontSize = 9.sp,
                color = BeltGold
            )
        }
    }
}

@Composable
private fun LandscapeButton(
    text: String,
    isSelected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    textColor: Color = if (isSelected) Color.Black else Color.White,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        color = if (isSelected) accentColor else CardBackground,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isSelected) accentColor else CardBorder)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsBottomSheet(
    state: TimerUiState,
    onDismiss: () -> Unit,
    onSetRestDuration: (Int) -> Unit,
    onAdjustRest: (Int) -> Unit,
    onToggleStartSound: () -> Unit,
    onToggleCountdownSound: () -> Unit,
    onToggleIntervalSound: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardBackground,
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "⏱️ Tempo de Intervalo entre Rounds",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = BeltGold
            )
            Text(
                text = "Escolha o tempo de descanso entre cada repetição de round",
                fontSize = 13.sp,
                color = Color.LightGray
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(TimerDefaults.availableIntervalsSec) { intervalSec ->
                    val isSelected = state.settings.restDurationSec == intervalSec
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSetRestDuration(intervalSec) },
                        color = if (isSelected) RestRed else DarkBackground,
                        shape = RoundedCornerShape(12.dp),
                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                    ) {
                        Text(
                            text = formatTime(intervalSec),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color.LightGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ajuste Fino de Intervalo",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onAdjustRest(-15) },
                        modifier = Modifier
                            .size(36.dp)
                            .background(DarkBackground, CircleShape)
                            .border(1.dp, CardBorder, CircleShape)
                    ) {
                        Text(text = "−", fontSize = 18.sp, color = Color.White)
                    }

                    Text(
                        text = formatTime(state.settings.restDurationSec),
                        modifier = Modifier
                            .width(80.dp)
                            .padding(horizontal = 4.dp),
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = RestRed
                    )

                    IconButton(
                        onClick = { onAdjustRest(15) },
                        modifier = Modifier
                            .size(36.dp)
                            .background(DarkBackground, CircleShape)
                            .border(1.dp, CardBorder, CircleShape)
                    ) {
                        Text(text = "+", fontSize = 18.sp, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            androidx.compose.material3.HorizontalDivider(color = CardBorder, thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "🔊 Alertas Sonoros Específicos",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = BeltGold
            )
            Text(
                text = "Ative ou silencie os alertas sonoros em cada momento do rola",
                fontSize = 13.sp,
                color = Color.LightGray
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 1. Início e Preparação
            SoundSettingRow(
                title = "Sons de Início e Preparação",
                subtitle = "Sino duplo de combate (Ding-Ding) e beeps de preparação (3, 2, 1)",
                isEnabled = state.settings.startSoundEnabled,
                onToggle = onToggleStartSound
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Contagem Regressiva Final (10s)
            SoundSettingRow(
                title = "Contagem Regressiva Final (10s)",
                subtitle = "Batida de madeira (Wood-Clap) aos 10s e bips a cada segundo (9 até 1)",
                isEnabled = state.settings.countdownSoundEnabled,
                onToggle = onToggleCountdownSound
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Sons de Intervalo e Fim do Round
            SoundSettingRow(
                title = "Sons de Intervalo e Fim do Round",
                subtitle = "Buzina de ginásio (Buzzer) ao encerrar o round e aviso de descanso",
                isEnabled = state.settings.intervalSoundEnabled,
                onToggle = onToggleIntervalSound
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = BeltGold, contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = "CONCLUIR", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun SoundSettingRow(
    title: String,
    subtitle: String,
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    lineHeight = 14.sp
                )
            }

            androidx.compose.material3.Switch(
                checked = isEnabled,
                onCheckedChange = { onToggle() },
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = BeltGold,
                    uncheckedThumbColor = Color.LightGray,
                    uncheckedTrackColor = CardBackground
                )
            )
        }
    }
}

fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}
