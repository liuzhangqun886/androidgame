package com.liuxiugame.minigames.core

import android.content.Context
import android.content.Intent

data class GameEntry(
    val id: String,
    val displayName: String,
    val description: String,
    val emoji: String,
    val launch: (Context) -> Intent,
)
