package com.liuxiugame.minigames.snake

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.liuxiugame.minigames.core.theme.MiniGamesTheme

class SnakeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MiniGamesTheme {
                SnakeScreen(onExit = { finish() })
            }
        }
    }

    companion object {
        fun newIntent(context: Context): Intent = Intent(context, SnakeActivity::class.java)
    }
}
