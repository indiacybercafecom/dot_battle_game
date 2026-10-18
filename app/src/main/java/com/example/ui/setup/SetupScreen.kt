package com.example.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.AiDifficulty
import com.example.game.model.BoardSize
import com.example.game.model.GameMode
import com.example.game.model.Player
import com.example.ui.theme.BorderMuted
import com.example.ui.theme.DotOrange
import com.example.ui.theme.PlayerColors
import com.example.ui.theme.SurfaceBlack
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SetupScreen(
    mode: GameMode,
    defaultBoardSize: BoardSize,
    defaultAiDifficulty: AiDifficulty,
    onBack: () -> Unit,
    onStartGame: (BoardSize, List<Player>) -> Unit
) {
    var selectedBoardSize by remember { mutableStateOf(defaultBoardSize) }
    var selectedDifficulty by remember { mutableStateOf(defaultAiDifficulty) }
    var selectedColorIndex by remember { mutableIntStateOf(0) }
    var totalPlayersCount by remember { mutableIntStateOf(if (mode == GameMode.MULTI_AI) 3 else 2) }
    var includeHuman by remember { mutableStateOf(true) }

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
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("setup_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = if (mode == GameMode.VS_AI) "Play vs AI Setup" else "Multi-AI Setup",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Board Size Selector
                SectionTitle(title = "BOARD SIZE")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BoardSize.values().forEach { size ->
                        val isSelected = size == selectedBoardSize
                        SelectableCard(
                            title = size.label,
                            subtitle = "${size.boxes * size.boxes} boxes",
                            isSelected = isSelected,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedBoardSize = size }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (mode == GameMode.VS_AI) {
                    // AI Difficulty
                    SectionTitle(title = "AI DIFFICULTY")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AiDifficulty.values().forEach { diff ->
                            val isSelected = diff == selectedDifficulty
                            SelectableCard(
                                title = diff.name.lowercase().replaceFirstChar { it.uppercase() },
                                subtitle = when (diff) {
                                    AiDifficulty.EASY -> "Casual"
                                    AiDifficulty.MEDIUM -> "Balanced"
                                    AiDifficulty.HARD -> "Minimax"
                                },
                                isSelected = isSelected,
                                modifier = Modifier.weight(1f),
                                onClick = { selectedDifficulty = diff }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Player Color Selector
                    SectionTitle(title = "YOUR COLOR")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlayerColors.forEachIndexed { index, color ->
                            val isSelected = index == selectedColorIndex
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorIndex = index },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // MULTI-AI MODE CONFIGURATION
                    SectionTitle(title = "NUMBER OF PLAYERS")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf(2, 3, 4).forEach { count ->
                            val isSelected = count == totalPlayersCount
                            SelectableCard(
                                title = "$count Players",
                                subtitle = if (count == 4) "Chaos" else "Standard",
                                isSelected = isSelected,
                                modifier = Modifier.weight(1f),
                                onClick = { totalPlayersCount = count }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // AI-Only Mode Toggle
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderMuted, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Include Human Player",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (includeHuman) "You vs ${totalPlayersCount - 1} AI Bots" else "Spectate AI Bots Battle",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                            Switch(
                                checked = includeHuman,
                                onCheckedChange = { includeHuman = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = DotOrange,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = SurfaceCardElevated
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    SectionTitle(title = "BOT DIFFICULTY")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AiDifficulty.values().forEach { diff ->
                            val isSelected = diff == selectedDifficulty
                            SelectableCard(
                                title = diff.name.lowercase().replaceFirstChar { it.uppercase() },
                                subtitle = when (diff) {
                                    AiDifficulty.EASY -> "Casual"
                                    AiDifficulty.MEDIUM -> "Tactical"
                                    AiDifficulty.HARD -> "Master"
                                },
                                isSelected = isSelected,
                                modifier = Modifier.weight(1f),
                                onClick = { selectedDifficulty = diff }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            // Start Game Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Button(
                    onClick = {
                        val players = buildPlayerList(
                            mode = mode,
                            includeHuman = includeHuman,
                            totalPlayers = if (mode == GameMode.VS_AI) 2 else totalPlayersCount,
                            humanColorIndex = selectedColorIndex,
                            difficulty = selectedDifficulty
                        )
                        onStartGame(selectedBoardSize, players)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("start_game_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = DotOrange),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = SurfaceBlack
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (mode == GameMode.MULTI_AI && !includeHuman) "Start AI Simulation" else "Start Match",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = SurfaceBlack
                    )
                }
            }
        }
    }
}

private fun buildPlayerList(
    mode: GameMode,
    includeHuman: Boolean,
    totalPlayers: Int,
    humanColorIndex: Int,
    difficulty: AiDifficulty
): List<Player> {
    val players = mutableListOf<Player>()
    var nextColorIdx = 0

    if (mode == GameMode.VS_AI) {
        players.add(
            Player(
                id = 1,
                name = "You",
                colorIndex = humanColorIndex,
                isAi = false
            )
        )
        val botColor = if (humanColorIndex == 1) 0 else 1
        players.add(
            Player(
                id = 2,
                name = "AI Bot (${difficulty.name.lowercase().replaceFirstChar { it.uppercase() }})",
                colorIndex = botColor,
                isAi = true,
                aiDifficulty = difficulty
            )
        )
    } else {
        // Multi-AI
        var idCounter = 1
        if (includeHuman) {
            players.add(
                Player(
                    id = idCounter++,
                    name = "You",
                    colorIndex = 0,
                    isAi = false
                )
            )
            nextColorIdx = 1
        }
        val botNames = listOf("Nexus AI", "Pulse Bot", "Vortex AI", "Omega Bot")
        while (players.size < totalPlayers) {
            val name = botNames.getOrElse(players.size) { "Bot ${players.size + 1}" }
            players.add(
                Player(
                    id = idCounter++,
                    name = name,
                    colorIndex = nextColorIdx % PlayerColors.size,
                    isAi = true,
                    aiDifficulty = difficulty
                )
            )
            nextColorIdx++
        }
    }
    return players
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.Bold
        ),
        color = TextSecondary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SelectableCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) DotOrange else BorderMuted,
                shape = RoundedCornerShape(10.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) DotOrange.copy(alpha = 0.12f) else SurfaceCardElevated
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isSelected) DotOrange else TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
    }
}
