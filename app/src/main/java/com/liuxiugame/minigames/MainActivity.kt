package com.liuxiugame.minigames

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.liuxiugame.minigames.core.theme.MiniGamesTheme
import com.liuxiugame.minigames.ui.HallScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MiniGamesTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HallScreen(games = GameRegistry.all)
                }
            }
        }
    }
}
