package com.example.ui.match

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.GameMode
import com.example.game.model.GameState
import com.example.game.model.MatchStatus
import com.example.game.model.Player
import com.example.ui.board.BoardCanvas
import com.example.ui.game.GameViewModel
import com.example.ui.theme.BorderMuted
import com.example.ui.theme.BorderStrong
import com.example.ui.theme.DotOrange
import com.example.ui.theme.PlayerColors
import com.example.ui.theme.SurfaceBlack
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun MatchScreen(
    viewModel: GameViewModel,
    onNavigateHome: () -> Unit
) {
    val gameState by viewModel.gameState.collectAsState()
    val gameMode by viewModel.gameMode.collectAsState()
    val isAiThinking by viewModel.isAiThinking.collectAsState()
    val isAiPaused by viewModel.isAiPaused.collectAsState()
    val comboText by viewModel.recentComboText.collectAsState()
    val view = LocalView.current

    var showExitDialog by remember { mutableStateOf(false) }

    // Intercept hardware and gesture back
    BackHandler {
        if (gameState.status == MatchStatus.IN_PROGRESS) {
            showExitDialog = true
        } else {
            onNavigateHome()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SurfaceBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Navigation & Status Bar
            TopBar(
                mode = gameMode,
                boardSizeLabel = gameState.boardSize.label,
                onBack = {
                    if (gameState.status == MatchStatus.IN_PROGRESS) {
                        showExitDialog = true
                    } else {
                        onNavigateHome()
                    }
                },
                onRestart = { viewModel.restartMatch() }
            )

            // Players Scoreboard
            Scoreboard(
                players = gameState.players,
                scores = gameState.scores,
                currentPlayerIndex = gameState.currentPlayerIndex,
                isAiThinking = isAiThinking
            )

            // Dynamic Combo / Extra-turn Banner
            AnimatedVisibility(
                visible = comboText != null,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = comboText ?: "",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = DotOrange
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(DotOrange.copy(alpha = 0.16f))
                            .border(1.dp, DotOrange.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            // Interactive Board Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                BoardCanvas(
                    gameState = gameState,
                    enabled = gameState.status == MatchStatus.IN_PROGRESS &&
                            !(gameState.currentPlayer?.isAi ?: false),
                    onEdgeSelected = { edge ->
                        viewModel.onUserSelectEdge(edge, view = view)
                    }
                )
            }

            // Controls Bar (AI Pause/Play/Step or Move Counter)
            BottomControlsBar(
                gameState = gameState,
                gameMode = gameMode,
                isAiPaused = isAiPaused,
                onToggleAiPause = { viewModel.toggleAiPause() },
                onStepAi = { viewModel.stepAiTurn() }
            )
        }

        // Leave Match Confirmation Dialog
        if (showExitDialog) {
            AlertDialog(
                onDismissRequest = { showExitDialog = false },
                title = { Text("Leave Match?", color = TextPrimary) },
                text = {
                    Text(
                        "Leaving will forfeit the active game progress. Are you sure you want to return to home?",
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showExitDialog = false
                            onNavigateHome()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DotOrange)
                    ) {
                        Text("Leave Match", color = SurfaceBlack)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExitDialog = false }) {
                        Text("Resume Play", color = TextSecondary)
                    }
                },
                containerColor = SurfaceCard
            )
        }

        // Match Completed Result Modal
        if (gameState.status == MatchStatus.FINISHED) {
            ResultModal(
                gameState = gameState,
                onRematch = { viewModel.restartMatch() },
                onReturnHome = onNavigateHome
            )
        }
    }
}

@Composable
private fun TopBar(
    mode: GameMode,
    boardSizeLabel: String,
    onBack: () -> Unit,
    onRestart: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.testTag("match_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Leave",
                tint = TextPrimary
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mode.label,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Text(
                text = "$boardSizeLabel Grid",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        IconButton(
            onClick = onRestart,
            modifier = Modifier.testTag("match_restart_button")
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Restart",
                tint = TextSecondary
            )
        }
    }
}

@Composable
private fun Scoreboard(
    players: List<Player>,
    scores: Map<Int, Int>,
    currentPlayerIndex: Int,
    isAiThinking: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        players.forEachIndexed { index, player ->
            val isCurrentTurn = index == currentPlayerIndex
            val playerColor = PlayerColors.getOrElse(player.colorIndex) { PlayerColors[0] }
            val score = scores[player.id] ?: 0

            Card(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .border(
                        width = if (isCurrentTurn) 2.dp else 1.dp,
                        color = if (isCurrentTurn) playerColor else BorderMuted,
                        shape = RoundedCornerShape(10.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCurrentTurn) playerColor.copy(alpha = 0.14f) else SurfaceCard
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(playerColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = player.name,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isCurrentTurn) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isCurrentTurn) TextPrimary else TextSecondary,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "$score",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (isCurrentTurn) playerColor else TextPrimary
                    )

                    if (isCurrentTurn) {
                        Text(
                            text = if (player.isAi && isAiThinking) "Thinking..." else "Active Turn",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = playerColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomControlsBar(
    gameState: GameState,
    gameMode: GameMode,
    isAiPaused: Boolean,
    onToggleAiPause: () -> Unit,
    onStepAi: () -> Unit
) {
    val allBots = gameState.players.isNotEmpty() && gameState.players.all { it.isAi }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Boxes: ${gameState.claimedBoxCount} / ${gameState.totalBoxes}",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Text(
                text = "Moves: ${gameState.sequenceNumber}",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }

        if (allBots) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onToggleAiPause,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAiPaused) DotOrange else SurfaceCardElevated
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = if (isAiPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (isAiPaused) "Resume" else "Pause",
                        tint = if (isAiPaused) SurfaceBlack else TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isAiPaused) "Resume" else "Pause",
                        color = if (isAiPaused) SurfaceBlack else TextPrimary
                    )
                }

                if (isAiPaused) {
                    Button(
                        onClick = onStepAi,
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceCardElevated),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Step",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Step", color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultModal(
    gameState: GameState,
    onRematch: () -> Unit,
    onReturnHome: () -> Unit
) {
    val winnerPlayer = gameState.players.find { it.id == gameState.winnerPlayerId }
    val winnerColor = if (winnerPlayer != null) {
        PlayerColors.getOrElse(winnerPlayer.colorIndex) { PlayerColors[0] }
    } else {
        DotOrange
    }

    AlertDialog(
        onDismissRequest = {},
        confirmButton = {
            Button(
                onClick = onRematch,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("result_rematch_button"),
                colors = ButtonDefaults.buttonColors(containerColor = DotOrange),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Play Rematch", color = SurfaceBlack, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onReturnHome,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("result_home_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Return to Home", color = TextSecondary)
            }
        },
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (gameState.isDraw) "MATCH DRAW!" else "${winnerPlayer?.name?.uppercase()} WINS!",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = if (gameState.isDraw) TextPrimary else winnerColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "All ${gameState.totalBoxes} boxes claimed",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (p in gameState.players) {
                    val color = PlayerColors.getOrElse(p.colorIndex) { PlayerColors[0] }
                    val score = gameState.scores[p.id] ?: 0
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceCardElevated)
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = p.name, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            text = "$score boxes",
                            color = color,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        },
        containerColor = SurfaceCard
    )
}
