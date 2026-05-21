package com.liuxiugame.minigames.snake

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.random.Random

private enum class Phase { Idle, Playing, Paused, GameOver }

private val DeathQuips = listOf(
    "刚才那是什么操作？",
    "蛇蛇也累了…",
    "再来！差一点点。",
    "贪心不足蛇吞身。",
    "墙：我又不会动。",
    "心态稳一点，速度就稳了。",
    "刚才那个转弯太果断。",
)

@Composable
fun SnakeScreen(onExit: () -> Unit) {
    val context = LocalContext.current
    val store = remember { HighScoreStore(context) }
    val haptics = remember { Haptics(context) }

    var config by remember { mutableStateOf(SnakeConfig()) }
    var phase by remember { mutableStateOf(Phase.Idle) }
    var state by remember { mutableStateOf(SnakeState.initial(config)) }
    var bestForCurrent by remember { mutableIntStateOf(store.get(config.difficulty)) }
    var newRecord by remember { mutableStateOf(false) }
    var quip by remember { mutableStateOf(DeathQuips.first()) }

    // 主游戏循环
    LaunchedEffect(phase, config) {
        if (phase != Phase.Playing) return@LaunchedEffect
        while (phase == Phase.Playing && !state.gameOver) {
            delay(state.tickInterval())
            val prev = state
            val next = prev.tick(Random.Default)
            state = next
            val delta = next.score - prev.score
            when {
                next.gameOver -> {
                    haptics.death()
                    val improved = store.setIfBetter(config.difficulty, next.score)
                    newRecord = improved
                    bestForCurrent = store.get(config.difficulty)
                    quip = DeathQuips.random()
                    phase = Phase.GameOver
                }
                delta == GOLD_FOOD_VALUE -> haptics.gold()
                delta == 1 -> haptics.tick()
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (phase) {
            Phase.Idle -> StartScreen(
                config = config,
                bests = store.allBests(),
                onConfigChange = {
                    config = it
                    bestForCurrent = store.get(it.difficulty)
                },
                onStart = {
                    state = SnakeState.initial(config)
                    newRecord = false
                    phase = Phase.Playing
                },
                onExit = onExit,
            )
            Phase.Playing, Phase.Paused, Phase.GameOver -> GameScreen(
                state = state,
                best = bestForCurrent,
                paused = phase == Phase.Paused,
                gameOver = phase == Phase.GameOver,
                newRecord = newRecord,
                deathQuip = quip,
                onSwipe = { dir -> state = state.queueDirection(dir) },
                onPauseToggle = {
                    phase = if (phase == Phase.Paused) Phase.Playing else Phase.Paused
                },
                onRestart = {
                    state = SnakeState.initial(config)
                    newRecord = false
                    phase = Phase.Playing
                },
                onBackToStart = { phase = Phase.Idle },
                onExit = onExit,
            )
        }
    }
}

// ─── Start Screen ────────────────────────────────────────────────────────────

@Composable
private fun StartScreen(
    config: SnakeConfig,
    bests: Map<Difficulty, Int>,
    onConfigChange: (SnakeConfig) -> Unit,
    onStart: () -> Unit,
    onExit: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
    ) {
        Text(text = "🐍", fontSize = 72.sp)
        Text(
            text = stringResource(R.string.snake_title),
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
        )

        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.snake_difficulty), fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Difficulty.entries.forEach { d ->
                        val selected = config.difficulty == d
                        DifficultyChip(
                            label = d.displayName,
                            selected = selected,
                            modifier = Modifier.weight(1f),
                            onClick = { onConfigChange(config.copy(difficulty = d)) },
                        )
                    }
                }
                Text(
                    text = "${stringResource(R.string.snake_best_label)}：${bests[config.difficulty] ?: 0}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(1.dp)) {
            Column(Modifier.padding(16.dp)) {
                ToggleRow(
                    label = stringResource(R.string.snake_wrap),
                    checked = config.wrapAround,
                    onChange = { onConfigChange(config.copy(wrapAround = it)) },
                )
                ToggleRow(
                    label = stringResource(R.string.snake_obstacles),
                    checked = config.obstacles,
                    onChange = { onConfigChange(config.copy(obstacles = it)) },
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text(stringResource(R.string.snake_start), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
        OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.snake_back_to_hall))
        }
    }
}

@Composable
private fun DifficultyChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = fg, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

// ─── Game Screen ─────────────────────────────────────────────────────────────

@Composable
private fun GameScreen(
    state: SnakeState,
    best: Int,
    paused: Boolean,
    gameOver: Boolean,
    newRecord: Boolean,
    deathQuip: String,
    onSwipe: (Direction) -> Unit,
    onPauseToggle: () -> Unit,
    onRestart: () -> Unit,
    onBackToStart: () -> Unit,
    onExit: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ScoreBar(score = state.score, best = best, length = state.snake.size)
        Spacer(Modifier.size(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(state.cols.toFloat() / state.rows.toFloat())
                .clip(RoundedCornerShape(12.dp))
                .background(boardBackgroundBrush())
                .pointerInput(gameOver) {
                    if (gameOver) return@pointerInput
                    var dx = 0f
                    var dy = 0f
                    detectDragGestures(
                        onDragStart = { dx = 0f; dy = 0f },
                        onDrag = { _, drag ->
                            dx += drag.x
                            dy += drag.y
                        },
                        onDragEnd = {
                            val threshold = 18f
                            if (abs(dx) < threshold && abs(dy) < threshold) return@detectDragGestures
                            val dir = if (abs(dx) > abs(dy)) {
                                if (dx > 0) Direction.RIGHT else Direction.LEFT
                            } else {
                                if (dy > 0) Direction.DOWN else Direction.UP
                            }
                            onSwipe(dir)
                        },
                    )
                },
        ) {
            Board(state = state, modifier = Modifier.fillMaxSize())
            when {
                gameOver -> GameOverOverlay(
                    score = state.score,
                    best = best,
                    newRecord = newRecord,
                    quip = deathQuip,
                    onRestart = onRestart,
                    onBackToStart = onBackToStart,
                    onExit = onExit,
                )
                paused -> PausedOverlay(onResume = onPauseToggle)
            }
        }
        Spacer(Modifier.size(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = onBackToStart, modifier = Modifier.weight(1f)) {
                Text("菜单")
            }
            if (!gameOver) {
                Button(onClick = onPauseToggle, modifier = Modifier.weight(1f)) {
                    Text(if (paused) stringResource(R.string.snake_resume) else stringResource(R.string.snake_pause))
                }
            } else {
                Button(onClick = onRestart, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.snake_restart))
                }
            }
        }
    }
}

@Composable
private fun ScoreBar(score: Int, best: Int, length: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = stringResource(R.string.snake_score, score),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.snake_length, length),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.snake_best, best),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun boardBackgroundBrush(): Brush {
    val top = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val bottom = MaterialTheme.colorScheme.surface
    return Brush.verticalGradient(listOf(top, bottom))
}

// ─── Board rendering ─────────────────────────────────────────────────────────

@Composable
private fun Board(state: SnakeState, modifier: Modifier) {
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)
    val obstacleColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    val foodColor = MaterialTheme.colorScheme.error
    val foodGlow = foodColor.copy(alpha = 0.25f)
    val goldColor = Color(0xFFFFC107)
    val goldGlow = goldColor.copy(alpha = 0.4f)
    val headColor = MaterialTheme.colorScheme.tertiary
    val bodyStart = MaterialTheme.colorScheme.primary
    val bodyEnd = MaterialTheme.colorScheme.secondary

    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.85f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )

    Canvas(modifier = modifier) {
        val cellW = size.width / state.cols
        val cellH = size.height / state.rows
        val cell = minOf(cellW, cellH)

        // 网格点（柔和的圆点）
        val dotR = cell * 0.04f
        for (i in 0 until state.cols) {
            for (j in 0 until state.rows) {
                drawCircle(
                    color = gridColor,
                    radius = dotR,
                    center = Offset(i * cellW + cellW / 2, j * cellH + cellH / 2),
                )
            }
        }

        // 障碍物
        state.obstacles.forEach { c ->
            drawRoundedRect(
                color = obstacleColor,
                cell = c,
                cellW = cellW,
                cellH = cellH,
                inset = cell * 0.08f,
                radius = cell * 0.18f,
            )
        }

        // 食物（呼吸缩放 + 光晕）
        drawFood(state.food, cellW, cellH, foodColor, foodGlow, pulse)

        // 金苹果（带光晕、TTL 越少越闪）
        state.goldFood?.let { g ->
            val flicker = if (state.goldTtl < 12) {
                0.6f + 0.4f * kotlin.math.abs(((state.goldTtl % 4) - 2)) / 2f
            } else 1f
            drawFood(g, cellW, cellH, goldColor.copy(alpha = flicker), goldGlow, pulse * 1.05f)
        }

        // 蛇身：先填补段间空隙，再画圆角段，最后画蛇头
        drawSnake(
            snake = state.snake,
            cols = state.cols,
            rows = state.rows,
            wrap = state.config.wrapAround,
            cellW = cellW,
            cellH = cellH,
            cell = cell,
            bodyStart = bodyStart,
            bodyEnd = bodyEnd,
            headColor = headColor,
            direction = state.direction,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRoundedRect(
    color: Color,
    cell: Cell,
    cellW: Float,
    cellH: Float,
    inset: Float,
    radius: Float,
) {
    drawRoundRect(
        color = color,
        topLeft = Offset(cell.x * cellW + inset, cell.y * cellH + inset),
        size = Size(cellW - 2 * inset, cellH - 2 * inset),
        cornerRadius = CornerRadius(radius, radius),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFood(
    c: Cell,
    cellW: Float,
    cellH: Float,
    color: Color,
    glow: Color,
    scale: Float,
) {
    val center = Offset(c.x * cellW + cellW / 2, c.y * cellH + cellH / 2)
    val base = minOf(cellW, cellH)
    drawCircle(color = glow, radius = base * 0.6f * scale, center = center)
    drawCircle(color = color, radius = base * 0.35f * scale, center = center)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSnake(
    snake: List<Cell>,
    cols: Int,
    rows: Int,
    wrap: Boolean,
    cellW: Float,
    cellH: Float,
    cell: Float,
    bodyStart: Color,
    bodyEnd: Color,
    headColor: Color,
    direction: Direction,
) {
    val inset = cell * 0.10f
    val radius = cell * 0.32f

    // 段间桥（避免圆角导致缝隙）
    for (i in 0 until snake.size - 1) {
        val a = snake[i]
        val b = snake[i + 1]
        // 跨越 wrap 边界时不画桥（视觉上是断开的）
        val dx = b.x - a.x
        val dy = b.y - a.y
        if (wrap && (abs(dx) > 1 || abs(dy) > 1)) continue
        val t = i.toFloat() / snake.size.coerceAtLeast(1)
        val bridgeColor = lerp(bodyStart, bodyEnd, t)
        val bridgeInset = cell * 0.10f
        if (dx != 0) {
            val left = (minOf(a.x, b.x) * cellW) + cellW - bridgeInset
            val top = a.y * cellH + bridgeInset
            drawRect(
                color = bridgeColor,
                topLeft = Offset(left, top),
                size = Size(2 * bridgeInset, cellH - 2 * bridgeInset),
            )
        } else if (dy != 0) {
            val left = a.x * cellW + bridgeInset
            val top = (minOf(a.y, b.y) * cellH) + cellH - bridgeInset
            drawRect(
                color = bridgeColor,
                topLeft = Offset(left, top),
                size = Size(cellW - 2 * bridgeInset, 2 * bridgeInset),
            )
        }
    }

    // 蛇身段
    snake.forEachIndexed { idx, c ->
        if (idx == 0) return@forEachIndexed
        val t = idx.toFloat() / snake.size.coerceAtLeast(1)
        val color = lerp(bodyStart, bodyEnd, t)
        drawRoundedRect(color, c, cellW, cellH, inset, radius)
    }

    // 蛇头
    val head = snake.first()
    drawRoundedRect(headColor, head, cellW, cellH, inset * 0.6f, radius * 1.05f)
    // 眼睛朝方向
    val cx = head.x * cellW + cellW / 2
    val cy = head.y * cellH + cellH / 2
    val eyeOffset = cell * 0.20f
    val eyeR = cell * 0.09f
    val (e1, e2) = when (direction) {
        Direction.RIGHT -> Offset(cx + eyeOffset, cy - eyeOffset) to Offset(cx + eyeOffset, cy + eyeOffset)
        Direction.LEFT -> Offset(cx - eyeOffset, cy - eyeOffset) to Offset(cx - eyeOffset, cy + eyeOffset)
        Direction.UP -> Offset(cx - eyeOffset, cy - eyeOffset) to Offset(cx + eyeOffset, cy - eyeOffset)
        Direction.DOWN -> Offset(cx - eyeOffset, cy + eyeOffset) to Offset(cx + eyeOffset, cy + eyeOffset)
    }
    drawCircle(Color.White, eyeR, e1)
    drawCircle(Color.White, eyeR, e2)
    drawCircle(Color(0xFF1B1B1B), eyeR * 0.55f, e1)
    drawCircle(Color(0xFF1B1B1B), eyeR * 0.55f, e2)
}

// ─── Overlays ────────────────────────────────────────────────────────────────

@Composable
private fun GameOverOverlay(
    score: Int,
    best: Int,
    newRecord: Boolean,
    quip: String,
    onRestart: () -> Unit,
    onBackToStart: () -> Unit,
    onExit: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.58f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp),
        ) {
            Text(
                text = stringResource(R.string.snake_game_over),
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
            )
            if (newRecord) {
                Spacer(Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.snake_new_record),
                    color = Color(0xFFFFC107),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.size(8.dp))
            Text(
                text = quip,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
            )
            Spacer(Modifier.size(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                ScorePill("本局", score, Color.White)
                ScorePill("最高", best, Color(0xFFFFC107))
            }
            Spacer(Modifier.size(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onRestart) { Text(stringResource(R.string.snake_restart)) }
                OutlinedButton(onClick = onBackToStart) { Text("菜单") }
                OutlinedButton(onClick = onExit) { Text(stringResource(R.string.snake_back_to_hall)) }
            }
        }
    }
}

@Composable
private fun ScorePill(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        Text(value.toString(), color = color, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PausedOverlay(onResume: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)),
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
