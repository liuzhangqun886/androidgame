package com.liuxiugame.minigames

import com.liuxiugame.minigames.core.GameEntry
import com.liuxiugame.minigames.game2048.Game2048Activity
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
        GameEntry(
            id = "g2048",
            displayName = "2048",
            description = "滑动合并相同数字，目标拼出 2048。",
            emoji = "🔢",
            launch = { Game2048Activity.newIntent(it) },
        ),
        // 后续：打砖块、扫雷、推箱子、摇骰子
    )
}
