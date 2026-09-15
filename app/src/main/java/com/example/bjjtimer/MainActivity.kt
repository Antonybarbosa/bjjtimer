package com.example.bjjtimer

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.bjjtimer.theme.BJJTimerTheme
import com.example.bjjtimer.theme.DarkBackground
import com.example.bjjtimer.ui.TimerScreen
import com.example.bjjtimer.ui.TimerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: TimerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mantém a tela acesa durante o treino no tatame
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Botões de volume do celular/tablet controlam diretamente o volume do som do treino
        volumeControlStream = android.media.AudioManager.STREAM_MUSIC

        enableEdgeToEdge()
        setContent {
            BJJTimerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    TimerScreen(viewModel = viewModel)
                }
            }
        }
    }
}
