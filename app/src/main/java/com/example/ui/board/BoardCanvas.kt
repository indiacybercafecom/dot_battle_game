package com.example.ui.board

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.BoardSize
import com.example.game.model.Box
import com.example.game.model.Edge
import com.example.game.model.GameState
import com.example.game.model.Orientation
import com.example.ui.theme.BorderMuted
import com.example.ui.theme.PlayerColors
import com.example.ui.theme.PoppinsFontFamily
import com.example.ui.theme.SurfaceCardElevated
import kotlin.math.hypot

@Composable
fun BoardCanvas(
    gameState: GameState,
    enabled: Boolean,
    onEdgeSelected: (Edge) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    // Touch threshold for edge selection
    val touchThresholdPx = with(density) { 26.dp.toPx() }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val boardDimension = minOf(maxWidth, maxHeight)
        val n = gameState.boardSize.boxes

        Canvas(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxSize()
                .padding(16.dp)
                .pointerInput(enabled, gameState.claimedEdges.size) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { tapOffset ->
                        val canvasSize = size.width.toFloat()
                        val cellSize = canvasSize / n
                        val bestEdge = findClosestAvailableEdge(
                            tapOffset = tapOffset,
                            n = n,
                            cellSize = cellSize,
                            claimedEdges = gameState.claimedEdges,
                            thresholdPx = touchThresholdPx
                        )
                        if (bestEdge != null) {
                            onEdgeSelected(bestEdge)
                        }
                    }
                }
        ) {
            val canvasSize = size.width
            val cellSize = canvasSize / n
            val dotRadius = when (gameState.boardSize) {
                BoardSize.SMALL -> 6.dp.toPx()
                BoardSize.MEDIUM -> 4.5.dp.toPx()
                BoardSize.LARGE -> 3.5.dp.toPx()
            }
            val edgeStrokeWidth = when (gameState.boardSize) {
                BoardSize.SMALL -> 6.dp.toPx()
                BoardSize.MEDIUM -> 4.5.dp.toPx()
                BoardSize.LARGE -> 3.5.dp.toPx()
            }

            // 1. Draw Captured Boxes Backgrounds & Initials
            drawBoxes(
                boxes = gameState.boxes,
                cellSize = cellSize,
                textMeasurer = textMeasurer,
                gameState = gameState
            )

            // 2. Draw Unclaimed Edge Guide Dots / Faint Lines
            drawEdgePlaceholders(
                n = n,
                cellSize = cellSize,
                claimedEdges = gameState.claimedEdges
            )

            // 3. Draw Claimed Edges
            for ((edge, playerId) in gameState.claimedEdges) {
                val player = gameState.players.find { it.id == playerId }
                val color = player?.let { PlayerColors.getOrElse(it.colorIndex) { PlayerColors[0] } }
                    ?: PlayerColors[0]

                drawClaimedEdge(
                    edge = edge,
                    cellSize = cellSize,
                    color = color,
                    strokeWidth = edgeStrokeWidth
                )
            }

            // 4. Draw Grid Dots
            for (r in 0..n) {
                for (c in 0..n) {
                    val dotCenter = Offset(c * cellSize, r * cellSize)
                    // Outer subtle ring
                    drawCircle(
                        color = Color.White.copy(alpha = 0.2f),
                        radius = dotRadius + 2.dp.toPx(),
                        center = dotCenter
                    )
                    // Inner bright dot
                    drawCircle(
                        color = Color.White,
                        radius = dotRadius,
                        center = dotCenter
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawBoxes(
    boxes: List<Box>,
    cellSize: Float,
    textMeasurer: TextMeasurer,
    gameState: GameState
) {
    for (box in boxes) {
        val playerId = box.claimedByPlayerId ?: continue
        val player = gameState.players.find { it.id == playerId }
        val playerColor = player?.let { PlayerColors.getOrElse(it.colorIndex) { PlayerColors[0] } }
            ?: PlayerColors[0]

        val left = box.col * cellSize
        val top = box.row * cellSize
        val boxSize = Size(cellSize, cellSize)

        // Semi-transparent box fill with slight inner margin
        drawRect(
            color = playerColor.copy(alpha = 0.22f),
            topLeft = Offset(left + 2f, top + 2f),
            size = Size(boxSize.width - 4f, boxSize.height - 4f)
        )

        // Player initial inside box
        val initial = player?.name?.take(1)?.uppercase() ?: "P"
        val fontSize = (cellSize * 0.38f).sp
        val textLayoutResult = textMeasurer.measure(
            text = initial,
            style = TextStyle(
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                color = playerColor.copy(alpha = 0.85f)
            )
        )

        val textOffset = Offset(
            x = left + (cellSize - textLayoutResult.size.width) / 2f,
            y = top + (cellSize - textLayoutResult.size.height) / 2f
        )
        drawText(textLayoutResult, topLeft = textOffset)
    }
}

private fun DrawScope.drawEdgePlaceholders(
    n: Int,
    cellSize: Float,
    claimedEdges: Map<Edge, Int>
) {
    // Subtle placeholder dashed/faint lines for unclaimed edges
    for (r in 0..n) {
        for (c in 0 until n) {
            val edge = Edge(Orientation.HORIZONTAL, r, c)
            if (!claimedEdges.containsKey(edge)) {
                drawLine(
                    color = BorderMuted.copy(alpha = 0.35f),
                    start = Offset(c * cellSize + 8f, r * cellSize),
                    end = Offset((c + 1) * cellSize - 8f, r * cellSize),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }

    for (r in 0 until n) {
        for (c in 0..n) {
            val edge = Edge(Orientation.VERTICAL, r, c)
            if (!claimedEdges.containsKey(edge)) {
                drawLine(
                    color = BorderMuted.copy(alpha = 0.35f),
                    start = Offset(c * cellSize, r * cellSize + 8f),
                    end = Offset(c * cellSize, (r + 1) * cellSize - 8f),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

private fun DrawScope.drawClaimedEdge(
    edge: Edge,
    cellSize: Float,
    color: Color,
    strokeWidth: Float
) {
    when (edge.orientation) {
        Orientation.HORIZONTAL -> {
            val start = Offset(edge.col * cellSize, edge.row * cellSize)
            val end = Offset((edge.col + 1) * cellSize, edge.row * cellSize)
            // Outer glow
            drawLine(
                color = color.copy(alpha = 0.35f),
                start = start,
                end = end,
                strokeWidth = strokeWidth * 2.2f,
                cap = StrokeCap.Round
            )
            // Solid center line
            drawLine(
                color = color,
                start = start,
                end = end,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
        Orientation.VERTICAL -> {
            val start = Offset(edge.col * cellSize, edge.row * cellSize)
            val end = Offset(edge.col * cellSize, (edge.row + 1) * cellSize)
            // Outer glow
            drawLine(
                color = color.copy(alpha = 0.35f),
                start = start,
                end = end,
                strokeWidth = strokeWidth * 2.2f,
                cap = StrokeCap.Round
            )
            // Solid center line
            drawLine(
                color = color,
                start = start,
                end = end,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun findClosestAvailableEdge(
    tapOffset: Offset,
    n: Int,
    cellSize: Float,
    claimedEdges: Map<Edge, Int>,
    thresholdPx: Float
): Edge? {
    var minDistance = Float.MAX_VALUE
    var closestEdge: Edge? = null

    // Check horizontal edges
    for (r in 0..n) {
        for (c in 0 until n) {
            val edge = Edge(Orientation.HORIZONTAL, r, c)
            if (claimedEdges.containsKey(edge)) continue

            val startX = c * cellSize
            val endX = (c + 1) * cellSize
            val y = r * cellSize

            // Distance to horizontal segment
            if (tapOffset.x in (startX - 8f)..(endX + 8f)) {
                val dist = kotlin.math.abs(tapOffset.y - y)
                if (dist < minDistance && dist <= thresholdPx) {
                    minDistance = dist
                    closestEdge = edge
                }
            }
        }
    }

    // Check vertical edges
    for (r in 0 until n) {
        for (c in 0..n) {
            val edge = Edge(Orientation.VERTICAL, r, c)
            if (claimedEdges.containsKey(edge)) continue

            val startY = r * cellSize
            val endY = (r + 1) * cellSize
            val x = c * cellSize

            // Distance to vertical segment
            if (tapOffset.y in (startY - 8f)..(endY + 8f)) {
                val dist = kotlin.math.abs(tapOffset.x - x)
                if (dist < minDistance && dist <= thresholdPx) {
                    minDistance = dist
                    closestEdge = edge
                }
            }
        }
    }

    return closestEdge
}
