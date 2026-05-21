package com.liuxiugame.minigames.game2048

import kotlin.random.Random

const val BOARD_SIZE = 4
const val WIN_VALUE = 2048

enum class Move { LEFT, RIGHT, UP, DOWN }

typealias Grid = List<List<Int>>

data class Game2048State(
    val grid: Grid,
    val score: Int,
    val gameOver: Boolean,
    val reachedWin: Boolean,
    val keepPlaying: Boolean,
    val lastSpawned: Pair<Int, Int>? = null,
    val mergedCells: Set<Pair<Int, Int>> = emptySet(),
) {
    fun canMove(): Boolean {
        if (grid.any { row -> row.any { it == 0 } }) return true
        for (r in 0 until BOARD_SIZE) {
            for (c in 0 until BOARD_SIZE) {
                val v = grid[r][c]
                if (r + 1 < BOARD_SIZE && grid[r + 1][c] == v) return true
                if (c + 1 < BOARD_SIZE && grid[r][c + 1] == v) return true
            }
        }
        return false
    }

    fun move(direction: Move, rng: Random = Random.Default): Game2048State {
        if (gameOver) return this
        val (rotated, rotations) = rotateForLeftMove(grid, direction)
        var gained = 0
        val merged = mutableSetOf<Pair<Int, Int>>()
        val movedRows = rotated.mapIndexed { ri, row ->
            val (newRow, rowGain, rowMerges) = slideAndMergeLeft(row)
            gained += rowGain
            rowMerges.forEach { ci -> merged += (ri to ci) }
            newRow
        }
        val unrotatedGrid = rotateBack(movedRows, direction)
        val unrotatedMerged = merged.map { (r, c) -> rotateCoordBack(r, c, rotations) }.toSet()
        if (unrotatedGrid == grid) return this // 无变化不计数
        val spawn = spawnRandomTile(unrotatedGrid, rng)
        val (afterSpawn, spawnPos) = spawn
        val newReachedWin = reachedWin || unrotatedGrid.any { row -> row.any { it >= WIN_VALUE } }
        val canContinue = canMoveOnGrid(afterSpawn)
        return copy(
            grid = afterSpawn,
            score = score + gained,
            gameOver = !canContinue,
            reachedWin = newReachedWin,
            lastSpawned = spawnPos,
            mergedCells = unrotatedMerged,
        )
    }

    companion object {
        fun initial(rng: Random = Random.Default): Game2048State {
            val empty = List(BOARD_SIZE) { List(BOARD_SIZE) { 0 } }
            val (g1, p1) = spawnRandomTile(empty, rng)
            val (g2, p2) = spawnRandomTile(g1, rng)
            return Game2048State(
                grid = g2,
                score = 0,
                gameOver = false,
                reachedWin = false,
                keepPlaying = false,
                lastSpawned = p2 ?: p1,
            )
        }

        private fun spawnRandomTile(grid: Grid, rng: Random): Pair<Grid, Pair<Int, Int>?> {
            val empties = mutableListOf<Pair<Int, Int>>()
            for (r in 0 until BOARD_SIZE) {
                for (c in 0 until BOARD_SIZE) {
                    if (grid[r][c] == 0) empties += (r to c)
                }
            }
            if (empties.isEmpty()) return grid to null
            val (r, c) = empties[rng.nextInt(empties.size)]
            val value = if (rng.nextDouble() < 0.9) 2 else 4
            val newGrid = grid.mapIndexed { ri, row ->
                row.mapIndexed { ci, v -> if (ri == r && ci == c) value else v }
            }
            return newGrid to (r to c)
        }

        private fun canMoveOnGrid(grid: Grid): Boolean {
            if (grid.any { row -> row.any { it == 0 } }) return true
            for (r in 0 until BOARD_SIZE) {
                for (c in 0 until BOARD_SIZE) {
                    val v = grid[r][c]
                    if (r + 1 < BOARD_SIZE && grid[r + 1][c] == v) return true
                    if (c + 1 < BOARD_SIZE && grid[r][c + 1] == v) return true
                }
            }
            return false
        }

        private fun slideAndMergeLeft(row: List<Int>): Triple<List<Int>, Int, List<Int>> {
            val nonZero = row.filter { it != 0 }
            val out = ArrayList<Int>(row.size)
            val mergedIndices = mutableListOf<Int>()
            var gained = 0
            var i = 0
            while (i < nonZero.size) {
                if (i + 1 < nonZero.size && nonZero[i] == nonZero[i + 1]) {
                    val v = nonZero[i] * 2
                    mergedIndices += out.size
                    out += v
                    gained += v
                    i += 2
                } else {
                    out += nonZero[i]
                    i += 1
                }
            }
            while (out.size < row.size) out += 0
            return Triple(out, gained, mergedIndices)
        }

        private fun rotateForLeftMove(grid: Grid, direction: Move): Pair<Grid, Int> {
            // 把 direction 方向转到 LEFT，再统一做 slideAndMergeLeft
            return when (direction) {
                Move.LEFT -> grid to 0
                Move.UP -> rotateCCW(grid) to 1   // 上 → 左
                Move.RIGHT -> rotateCCW(rotateCCW(grid)) to 2
                Move.DOWN -> rotateCCW(rotateCCW(rotateCCW(grid))) to 3
            }
        }

        private fun rotateBack(grid: Grid, direction: Move): Grid = when (direction) {
            Move.LEFT -> grid
            Move.UP -> rotateCW(grid)
            Move.RIGHT -> rotateCW(rotateCW(grid))
            Move.DOWN -> rotateCW(rotateCW(rotateCW(grid)))
        }

        private fun rotateCoordBack(r: Int, c: Int, rotations: Int): Pair<Int, Int> {
            var rr = r; var cc = c
            repeat(rotations) {
                // CCW 的反向：(r, c) -> (c, N-1-r)  即 CW 一次
                val nr = cc
                val nc = BOARD_SIZE - 1 - rr
                rr = nr; cc = nc
            }
            return rr to cc
        }

        private fun rotateCCW(g: Grid): Grid {
            // (r, c) -> (BOARD_SIZE-1-c, r)
            return List(BOARD_SIZE) { r ->
                List(BOARD_SIZE) { c -> g[c][BOARD_SIZE - 1 - r] }
            }
        }

        private fun rotateCW(g: Grid): Grid {
            // (r, c) -> (c, BOARD_SIZE-1-r)
            return List(BOARD_SIZE) { r ->
                List(BOARD_SIZE) { c -> g[BOARD_SIZE - 1 - c][r] }
            }
        }
    }
}
