package com.liuxiugame.minigames.game2048

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Composable
fun Game2048Screen(onExit: () -> Unit) {
    val context = LocalContext.current
    val store = remember { HighScoreStore(context) }
    val haptics = remember { Haptics(context) }

    var state by remember { mutableStateOf(Game2048State.initial()) }
    var history by remember { mutableStateOf<List<Game2048State>>(emptyList()) }
    var best by remember { mutableIntStateOf(store.getBest()) }
    var newRecord by remember { mutableStateOf(false) }
    var showWinDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.score) {
        if (store.setIfBetter(state.score)) {
            newRecord = true
            best = state.score
        }
    }

    LaunchedEffect(state.reachedWin, state.keepPlaying) {
        if (state.reachedWin && !state.keepPlaying) showWinDialog = true
    }

    fun doMove(direction: Move) {
        if (state.gameOver) return
        val next = state.move(direction)
        if (next === state) return
        history = (history + state).takeLast(20)
        // 检测最大合并是否较大 → 给个大震
        val maxValue = next.mergedCells.maxOfOrNull { (r, c) -> next.grid[r][c] } ?: 0
        when {
            next.gameOver -> haptics.gameOver()
            maxValue >= 128 -> haptics.bigMerge()
            next.mergedCells.isNotEmpty() -> haptics.merge()
        }
        state = next
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TopBar(
                score = state.score,
                best = best,
                onNewGame = {
                    state = Game2048State.initial()
                    history = emptyList()
                    newRecord = false
                    showWinDialog = false
                },
                onUndo = {
                    if (history.isNotEmpty()) {
                        state = history.last()
                        history = history.dropLast(1)
                    }
                },
                canUndo = history.isNotEmpty(),
            )
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BoardBg)
                    .pointerInput(state.gameOver) {
                        var dx = 0f
                        var dy = 0f
                        detectDragGestures(
                            onDragStart = { dx = 0f; dy = 0f },
                            onDrag = { _, drag -> dx += drag.x; dy += drag.y },
                            onDragEnd = {
                                val threshold = 40f
                                if (abs(dx) < threshold && abs(dy) < threshold) return@detectDragGestures
                                val dir = if (abs(dx) > abs(dy)) {
                                    if (dx > 0) Move.RIGHT else Move.LEFT
                                } else {
                                    if (dy > 0) Move.DOWN else Move.UP
                                }
                                doMove(dir)
                            },
                        )
                    },
            ) {
                BoardView(state = state)
                if (state.gameOver) {
                    GameOverOverlay(
                        score = state.score,
                        best = best,
                        newRecord = newRecord,
                        onRestart = {
                            state = Game2048State.initial()
                            history = emptyList()
                            newRecord = false
                        },
                        onExit = onExit,
                    )
                } else if (showWinDialog) {
                    WinOverlay(
                        onContinue = {
                            state = state.copy(keepPlaying = true)
                            showWinDialog = false
                        },
                        onRestart = {
                            state = Game2048State.initial()
                            history = emptyList()
                            showWinDialog = false
                        },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.g2048_hint),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.g2048_back_to_hall))
            }
        }
    }
}

@Composable
private fun TopBar(
    score: Int,
    best: Int,
    onNewGame: () -> Unit,
    onUndo: () -> Unit,
    canUndo: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "2048",
            fontSize = 36.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        ScoreBox(label = stringResource(R.string.g2048_score), value = score)
        Spacer(Modifier.size(8.dp))
        ScoreBox(label = stringResource(R.string.g2048_best), value = best)
    }
    Spacer(Modifier.height(12.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(onClick = onNewGame, modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.g2048_new_game))
        }
        OutlinedButton(
            onClick = onUndo,
            modifier = Modifier.weight(1f),
            enabled = canUndo,
        ) {
            Text(stringResource(R.string.g2048_undo))
        }
    }
}

@Composable
private fun ScoreBox(label: String, value: Int) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(BoardBg)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp)
        Text(value.toString(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BoardView(state: Game2048State) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        val gap = 6.dp
        val cellSize = (maxWidth - gap * (BOARD_SIZE + 1)) / BOARD_SIZE
        // 空格背景
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            for (r in 0 until BOARD_SIZE) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    for (c in 0 until BOARD_SIZE) {
                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .clip(RoundedCornerShape(6.dp))
                                .background(EmptyCellBg),
                        )
                    }
                }
            }
        }
        // 数字格
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            for (r in 0 until BOARD_SIZE) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    for (c in 0 until BOARD_SIZE) {
                        val value = state.grid[r][c]
                        val isSpawn = state.lastSpawned == (r to c)
                        val isMerged = (r to c) in state.mergedCells
                        TileCell(
                            value = value,
                            size = cellSize,
                            isSpawn = isSpawn,
                            isMerged = isMerged,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TileCell(
    value: Int,
    size: androidx.compose.ui.unit.Dp,
    isSpawn: Boolean,
    isMerged: Boolean,
) {
    if (value == 0) {
        Box(modifier = Modifier.size(size))
        return
    }
    // 出现/合并的小动画
    val targetScale = 1f
    val initialScale = when {
        isMerged -> 1.18f
        isSpawn -> 0.5f
        else -> 1f
    }
    var scaleTarget by remember(value, isSpawn, isMerged) { mutableStateOf(initialScale) }
    LaunchedEffect(value, isSpawn, isMerged) {
        scaleTarget = targetScale
    }
    val scale by animateFloatAsState(
        targetValue = scaleTarget,
        animationSpec = if (isMerged)
            tween(durationMillis = 140)
        else
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "tile",
    )
    val (bg, fg) = colorsFor(value)
    val textSize = when {
        value < 100 -> 32.sp
        value < 1000 -> 28.sp
        value < 10_000 -> 22.sp
        else -> 18.sp
    }
    Box(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .clip(RoundedCornerShape(6.dp))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = value.toString(),
            color = fg,
            fontSize = textSize,
            fontWeight = FontWeight.Black,
            modifier = Modifier.wrapContentSize(),
        )
    }
}

@Composable
private fun GameOverOverlay(
    score: Int,
    best: Int,
    newRecord: Boolean,
    onRestart: () -> Unit,
    onExit: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.g2048_game_over),
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
            )
            if (newRecord) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.g2048_new_record),
                    color = Color(0xFFFFC107),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text("分数 $score · 最高 $best", color = Color.White.copy(alpha = 0.9f))
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onRestart) { Text(stringResource(R.string.g2048_try_again)) }
                OutlinedButton(onClick = onExit) { Text(stringResource(R.string.g2048_back_to_hall)) }
            }
        }
    }
}

@Composable
private fun WinOverlay(onContinue: () -> Unit, onRestart: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "🎉",
                fontSize = 48.sp,
            )
            Text(
                text = stringResource(R.string.g2048_you_win),
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onContinue) { Text(stringResource(R.string.g2048_keep_playing)) }
                OutlinedButton(onClick = onRestart) { Text(stringResource(R.string.g2048_try_again)) }
            }
        }
    }
}

// ─── Palette ─────────────────────────────────────────────────────────────────

private val BoardBg = Color(0xFFBBADA0)
private val EmptyCellBg = Color(0xFFCDC1B4)

private fun colorsFor(value: Int): Pair<Color, Color> = when (value) {
    2 -> Color(0xFFEEE4DA) to Color(0xFF776E65)
    4 -> Color(0xFFEDE0C8) to Color(0xFF776E65)
    8 -> Color(0xFFF2B179) to Color.White
    16 -> Color(0xFFF59563) to Color.White
    32 -> Color(0xFFF67C5F) to Color.White
    64 -> Color(0xFFF65E3B) to Color.White
    128 -> Color(0xFFEDCF72) to Color.White
    256 -> Color(0xFFEDCC61) to Color.White
    512 -> Color(0xFFEDC850) to Color.White
    1024 -> Color(0xFFEDC53F) to Color.White
    2048 -> Color(0xFFEDC22E) to Color.White
    else -> Color(0xFF3C3A32) to Color.White
}
