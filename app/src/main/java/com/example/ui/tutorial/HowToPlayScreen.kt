package com.example.ui.tutorial

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Scoreboard
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderMuted
import com.example.ui.theme.DotOrange
import com.example.ui.theme.Player2Color
import com.example.ui.theme.Player3Color
import com.example.ui.theme.Player4Color
import com.example.ui.theme.SurfaceBlack
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HowToPlayScreen(onBack: () -> Unit) {
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
                    onClick = onBack,
                    modifier = Modifier.testTag("tutorial_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "How to Play",
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
                Spacer(modifier = Modifier.height(12.dp))

                TutorialCard(
                    stepNumber = "1",
                    title = "Connect Dots with Lines",
                    description = "On your turn, tap between any two adjacent dots to draw a horizontal or vertical line. Once a line is claimed, it cannot be drawn again.",
                    icon = Icons.Default.LinearScale,
                    accentColor = DotOrange
                )

                Spacer(modifier = Modifier.height(14.dp))

                TutorialCard(
                    stepNumber = "2",
                    title = "Capture Boxes",
                    description = "When you draw the fourth line that surrounds a 1×1 square, the box is instantly claimed in your player color and awarded to your score.",
                    icon = Icons.Default.CropSquare,
                    accentColor = Player2Color
                )

                Spacer(modifier = Modifier.height(14.dp))

                TutorialCard(
                    stepNumber = "3",
                    title = "Bonus Turns & Double Boxes",
                    description = "Completing a box grants you an immediate EXTRA TURN! If one line completes two adjacent boxes simultaneously, you capture both and score 2 points.",
                    icon = Icons.Default.Repeat,
                    accentColor = Player3Color
                )

                Spacer(modifier = Modifier.height(14.dp))

                TutorialCard(
                    stepNumber = "4",
                    title = "Scoring & Winning",
                    description = "The match concludes when every box on the board is claimed. The player with the highest box count wins the match! Equal scores result in a draw.",
                    icon = Icons.Default.Scoreboard,
                    accentColor = Player4Color
                )

                Spacer(modifier = Modifier.height(14.dp))

                TutorialCard(
                    stepNumber = "5",
                    title = "Online Rooms & Matchmaking",
                    description = "In Play Online, use Quick Match to automatically find opponents or Create a Private Room to receive a 6-character code that friends can enter to join instantly.",
                    icon = Icons.Default.Wifi,
                    accentColor = Color(0xFFFFD166)
                )

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun TutorialCard(
    stepNumber: String,
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderMuted, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.16f))
                    .border(1.dp, accentColor.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stepNumber,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = accentColor
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            }
        }
    }
}
