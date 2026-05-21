package com.liuxiugame.minigames.snake

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import kotlinx.coroutines.delay
import kotlin.math.abs

private const val TICK_MS_INITIAL = 180L
private const val TICK_MS_MIN = 70L

@Composable
fun SnakeScreen(onExit: () -> Unit) {
    var state by remember { mutableStateOf(SnakeState.initial()) }
    var paused by remember { mutableStateOf(false) }
    var best by remember { mutableStateOf(0) }

    LaunchedEffect(state.gameOver, paused) {
        if (state.gameOver || paused) return@LaunchedEffect
        while (!state.gameOver && !paused) {
            // 蛇越长，节奏越快
            val tick = (TICK_MS_INITIAL - state.score * 4L).coerceAtLeast(TICK_MS_MIN)
            delay(tick)
            state = state.tick(kotlin.random.Random.Default)
            if (state.score > best) best = state.score
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ScoreBar(score = state.score, best = best)
            Spacer(Modifier.size(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(state.cols.toFloat() / state.rows.toFloat())
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .pointerInput(state.gameOver) {
                        if (state.gameOver) return@pointerInput
                        var dx = 0f
                        var dy = 0f
                        detectDragGestures(
                            onDragStart = { dx = 0f; dy = 0f },
                            onDrag = { _, dragAmount ->
                                dx += dragAmount.x
                                dy += dragAmount.y
                            },
                            onDragEnd = {
                                val threshold = 24f
                                if (abs(dx) < threshold && abs(dy) < threshold) return@detectDragGestures
                                val next = if (abs(dx) > abs(dy)) {
                                    if (dx > 0) Direction.RIGHT else Direction.LEFT
                                } else {
                                    if (dy > 0) Direction.DOWN else Direction.UP
                                }
                                state = state.withDirection(next)
                            },
                        )
                    },
            ) {
                Board(state = state, modifier = Modifier.fillMaxSize())
                if (state.gameOver) {
                    GameOverOverlay(
                        score = state.score,
                        onRestart = {
                            state = SnakeState.initial()
                            paused = false
                        },
                        onExit = onExit,
                    )
                } else if (paused) {
                    PausedOverlay(onResume = { paused = false })
                }
            }
            Spacer(Modifier.size(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                OutlinedButton(onClick = onExit) { Text("返回大厅") }
                if (!state.gameOver) {
                    Button(onClick = { paused = !paused }) {
                        Text(if (paused) stringResource(R.string.snake_resume) else stringResource(R.string.snake_pause))
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreBar(score: Int, best: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.snake_score, score),
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.snake_best, best),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Board(state: SnakeState, modifier: Modifier) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    val snakeColor = MaterialTheme.colorScheme.primary
    val headColor = MaterialTheme.colorScheme.tertiary
    val foodColor = MaterialTheme.colorScheme.error
    Canvas(modifier = modifier) {
        val cellW = size.width / state.cols
        val cellH = size.height / state.rows
        // 网格
        for (i in 1 until state.cols) {
            drawLine(
                color = gridColor,
                start = Offset(i * cellW, 0f),
                end = Offset(i * cellW, size.height),
                strokeWidth = 1f,
            )
        }
        for (j in 1 until state.rows) {
            drawLine(
                color = gridColor,
                start = Offset(0f, j * cellH),
                end = Offset(size.width, j * cellH),
                strokeWidth = 1f,
            )
        }
        // 食物
        drawRect(
            color = foodColor,
            topLeft = Offset(state.food.x * cellW + cellW * 0.1f, state.food.y * cellH + cellH * 0.1f),
            size = Size(cellW * 0.8f, cellH * 0.8f),
        )
        // 蛇身
        state.snake.forEachIndexed { idx, c ->
            drawRect(
                color = if (idx == 0) headColor else snakeColor,
                topLeft = Offset(c.x * cellW + 1f, c.y * cellH + 1f),
                size = Size(cellW - 2f, cellH - 2f),
            )
        }
    }
}

@Composable
private fun GameOverOverlay(score: Int, onRestart: () -> Unit, onExit: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.snake_game_over),
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.size(12.dp))
            Text(
                text = stringResource(R.string.snake_score, score),
                color = Color.White,
                fontSize = 20.sp,
            )
            Spacer(Modifier.size(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onRestart) { Text(stringResource(R.string.snake_restart)) }
                OutlinedButton(onClick = onExit) { Text("返回大厅") }
            }
        }
    }
}

@Composable
private fun PausedOverlay(onResume: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.snake_paused),
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.size(16.dp))
            Button(onClick = onResume) { Text(stringResource(R.string.snake_resume)) }
        }
    }
}
