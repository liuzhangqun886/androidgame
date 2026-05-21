package com.liuxiugame.minigames

import com.liuxiugame.minigames.core.GameEntry
import com.liuxiugame.minigames.snake.SnakeActivity

object GameRegistry {
    val all: List<GameEntry> = listOf(
        GameEntry(
            id = "snake",
            displayName = "贪吃蛇",
            description = "经典网格小游戏，滑动控制方向。",
            emoji = "🐍",
            launch = { SnakeActivity.newIntent(it) },
        ),
        // 后续游戏在此追加：2048、打砖块、扫雷、推箱子、摇骰子
    )
}
