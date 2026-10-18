package com.example.ui.online

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.BoardSize
import com.example.game.model.GameMode
import com.example.game.model.Player
import com.example.network.MultiplayerManager
import com.example.network.NetworkState
import com.example.ui.game.GameViewModel
import com.example.ui.theme.BorderMuted
import com.example.ui.theme.BorderStrong
import com.example.ui.theme.DotOrange
import com.example.ui.theme.Player2Color
import com.example.ui.theme.Player3Color
import com.example.ui.theme.SurfaceBlack
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun OnlineLobbyScreen(
    viewModel: GameViewModel,
    onBack: () -> Unit,
    onStartOnlineGame: (BoardSize, List<Player>) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val multiplayer = viewModel.multiplayerManager
    val networkState by multiplayer.networkState.collectAsState()
    val currentRoom by multiplayer.currentRoom.collectAsState()
    val errorMessage by multiplayer.errorMessage.collectAsState()
    val userSettings by viewModel.settings.collectAsState()

    var joinRoomCodeInput by remember { mutableStateOf("") }
    var selectedBoardSize by remember { mutableStateOf(BoardSize.MEDIUM) }

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
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        multiplayer.leaveRoom()
                        onBack()
                    },
                    modifier = Modifier.testTag("online_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = "Online Multiplayer",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "Player: ${userSettings.playerName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // Error Banner
                AnimatedVisibility(visible = errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF4A1515))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage ?: "",
                                color = Color(0xFFFFB4AB),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { multiplayer.clearError() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Matchmaking Status Indicator Card
                if (networkState == NetworkState.QUEUED) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Player2Color, RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = Player2Color,
                                modifier = Modifier.size(44.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Finding Opponent...",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Searching global queue for a match",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = { multiplayer.cancelMatchmaking() },
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCardElevated),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Cancel Matchmaking", color = TextPrimary)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                } else if (currentRoom != null && !currentRoom!!.isGameStarted) {
                    // Private Room Waiting Lobby
                    val room = currentRoom!!
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, DotOrange, RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "ROOM CREATED",
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                                color = DotOrange
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = room.roomId,
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 4.sp
                                    ),
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Room Code", room.roomId))
                                        Toast.makeText(context, "Room code copied!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy code",
                                        tint = DotOrange
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Share this 6-character code with your opponent",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            CircularProgressIndicator(
                                color = DotOrange,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Waiting for player to join...",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { multiplayer.leaveRoom() },
                                colors = ButtonDefaults.buttonColors(containerColor = SurfaceCardElevated),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Cancel Room", color = TextPrimary)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                } else if (currentRoom != null && currentRoom!!.isGameStarted) {
                    // Both players ready - launch match
                    val room = currentRoom!!
                    val isHost = room.hostPlayerId == multiplayer.localUserId
                    val players = listOf(
                        Player(
                            id = 1,
                            name = if (isHost) room.hostPlayerName else (room.guestPlayerName ?: "You"),
                            colorIndex = 0,
                            isAi = false
                        ),
                        Player(
                            id = 2,
                            name = if (isHost) (room.guestPlayerName ?: "Opponent") else room.hostPlayerName,
                            colorIndex = 1,
                            isAi = false
                        )
                    )
                    onStartOnlineGame(room.boardSize, players)
                }

                // Quick Match Option
                Text(
                    text = "QUICK PLAY",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            multiplayer.startQuickMatch(
                                playerName = userSettings.playerName,
                                boardSize = selectedBoardSize,
                                onMatched = { room ->
                                    val players = listOf(
                                        Player(1, userSettings.playerName, 0, false),
                                        Player(2, room.guestPlayerName ?: "Opponent", 1, false)
                                    )
                                    onStartOnlineGame(room.boardSize, players)
                                }
                            )
                        }
                        .border(1.dp, BorderMuted, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Player2Color.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FlashOn, contentDescription = null, tint = Player2Color)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Quick Match",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Instantly queue against an online player",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Private Room Options
                Text(
                    text = "PRIVATE ROOM",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontWeight = FontWeight.Bold),
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Create Private Room
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            multiplayer.createRoom(
                                hostName = userSettings.playerName,
                                boardSize = selectedBoardSize
                            )
                        }
                        .border(1.dp, BorderMuted, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(DotOrange.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = null, tint = DotOrange)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Create Private Room",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Get a 6-digit code for a friend to join",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Join Private Room Card with input
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderMuted, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Player3Color.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, tint = Player3Color)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Join with Room Code",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Enter code to connect to an active match",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = joinRoomCodeInput,
                                onValueChange = {
                                    if (it.length <= 6) joinRoomCodeInput = it.uppercase()
                                },
                                placeholder = { Text("e.g. 7K4B2A", color = TextMuted) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("room_code_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Player3Color,
                                    unfocusedBorderColor = BorderMuted,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Characters,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { focusManager.clearFocus() }
                                )
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    multiplayer.joinRoom(
                                        roomCode = joinRoomCodeInput,
                                        guestName = userSettings.playerName,
                                        onJoined = { room ->
                                            val players = listOf(
                                                Player(1, userSettings.playerName, 0, false),
                                                Player(2, room.hostPlayerName, 1, false)
                                            )
                                            onStartOnlineGame(room.boardSize, players)
                                        },
                                        onError = {}
                                    )
                                },
                                enabled = joinRoomCodeInput.trim().length >= 4,
                                colors = ButtonDefaults.buttonColors(containerColor = Player3Color),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("join_room_button")
                            ) {
                                Text("Join", color = SurfaceBlack, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
