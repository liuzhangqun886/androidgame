package com.liuxiugame.minigames.snake

import kotlin.random.Random

enum class Direction { UP, DOWN, LEFT, RIGHT }

private fun Direction.opposite(): Direction = when (this) {
    Direction.UP -> Direction.DOWN
    Direction.DOWN -> Direction.UP
    Direction.LEFT -> Direction.RIGHT
    Direction.RIGHT -> Direction.LEFT
}

data class Cell(val x: Int, val y: Int)

data class SnakeState(
    val cols: Int,
    val rows: Int,
    val snake: List<Cell>,
    val direction: Direction,
    val food: Cell,
    val score: Int,
    val gameOver: Boolean,
) {
    val head: Cell get() = snake.first()

    fun withDirection(next: Direction): SnakeState {
        // 不允许 180 度掉头
        if (next == direction.opposite()) return this
        return copy(direction = next)
    }

    fun tick(rng: Random): SnakeState {
        if (gameOver) return this
        val nextHead = when (direction) {
            Direction.UP -> head.copy(y = head.y - 1)
            Direction.DOWN -> head.copy(y = head.y + 1)
            Direction.LEFT -> head.copy(x = head.x - 1)
            Direction.RIGHT -> head.copy(x = head.x + 1)
        }
        val outOfBounds = nextHead.x !in 0 until cols || nextHead.y !in 0 until rows
        val ate = nextHead == food
        val newBody = if (ate) listOf(nextHead) + snake else listOf(nextHead) + snake.dropLast(1)
        val hitSelf = newBody.drop(1).contains(nextHead)
        if (outOfBounds || hitSelf) {
            return copy(gameOver = true)
        }
        val newFood = if (ate) randomFood(cols, rows, newBody, rng) else food
        return copy(
            snake = newBody,
            food = newFood,
            score = if (ate) score + 1 else score,
        )
    }

    companion object {
        fun initial(cols: Int = 20, rows: Int = 28, rng: Random = Random.Default): SnakeState {
            val startX = cols / 2
            val startY = rows / 2
            val body = listOf(
                Cell(startX, startY),
                Cell(startX - 1, startY),
                Cell(startX - 2, startY),
            )
            return SnakeState(
                cols = cols,
                rows = rows,
                snake = body,
                direction = Direction.RIGHT,
                food = randomFood(cols, rows, body, rng),
                score = 0,
                gameOver = false,
            )
        }

        private fun randomFood(cols: Int, rows: Int, occupied: List<Cell>, rng: Random): Cell {
            val taken = occupied.toHashSet()
            // 网格不大，循环采样足够
            while (true) {
                val c = Cell(rng.nextInt(cols), rng.nextInt(rows))
                if (c !in taken) return c
            }
        }
    }
}
