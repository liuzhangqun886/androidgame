package com.liuxiugame.minigames.game2048

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.liuxiugame.minigames.core.theme.MiniGamesTheme

class Game2048Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MiniGamesTheme {
                Game2048Screen(onExit = { finish() })
            }
        }
    }

    companion object {
        fun newIntent(context: Context): Intent = Intent(context, Game2048Activity::class.java)
    }
}
