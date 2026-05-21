package com.liuxiugame.minigames.snake

import kotlin.math.abs
import kotlin.random.Random

enum class Direction { UP, DOWN, LEFT, RIGHT }

private fun Direction.opposite(): Direction = when (this) {
    Direction.UP -> Direction.DOWN
    Direction.DOWN -> Direction.UP
    Direction.LEFT -> Direction.RIGHT
    Direction.RIGHT -> Direction.LEFT
}

enum class Difficulty(
    val displayName: String,
    val tickMs: Long,
    val minTickMs: Long,
    val accel: Long,
    val obstacleCount: Int,
) {
    EASY("慢", 220L, 110L, 3L, 3),
    NORMAL("普通", 160L, 80L, 4L, 5),
    HARD("快", 110L, 55L, 5L, 7);
}

data class SnakeConfig(
    val difficulty: Difficulty = Difficulty.NORMAL,
    val wrapAround: Boolean = false,
    val obstacles: Boolean = false,
)

data class Cell(val x: Int, val y: Int)

const val GOLD_FOOD_TTL = 35
const val GOLD_FOOD_VALUE = 5
private const val GOLD_FOOD_CHANCE = 0.30

data class SnakeState(
    val cols: Int,
    val rows: Int,
    val snake: List<Cell>,
    val direction: Direction,
    val nextDirection: Direction,
    val food: Cell,
    val goldFood: Cell?,
    val goldTtl: Int,
    val obstacles: Set<Cell>,
    val score: Int,
    val gameOver: Boolean,
    val config: SnakeConfig,
) {
    val head: Cell get() = snake.first()

    fun tickInterval(): Long {
        val d = config.difficulty
        return (d.tickMs - score * d.accel).coerceAtLeast(d.minTickMs)
    }

    fun queueDirection(next: Direction): SnakeState {
        // 不允许相对当前方向 180 度反向
        if (next == direction.opposite()) return this
        return copy(nextDirection = next)
    }

    fun tick(rng: Random): SnakeState {
        if (gameOver) return this

        val activeDir = if (nextDirection != direction.opposite()) nextDirection else direction
        val rawNext = when (activeDir) {
            Direction.UP -> head.copy(y = head.y - 1)
            Direction.DOWN -> head.copy(y = head.y + 1)
            Direction.LEFT -> head.copy(x = head.x - 1)
            Direction.RIGHT -> head.copy(x = head.x + 1)
        }
        val nextHead = if (config.wrapAround) {
            Cell(((rawNext.x % cols) + cols) % cols, ((rawNext.y % rows) + rows) % rows)
        } else {
            rawNext
        }
        val outOfBounds = !config.wrapAround &&
            (nextHead.x !in 0 until cols || nextHead.y !in 0 until rows)
        if (outOfBounds) return copy(gameOver = true, direction = activeDir)

        val ateNormal = nextHead == food
        val ateGold = goldFood != null && nextHead == goldFood
        val grew = ateNormal || ateGold
        val newBody = if (grew) {
            listOf(nextHead) + snake
        } else {
            listOf(nextHead) + snake.dropLast(1)
        }
        val hitSelf = newBody.drop(1).contains(nextHead)
        val hitObstacle = nextHead in obstacles
        if (hitSelf || hitObstacle) return copy(gameOver = true, direction = activeDir)

        var newFood = food
        var newGold = goldFood
        var newGoldTtl = goldTtl
        if (ateNormal) {
            newFood = randomEmpty(cols, rows, newBody, obstacles, listOfNotNull(goldFood), rng)
            if (goldFood == null && rng.nextDouble() < GOLD_FOOD_CHANCE) {
                newGold = randomEmpty(cols, rows, newBody, obstacles, listOf(newFood), rng)
                newGoldTtl = GOLD_FOOD_TTL
            }
        }
        if (ateGold) {
            newGold = null
            newGoldTtl = 0
        } else if (newGold != null) {
            newGoldTtl -= 1
            if (newGoldTtl <= 0) {
                newGold = null
                newGoldTtl = 0
            }
        }
        val gained = when {
            ateGold -> GOLD_FOOD_VALUE
            ateNormal -> 1
            else -> 0
        }
        return copy(
            snake = newBody,
            direction = activeDir,
            food = newFood,
            goldFood = newGold,
            goldTtl = newGoldTtl,
            score = score + gained,
        )
    }

    companion object {
        fun initial(
            config: SnakeConfig = SnakeConfig(),
            cols: Int = 20,
            rows: Int = 28,
            rng: Random = Random.Default,
        ): SnakeState {
            val sx = cols / 2
            val sy = rows / 2
            val body = listOf(Cell(sx, sy), Cell(sx - 1, sy), Cell(sx - 2, sy))
            val obstacles = if (config.obstacles) {
                generateObstacles(cols, rows, body, config.difficulty.obstacleCount, rng)
            } else emptySet()
            val food = randomEmpty(cols, rows, body, obstacles, emptyList(), rng)
            return SnakeState(
                cols = cols,
                rows = rows,
                snake = body,
                direction = Direction.RIGHT,
                nextDirection = Direction.RIGHT,
                food = food,
                goldFood = null,
                goldTtl = 0,
                obstacles = obstacles,
                score = 0,
                gameOver = false,
                config = config,
            )
        }

        private fun generateObstacles(
            cols: Int, rows: Int, snake: List<Cell>, count: Int, rng: Random,
        ): Set<Cell> {
            val taken = HashSet<Cell>()
            taken += snake
            // 避免把障碍堆在出生方向前方
            val frontBuffer = (1..6).map { Cell(snake.first().x + it, snake.first().y) }
            taken += frontBuffer
            val result = LinkedHashSet<Cell>()
            var attempts = 0
            while (result.size < count && attempts < count * 80) {
                attempts++
                val c = Cell(rng.nextInt(cols), rng.nextInt(rows))
                if (c in taken) continue
                if (abs(c.y - snake.first().y) < 2) continue
                result += c
                taken += c
            }
            return result
        }

        private fun randomEmpty(
            cols: Int, rows: Int,
            snake: List<Cell>,
            obstacles: Set<Cell>,
            others: List<Cell>,
            rng: Random,
        ): Cell {
            val taken = HashSet<Cell>(snake.size + obstacles.size + others.size)
            taken += snake; taken += obstacles; taken += others
            while (true) {
                val c = Cell(rng.nextInt(cols), rng.nextInt(rows))
                if (c !in taken) return c
            }
        }
    }
}
