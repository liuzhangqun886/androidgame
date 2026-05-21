package com.liuxiugame.minigames.snake

import android.content.Context

class HighScoreStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("snake_scores", Context.MODE_PRIVATE)

    fun get(difficulty: Difficulty): Int = prefs.getInt(keyOf(difficulty), 0)

    fun setIfBetter(difficulty: Difficulty, score: Int): Boolean {
        val cur = get(difficulty)
        if (score <= cur) return false
        prefs.edit().putInt(keyOf(difficulty), score).apply()
        return true
    }

    fun allBests(): Map<Difficulty, Int> =
        Difficulty.entries.associateWith { get(it) }

    private fun keyOf(d: Difficulty) = "best_${d.name}"
}
