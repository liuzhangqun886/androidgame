package com.liuxiugame.minigames.game2048

import android.content.Context

class HighScoreStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("g2048_scores", Context.MODE_PRIVATE)

    fun getBest(): Int = prefs.getInt(KEY_BEST, 0)

    fun setIfBetter(score: Int): Boolean {
        if (score <= getBest()) return false
        prefs.edit().putInt(KEY_BEST, score).apply()
        return true
    }

    companion object {
        private const val KEY_BEST = "best_score"
    }
}
